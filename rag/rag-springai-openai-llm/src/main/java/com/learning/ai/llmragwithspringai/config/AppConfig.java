package com.learning.ai.llmragwithspringai.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.reader.ExtractedTextFormatter;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.jdbc.core.JdbcTemplate;

@Configuration(proxyBeanMethods = false)
class AppConfig {
    private static final Logger log = LoggerFactory.getLogger(AppConfig.class);

    @Value("classpath:Rohit_Gurunath_Sharma.docx")
    private Resource resource;

    @Bean
    TokenTextSplitter tokenTextSplitter() {
        return new TokenTextSplitter();
    }

    @Bean
    ApplicationRunner runner(VectorStore vectorStore, JdbcTemplate template, TokenTextSplitter tokenTextSplitter) {
        return args -> {
            log.info("Loading file(s) as Documents");
            ExtractedTextFormatter textFormatter = ExtractedTextFormatter.builder()
                    .withNumberOfBottomTextLinesToDelete(3)
                    .withNumberOfTopPagesToSkipBeforeDelete(1)
                    .build();
            TikaDocumentReader documentReader = new TikaDocumentReader(resource, textFormatter);
            template.update("delete from vector_store");
            vectorStore.accept(tokenTextSplitter.apply(documentReader.get()));
            log.info("Loaded document to database.");
        };
    }
}
