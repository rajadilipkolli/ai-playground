package com.learning.ai.modelregression.controller;

import com.learning.ai.modelregression.model.EmailClassification;
import com.learning.ai.modelregression.model.EmailRequest;
import com.learning.ai.modelregression.model.PromptConfig;
import com.learning.ai.modelregression.prompt.PromptConfigLoader;
import com.learning.ai.modelregression.service.EmailClassifierService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1/classifier")
public class ClassifierController {

    private final EmailClassifierService emailClassifierService;
    private final PromptConfigLoader promptConfigLoader;

    public ClassifierController(EmailClassifierService emailClassifierService, PromptConfigLoader promptConfigLoader) {
        this.emailClassifierService = emailClassifierService;
        this.promptConfigLoader = promptConfigLoader;
    }

    @PostMapping
    public EmailClassification classifyEmail(
            @Valid @RequestBody EmailRequest request, @RequestParam(required = false) String version) {

        PromptConfig promptConfig = promptConfigLoader.loadConfig(version);
        EmailClassifierService.ClassificationResult result =
                emailClassifierService.classify(request.emailText(), promptConfig);

        if (result.classification() == null) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Classification failed");
        }
        return result.classification();
    }
}
