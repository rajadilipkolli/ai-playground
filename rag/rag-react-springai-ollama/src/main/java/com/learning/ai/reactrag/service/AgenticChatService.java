package com.learning.ai.reactrag.service;

import com.learning.ai.reactrag.model.response.ChatResponse;
import io.micrometer.observation.annotation.Observed;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.rag.advisor.RetrievalAugmentationAdvisor;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

@Service
@Observed(name = "agentic.chat", contextualName = "agentic-chat")
public class AgenticChatService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AgenticChatService.class);

    private final ChatClient chatClient;
    private final List<ToolCallback> toolCallbacks;
    private final Resource systemPromptResource;

    public AgenticChatService(
            ChatClient.Builder chatClientBuilder,
            List<ToolCallback> toolCallbacks,
            @Value("classpath:prompts/react-system-prompt.txt") Resource systemPromptResource) {

        this.toolCallbacks = toolCallbacks;
        this.systemPromptResource = systemPromptResource;

        this.chatClient = chatClientBuilder.build();
    }

    public ChatResponse chat(String query, boolean includeDiagnostics) {
        LOGGER.info("Starting agentic chat with query: {}", query);

        org.springframework.ai.chat.model.ChatResponse aiResponse = chatClient
                .prompt()
                .system(systemPromptResource)
                .user(query)
                .tools(toolCallbacks.toArray(new ToolCallback[0]))
                .call()
                .chatResponse();

        String answer = aiResponse.getResult().getOutput().getText();

        List<String> retrievedDocuments = new ArrayList<>();
        List<String> toolsUsed =
                new ArrayList<>(); // Spring AI does not trivially expose executed tools in the response metadata
        // natively in this release, so this may be empty unless specifically extracted.

        if (includeDiagnostics) {
            Object docsObj = aiResponse.getMetadata().get(RetrievalAugmentationAdvisor.DOCUMENT_CONTEXT);
            if (docsObj instanceof List<?> docs) {
                for (Object obj : docs) {
                    if (obj instanceof Document doc) {
                        retrievedDocuments.add(doc.getText());
                    }
                }
            }
            // Add tool calls to toolsUsed if present in the response
            if (aiResponse.getResult().getOutput().hasToolCalls()) {
                aiResponse.getResult().getOutput().getToolCalls().forEach(tc -> {
                    toolsUsed.add("Tool: " + tc.name() + ", Args: " + tc.arguments());
                });
            }
        }

        return new ChatResponse(
                answer,
                includeDiagnostics ? retrievedDocuments : Collections.emptyList(),
                includeDiagnostics ? toolsUsed : Collections.emptyList());
    }
}
