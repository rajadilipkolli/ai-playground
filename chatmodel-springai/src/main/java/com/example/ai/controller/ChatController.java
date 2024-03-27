package com.example.ai.controller;

import com.example.ai.model.response.AIChatResponse;
import org.springframework.ai.chat.ChatClient;
import org.springframework.ai.chat.ChatResponse;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/ai")
public class ChatController {

    private final ChatClient chatClient;

    ChatController(ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    @GetMapping("/chat")
    Map<String,String> chat(@RequestParam String question) {
        var response = chatClient.call(question);
        return Map.of("question", question, "answer", response);
    }

    @GetMapping("/chat-with-prompt")
    AIChatResponse chatWithPrompt(@RequestParam String subject) {
        PromptTemplate promptTemplate = new PromptTemplate("Tell me a joke about {subject}");
        Prompt prompt = promptTemplate.create(Map.of("subject", subject));
        ChatResponse response = chatClient.call(prompt);
        String answer = response.getResult().getOutput().getContent();
        return new AIChatResponse(answer);
    }

    @GetMapping("/chat-with-system-prompt")
    AIChatResponse chatWithSystemPrompt(@RequestParam String subject) {
        SystemMessage systemMessage = new SystemMessage("You are a sarcastic and funny chatbot");
        UserMessage userMessage = new UserMessage("Tell me a joke about " + subject);
        Prompt prompt = new Prompt(List.of(systemMessage, userMessage));
        ChatResponse response = chatClient.call(prompt);
        String answer = response.getResult().getOutput().getContent();
        return new AIChatResponse(answer);
    }
}
