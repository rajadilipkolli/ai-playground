package com.example.ai.service;

import com.example.ai.model.response.AIChatResponse;
import com.example.ai.model.response.ActorsFilms;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.ChatClient;
import org.springframework.ai.chat.ChatResponse;
import org.springframework.ai.chat.Generation;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.AssistantPromptTemplate;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingClient;
import org.springframework.ai.parser.BeanOutputParser;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

@Service
public class ChatService {

    private static final Logger logger = LoggerFactory.getLogger(ChatService.class);
    private static final String SENTIMENT_ANALYSIS_TEMPLATE =
            "{query}, You must answer strictly in the following format: one of [POSITIVE, NEGATIVE, SARCASTIC]";

    @Value("classpath:/data/restaurants.json")
    private Resource restaurantsResource;

    @Value("classpath:/rag-prompt-template.st")
    private Resource ragPromptTemplate;

    private final EmbeddingClient embeddingClient;
    private final ChatClient chatClient;

    public ChatService(EmbeddingClient embeddingClient, ChatClient chatClient) {
        this.embeddingClient = embeddingClient;
        this.chatClient = chatClient;
    }

    public AIChatResponse chat(String query) {
        String answer = chatClient.call(query);
        return new AIChatResponse(answer);
    }

    public AIChatResponse chatWithPrompt(String query) {
        PromptTemplate promptTemplate = new PromptTemplate("Tell me a joke about {subject}");
        Prompt prompt = promptTemplate.create(Map.of("subject", query));
        ChatResponse response = chatClient.call(prompt);
        Generation generation = response.getResult();
        String answer = (generation != null) ? generation.getOutput().getContent() : "";
        return new AIChatResponse(answer);
    }

    public AIChatResponse chatWithSystemPrompt(String query) {
        SystemMessage systemMessage = new SystemMessage("You are a sarcastic and funny chatbot");
        UserMessage userMessage = new UserMessage("Tell me a joke about " + query);
        Prompt prompt = new Prompt(List.of(systemMessage, userMessage));
        ChatResponse response = chatClient.call(prompt);
        String answer = response.getResult().getOutput().getContent();
        return new AIChatResponse(answer);
    }

    public AIChatResponse analyzeSentiment(String query) {
        AssistantPromptTemplate promptTemplate = new AssistantPromptTemplate(SENTIMENT_ANALYSIS_TEMPLATE);
        Prompt prompt = promptTemplate.create(Map.of("query", query));
        ChatResponse response = chatClient.call(prompt);
        Generation generation = response.getResult();
        String answer = (generation != null) ? generation.getOutput().getContent() : "";
        return new AIChatResponse(answer);
    }

    public AIChatResponse getEmbeddings(String query) {
        List<Double> embed = embeddingClient.embed(query);
        return new AIChatResponse(embed.toString());
    }

    public ActorsFilms generateAsBean(String actor) {
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

    public AIChatResponse ragGenerate(String query) throws IOException {

        // Step 1 - Load JSON document as Documents and save
        logger.info("Loading JSON as Documents and save");
        SimpleVectorStore simpleVectorStore = new SimpleVectorStore(embeddingClient);
        List<Document> linesDocuments = new ArrayList<>();

        if (restaurantsResource.exists()) { // load existing vector store if exists
            String contentAsString = restaurantsResource.getContentAsString(StandardCharsets.UTF_8);
            // Convert lines to Documents in parallel
            linesDocuments =
                    contentAsString.lines().parallel().map(Document::new).toList();
            simpleVectorStore.accept(linesDocuments);
        }

        // Step 2 retrieve related documents to query
        logger.info("Retrieving relevant documents");
        List<Document> similarDocuments =
                simpleVectorStore.similaritySearch(SearchRequest.query(query).withTopK(2));
        logger.info(String.format("Found %s relevant documents.", similarDocuments.size()));

        List<String> contentList =
                similarDocuments.stream().map(Document::getContent).toList();
        PromptTemplate promptTemplate = new PromptTemplate(ragPromptTemplate);
        Map<String, Object> promptParameters = new HashMap<>();

        promptParameters.put("input", query);
        promptParameters.put("documents", String.join("\n", contentList));
        Prompt prompt = promptTemplate.create(promptParameters);

        ChatResponse response = chatClient.call(prompt);
        Generation generation = response.getResult();
        String answer = (generation != null) ? generation.getOutput().getContent() : "";
        simpleVectorStore.delete(linesDocuments.stream().map(Document::getId).toList());
        return new AIChatResponse(answer);
    }
}
