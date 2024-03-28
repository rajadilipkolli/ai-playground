package com.learning.ai.llmragwithspringai.service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.ai.chat.ChatClient;
import org.springframework.ai.chat.ChatResponse;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.SystemPromptTemplate;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

@Service
public class AIChatService {

    private static final String template =
            """
            You're assisting with questions about cricket

            Cricket is a bat-and-ball game that is played between two teams of eleven players on a field at the centre of which is a 22-yard (20-metre) pitch with a wicket at each end,
            each comprising two bails balanced on three stumps.
            Two players from the batting team (the striker and nonstriker) stand in front of either wicket,
            with one player from the fielding team (the bowler) bowling the ball towards the striker's wicket from the opposite end of the pitch.
            The striker's goal is to hit the bowled ball and then switch places with the nonstriker, with the batting team scoring one run for each exchange.
            Runs are also scored when the ball reaches or crosses the boundary of the field or when the ball is bowled illegally.

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

    public String chat(String searchQuery) {
        // Querying the VectorStore using natural language looking for the information about info asked.
        List<Document> listOfSimilarDocuments = this.vectorStore.similaritySearch(searchQuery);
        String documents = listOfSimilarDocuments.stream()
                .map(Document::getContent)
                .collect(Collectors.joining(System.lineSeparator()));
        // Constructing the systemMessage to indicate the AI model to use the passed information
        // to answer the question.
        Message systemMessage = new SystemPromptTemplate(template).createMessage(Map.of("documents", documents));
        UserMessage userMessage = new UserMessage(searchQuery);
        Prompt prompt = new Prompt(List.of(systemMessage, userMessage));
        ChatResponse aiResponse = aiClient.call(prompt);
        return aiResponse.getResult().getOutput().getContent();
    }
}
