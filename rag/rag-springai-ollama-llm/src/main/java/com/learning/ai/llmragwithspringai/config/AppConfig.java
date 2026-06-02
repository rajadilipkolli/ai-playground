package com.learning.ai.llmragwithspringai.config;

import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class AppConfig {

    @Value("${rag.chunking.size:300}")
    private int chunkSize;

    @Value("${rag.chunking.minSize:100}")
    private int chunkMinSize;

    // Rationale: We use a default chunkSize of 300 to balance context richness and retrieval precision.
    // Nomic-embed-text supports a large context window, but chunks around 300-500 tokens generally yield the best RAG
    // results.
    // Note on Splitter Differences: Spring AI's TokenTextSplitter differs from LangChain4j's recursive splitter.
    // It does not have a direct 'token overlap' parameter; instead, it uses size constraints
    // and attempts to preserve semantic boundaries (like sentences).
    @Bean
    TokenTextSplitter tokenTextSplitter() {
        return TokenTextSplitter.builder()
                .withChunkSize(chunkSize)
                .withMinChunkSizeChars(chunkMinSize)
                .build();
    }
}
