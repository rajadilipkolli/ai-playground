package com.learning.ai.modelregression.service;

import com.learning.ai.modelregression.model.EmailClassification;
import com.learning.ai.modelregression.model.PromptConfig;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.stereotype.Service;

@Service
public class EmailClassifierService {

    private static final Logger log = LoggerFactory.getLogger(EmailClassifierService.class);

    private final ChatClient chatClient;
    private final Validator validator;
    private final MeterRegistry meterRegistry;

    public EmailClassifierService(
            ChatClient.Builder chatClientBuilder, Validator validator, MeterRegistry meterRegistry) {
        this.chatClient = chatClientBuilder.build();
        this.validator = validator;
        this.meterRegistry = meterRegistry;
    }

    public ClassificationResult classify(String emailText, PromptConfig promptConfig) {
        BeanOutputConverter<EmailClassification> outputParser = new BeanOutputConverter<>(EmailClassification.class);
        String format = outputParser.getFormat();

        String combinedSystemPrompt = promptConfig.systemPrompt() + "\n\n"
                + promptConfig.fewShotExamples() + "\n\n"
                + "You must respond strictly in the requested JSON format.\n"
                + "{format}";

        // Add the user message directly to the prompt.

        Timer.Sample sample = Timer.start(meterRegistry);

        try {
            ChatResponse response = chatClient
                    .prompt()
                    .system(s -> s.text(combinedSystemPrompt).param("format", format))
                    .user(emailText)
                    .call()
                    .chatResponse();

            long latencyMs = sample.stop(meterRegistry.timer("email.classifier.latency")) / 1_000_000;

            Integer inputTokens = 0;
            Integer outputTokens = 0;
            if (response.getMetadata() != null && response.getMetadata().getUsage() != null) {
                Integer pTokens = response.getMetadata().getUsage().getPromptTokens();
                Integer cTokens = response.getMetadata().getUsage().getCompletionTokens();
                if (pTokens != null) inputTokens = pTokens;
                if (cTokens != null) outputTokens = cTokens;
            }

            String content = response.getResult().getOutput().getText();

            EmailClassification classification = parseAndValidate(content, outputParser);
            return new ClassificationResult(classification, latencyMs, inputTokens, outputTokens, false);

        } catch (Exception e) {
            log.error("Failed to classify email, falling back.", e);
            long latencyMs = sample.stop(meterRegistry.timer("email.classifier.latency", "error", "true")) / 1_000_000;
            return new ClassificationResult(null, latencyMs, 0, 0, true);
        }
    }

    private EmailClassification parseAndValidate(String content, BeanOutputConverter<EmailClassification> parser) {
        try {
            EmailClassification result = parser.convert(content);
            if (result == null) {
                throw new IllegalStateException("Parsed result is null");
            }
            Set<ConstraintViolation<EmailClassification>> violations = validator.validate(result);
            if (!violations.isEmpty()) {
                throw new IllegalStateException("Validation failed: " + violations);
            }
            return result;
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to parse or validate: " + ex.getMessage(), ex);
        }
    }

    public record ClassificationResult(
            EmailClassification classification, long latencyMs, int inputTokens, int outputTokens, boolean error) {}
}
