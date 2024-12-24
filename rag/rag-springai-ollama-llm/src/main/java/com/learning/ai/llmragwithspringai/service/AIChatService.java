package com.learning.ai.llmragwithspringai.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.RetrievalAugmentationAdvisor;
import org.springframework.ai.chat.prompt.AssistantPromptTemplate;
import org.springframework.ai.rag.generation.augmentation.ContextualQueryAugmenter;
import org.springframework.ai.rag.retrieval.search.VectorStoreDocumentRetriever;
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
            {query}

            """;

    private final ChatClient aiClient;

    public AIChatService(ChatClient.Builder builder, VectorStore vectorStore) {

        var documentRetriever = VectorStoreDocumentRetriever.builder()
                .vectorStore(vectorStore)
                .similarityThreshold(0.50)
                .build();

        var queryAugmenter = ContextualQueryAugmenter.builder()
                .promptTemplate(new AssistantPromptTemplate(template))
                .allowEmptyContext(true)
                .build();

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
