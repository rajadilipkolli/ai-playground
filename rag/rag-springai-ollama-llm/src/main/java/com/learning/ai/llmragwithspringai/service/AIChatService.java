package com.learning.ai.llmragwithspringai.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.QuestionAnswerAdvisor;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

@Service
public class AIChatService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AIChatService.class);

    private static final String template =
            """
            You are a helpful assistant, conversing with a user about the subjects contained in a set of documents.
            Use the information from the DOCUMENTS section to provide accurate answers. If unsure or if the answer
            isn't found in the DOCUMENTS section, simply state that you don't know the answer.

            DOCUMENTS:
            {documents}

            """;

    private final ChatClient aiClient;

    public AIChatService(ChatClient.Builder builder, VectorStore vectorStore) {
        this.aiClient = builder.defaultSystem(template)
                .defaultAdvisors(new QuestionAnswerAdvisor(
                        vectorStore, SearchRequest.defaults().withTopK(5))) // RAG
                .build();
    }

    public String chat(String query) {
        ChatResponse aiResponse = aiClient.prompt().user(query).call().chatResponse();
        LOGGER.info("Response received from call :{}", aiResponse);
        Generation generation = aiResponse.getResult();
        return (generation != null) ? generation.getOutput().getContent() : "";
    }
}
