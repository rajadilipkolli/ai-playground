package com.learning.ai.llmragwithspringai.controller;

import com.learning.ai.llmragwithspringai.service.AIChatService;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ai")
@Validated
public class AiController {

    private static final Logger LOGGER = LoggerFactory.getLogger(AiController.class);

    private final AIChatService aiChatService;

    public AiController(AIChatService aiChatService) {
        this.aiChatService = aiChatService;
    }

    @GetMapping("/chat")
    Map<String, String> ragService(
            @RequestParam
                    @NotBlank(message = "Query cannot be empty")
                    @Size(max = 255, message = "Query exceeds maximum length")
                    @Pattern(regexp = "^[a-zA-Z0-9 ]*$", message = "Invalid characters in query")
                    String question) {
        String chatResponse = aiChatService.chat(question);
        LOGGER.info("chatResponse :{}", chatResponse);
        return Map.of("response", chatResponse);
    }
}
