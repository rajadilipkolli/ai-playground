package com.example.ai.controller;

import com.example.ai.model.request.AIChatRequest;
import com.example.ai.model.response.AIChatResponse;
import com.example.ai.model.response.ActorsFilms;
import java.util.List;
import java.util.Map;
import org.springframework.ai.chat.ChatClient;
import org.springframework.ai.chat.ChatResponse;
import org.springframework.ai.chat.Generation;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.embedding.EmbeddingClient;
import org.springframework.ai.parser.BeanOutputParser;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ai")
public class ChatController {

    private final ChatClient chatClient;

    private final EmbeddingClient embeddingClient;

    ChatController(ChatClient chatClient, EmbeddingClient embeddingClient) {
        this.chatClient = chatClient;
        this.embeddingClient = embeddingClient;
    }

    @PostMapping("/chat")
    AIChatResponse chat(@RequestBody AIChatRequest aiChatRequest) {
        var answer = chatClient.call(aiChatRequest.query());
        return new AIChatResponse(answer);
    }

    @PostMapping("/chat-with-prompt")
    AIChatResponse chatWithPrompt(@RequestBody AIChatRequest aiChatRequest) {
        PromptTemplate promptTemplate = new PromptTemplate("Tell me a joke about {subject}");
        Prompt prompt = promptTemplate.create(Map.of("subject", aiChatRequest.query()));
        ChatResponse response = chatClient.call(prompt);
        Generation generation = response.getResult();
        String answer = (generation != null) ? generation.getOutput().getContent() : "";
        return new AIChatResponse(answer);
    }

    @PostMapping("/chat-with-system-prompt")
    AIChatResponse chatWithSystemPrompt(@RequestBody AIChatRequest aiChatRequest) {
        SystemMessage systemMessage = new SystemMessage("You are a sarcastic and funny chatbot");
        UserMessage userMessage = new UserMessage("Tell me a joke about " + aiChatRequest.query());
        Prompt prompt = new Prompt(List.of(systemMessage, userMessage));
        ChatResponse response = chatClient.call(prompt);
        String answer = response.getResult().getOutput().getContent();
        return new AIChatResponse(answer);
    }

    @PostMapping("/emebedding-client-conversion")
    AIChatResponse chatWithEmbeddingClient(@RequestBody AIChatRequest aiChatRequest) {
        List<Double> embed = embeddingClient.embed(aiChatRequest.query());
        return new AIChatResponse(embed.toString());
    }

    @GetMapping("/output")
    public ActorsFilms generate(@RequestParam(value = "actor", defaultValue = "Jr NTR") String actor) {
        BeanOutputParser<ActorsFilms> outputParser = new BeanOutputParser<>(ActorsFilms.class);

        String format = outputParser.getFormat();
        String template = """
				Generate the filmography for the actor {actor}.
				{format}
				""";
        PromptTemplate promptTemplate = new PromptTemplate(template, Map.of("actor", actor, "format", format));
        Prompt prompt = new Prompt(promptTemplate.createMessage());
        ChatResponse response = chatClient.call(prompt);
        Generation generation = response.getResult();

        return outputParser.parse(generation.getOutput().getContent());
    }
}
