package com.learning.ai.controller;

import com.learning.ai.config.AICustomerSupportAgent;
import com.learning.ai.domain.request.AIChatRequest;
import com.learning.ai.domain.response.AICustomerSupportResponse;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ai")
@Validated
public class CustomerSupportController {

    private final AICustomerSupportAgent aiCustomerSupportAgent;

    public CustomerSupportController(AICustomerSupportAgent aiCustomerSupportAgent) {
        this.aiCustomerSupportAgent = aiCustomerSupportAgent;
    }

    @PostMapping("/chat")
    public AICustomerSupportResponse customerSupportChat(@RequestBody @Valid AIChatRequest aiChatRequest) {
        return aiCustomerSupportAgent.chat(aiChatRequest.question());
    }
}
