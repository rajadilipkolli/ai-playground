package com.learning.ai.llmragwithspringai.service;

import org.springframework.ai.chat.ChatClient;
import org.springframework.ai.chat.ChatResponse;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.SystemPromptTemplate;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class AIChatService {

    private static final String template = """
                        
            You're assisting with questions about services offered by Carina.
            Carina is a two-sided healthcare marketplace focusing on home care aides (caregivers)
            and their Medicaid in-home care clients (adults and children with developmental disabilities and low income elderly population).
            Carina's mission is to build online tools to bring good jobs to care workers, so care workers can provide the
            best possible care for those who need it.
                    
            Use the information from the DOCUMENTS section to provide accurate answers but act as if you knew this information innately.
            If unsure, simply state that you don't know.
                    
            DOCUMENTS:
            {documents}
                        
            """;

    private final ChatClient aiClient;
    private final VectorStore vectorStore;

    public AIChatService(ChatClient aiClient, VectorStore vectorStore) {
        this.aiClient = aiClient;
        this.vectorStore = vectorStore;
    }

    public String chat(String message) {
        // Querying the VectorStore using natural language looking for the information about info asked.
        List<Document> listOfSimilarDocuments = this.vectorStore.similaritySearch(message);
        String documents = listOfSimilarDocuments
                .stream()
                .map(Document::getContent)
                .collect(Collectors.joining(System.lineSeparator()));
        // Constructing the systemMessage to indicate the AI model to use the passed information
        // to answer the question.
        Message systemMessage = new SystemPromptTemplate(template)
                .createMessage(Map.of("documents", documents));
        UserMessage userMessage = new UserMessage(message);
        Prompt prompt = new Prompt(List.of(systemMessage, userMessage));
        ChatResponse aiResponse = aiClient.call(prompt);
        return aiResponse.getResult().getOutput().getContent();
    }


}
