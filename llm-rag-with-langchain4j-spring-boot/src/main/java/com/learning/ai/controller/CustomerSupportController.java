package com.learning.ai.controller;

import com.learning.ai.config.AICustomerSupportAgent;
import com.learning.ai.domain.AICustomerSupportResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class CustomerSupportController {

    private final AICustomerSupportAgent aiCustomerSupportAgent;

    public CustomerSupportController(AICustomerSupportAgent aiCustomerSupportAgent) {
        this.aiCustomerSupportAgent = aiCustomerSupportAgent;
    }

    @GetMapping("/chat")
    public AICustomerSupportResponse customerSupportChat(
            @RequestParam(
                            value = "message",
                            defaultValue =
                                    "what should I know about the transition to consumer direct care network washington?")
                    String message) {
        return aiCustomerSupportAgent.chat(message);
    }
}
