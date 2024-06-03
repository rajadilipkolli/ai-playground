package com.learning.ai.llmragwithspringai.service;

import static org.springframework.ai.chat.client.advisor.AbstractChatMemoryAdvisor.CHAT_MEMORY_RETRIEVE_SIZE_KEY;

import java.util.List;
import java.util.stream.Collectors;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.PromptChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.QuestionAnswerAdvisor;
import org.springframework.ai.chat.memory.InMemoryChatMemory;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
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

    public AIChatService(ChatClient.Builder modelBuilder, VectorStore vectorStore) {
        this.aiClient = modelBuilder
                .defaultSystem(template)
                .defaultAdvisors(
                        new PromptChatMemoryAdvisor(new InMemoryChatMemory()),
                        // new MessageChatMemoryAdvisor(chatMemory), // CHAT MEMORY
                        new QuestionAnswerAdvisor(vectorStore, SearchRequest.defaults())) // RAG
                .defaultFunctions("currentDateFunction") // FUNCTION CALLING
                .build();
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
        ChatResponse aiResponse = aiClient.prompt()
                .system(sp -> sp.param("documents", documents))
                .user(searchQuery)
                .advisors(a -> a.param(CHAT_MEMORY_RETRIEVE_SIZE_KEY, 100))
                .call()
                .chatResponse();
        Generation generation = aiResponse.getResult();
        return (generation != null) ? generation.getOutput().getContent() : "";
    }
}
