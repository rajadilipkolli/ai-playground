package com.learning.ai.llmragwithspringai.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.rag.advisor.RetrievalAugmentationAdvisor;
import org.springframework.ai.rag.generation.augmentation.ContextualQueryAugmenter;
import org.springframework.ai.rag.retrieval.search.VectorStoreDocumentRetriever;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

@Service
public class AIChatService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AIChatService.class);

    private final ChatClient aiClient;

    public AIChatService(ChatClient.Builder builder, VectorStore vectorStore) {

        var documentRetriever = VectorStoreDocumentRetriever.builder()
                .vectorStore(vectorStore)
                .similarityThreshold(0.50)
                .build();

        var queryAugmenter =
                ContextualQueryAugmenter.builder().allowEmptyContext(true).build();

        RetrievalAugmentationAdvisor retrievalAugmentationAdvisor = RetrievalAugmentationAdvisor.builder()
                .documentRetriever(documentRetriever)
                .queryAugmenter(queryAugmenter)
                .build();
        this.aiClient =
                builder.clone().defaultAdvisors(retrievalAugmentationAdvisor).build();
    }

    public String chat(String query) {
        String aiResponse = aiClient.prompt().user(query).call().content();
        LOGGER.info("Response received from call :{}", aiResponse);
        return aiResponse;
    }
}
