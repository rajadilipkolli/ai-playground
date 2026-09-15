package com.learning.ai.controller;

import com.learning.ai.domain.request.AIChatRequest;
import com.learning.ai.domain.response.AICustomerSupportResponseWrapper;
import com.learning.ai.service.CustomerSupportService;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ai")
@Validated
class CustomerSupportController {

    private final CustomerSupportService customerSupportService;

    public CustomerSupportController(CustomerSupportService customerSupportService) {
        this.customerSupportService = customerSupportService;
    }

    @PostMapping("/chat")
    AICustomerSupportResponseWrapper customerSupportChat(
            @RequestBody @Valid AIChatRequest aiChatRequest,
            @RequestParam(defaultValue = "false") boolean includeDiagnostics) {
        return customerSupportService.chat(aiChatRequest.question(), includeDiagnostics);
    }
}
