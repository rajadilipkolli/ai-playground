package com.learning.ai.controller;

import com.learning.ai.domain.request.AIChatRequest;
import com.learning.ai.domain.response.AICustomerSupportResponse;
import com.learning.ai.service.CustomerSupportService;
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

    private final CustomerSupportService customerSupportService;

    public CustomerSupportController(CustomerSupportService customerSupportService) {
        this.customerSupportService = customerSupportService;
    }

    @PostMapping("/chat")
    public AICustomerSupportResponse customerSupportChat(@RequestBody @Valid AIChatRequest aiChatRequest) {
        return customerSupportService.chat(aiChatRequest.question());
    }
}
