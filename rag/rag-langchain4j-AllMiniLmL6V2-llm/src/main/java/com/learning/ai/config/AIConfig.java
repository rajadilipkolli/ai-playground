package com.learning.ai.config;

import static dev.langchain4j.data.document.loader.FileSystemDocumentLoader.loadDocument;

import com.learning.ai.service.AICustomerSupportAgent;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.document.parser.apache.pdfbox.ApachePdfBoxDocumentParser;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.listener.ChatModelErrorContext;
import dev.langchain4j.model.chat.listener.ChatModelListener;
import dev.langchain4j.model.chat.listener.ChatModelRequestContext;
import dev.langchain4j.model.chat.listener.ChatModelResponseContext;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.embedding.onnx.allminilml6v2.AllMiniLmL6V2EmbeddingModel;
import dev.langchain4j.model.openai.OpenAiChatModelName;
import dev.langchain4j.model.openai.OpenAiTokenizer;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.EmbeddingStoreIngestor;
import dev.langchain4j.store.embedding.pgvector.PgVectorEmbeddingStore;
import io.micrometer.core.instrument.MeterRegistry;
import java.io.IOException;
import java.net.URI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.jdbc.autoconfigure.JdbcConnectionDetails;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

@Configuration(proxyBeanMethods = false)
class AIConfig {

    private static final Logger log = LoggerFactory.getLogger(AIConfig.class);

    @Value("${langchain4j.rag.chunking.size:300}")
    private int chunkSize;

    @Value("${langchain4j.rag.chunking.overlap:50}")
    private int chunkOverlap;

    @Value("${langchain4j.rag.ingest.enabled:false}")
    private boolean ingestEnabled;

    @Bean
    AICustomerSupportAgent aiCustomerSupportAgent(
            ChatLanguageModel chatLanguageModel,
            ChatTools chatAssistantTools,
            ContentRetriever contentRetriever,
            ChatMemory chatMemory) {
        return AiServices.builder(AICustomerSupportAgent.class)
                .chatLanguageModel(chatLanguageModel)
                .chatMemory(chatMemory)
                .tools(chatAssistantTools)
                .contentRetriever(contentRetriever)
                .build();
    }

    @Bean
    ChatMemory chatMemory() {
        return MessageWindowChatMemory.withMaxMessages(15);
    }

    @Bean
    EmbeddingModel embeddingModel() {
        return new AllMiniLmL6V2EmbeddingModel();
    }

    @Bean
    OpenAiTokenizer openAiTokenizer() {
        return new OpenAiTokenizer(OpenAiChatModelName.GPT_3_5_TURBO.toString());
    }

    @Bean
    ChatModelListener chatModelListener(MeterRegistry meterRegistry) {
        return new ChatModelListener() {
            @Override
            public void onRequest(ChatModelRequestContext requestContext) {
                log.info("Sending request to LLM: {}", requestContext.request().messages());
                meterRegistry.counter("llm.requests").increment();
            }

            @Override
            public void onResponse(ChatModelResponseContext responseContext) {
                log.info("Received response from LLM");
                meterRegistry.counter("llm.responses").increment();
            }

            @Override
            public void onError(ChatModelErrorContext errorContext) {
                log.error("Error during LLM call", errorContext.error());
                meterRegistry.counter("llm.errors").increment();
            }
        };
    }

    @Bean
    EmbeddingStore<TextSegment> embeddingStore(
            EmbeddingModel embeddingModel,
            ResourceLoader resourceLoader,
            JdbcConnectionDetails jdbcConnectionDetails,
            OpenAiTokenizer openAiTokenizer)
            throws IOException {

        // Normally, you would already have your embedding store filled with your data.
        // However, for the purpose of this demonstration, we will:

        String jdbcUrl = jdbcConnectionDetails.getJdbcUrl();
        URI uri = URI.create(jdbcUrl.substring(5));
        String host = uri.getHost();
        int dbPort = uri.getPort();
        String path = uri.getPath();
        // 1. Create an postgres embedding store
        // dimension of the embedding is 384 (all-minilm) and 1536 (openai)
        EmbeddingStore<TextSegment> embeddingStore = PgVectorEmbeddingStore.builder()
                .host(host)
                .port(dbPort != -1 ? dbPort : 5432)
                .user(jdbcConnectionDetails.getUsername())
                .password(jdbcConnectionDetails.getPassword())
                .database(path.substring(1))
                .table("ai_vector_store")
                .dropTableFirst(false)
                .dimension(384)
                .build();

        if (ingestEnabled) {
            log.info("Ingesting document into vector store...");
            Resource pdfResource = resourceLoader.getResource("classpath:Rohit.pdf");
            Document document = loadDocument(pdfResource.getFile().toPath(), new ApachePdfBoxDocumentParser());

            DocumentSplitter documentSplitter = DocumentSplitters.recursive(chunkSize, chunkOverlap, openAiTokenizer);
            EmbeddingStoreIngestor ingestor = EmbeddingStoreIngestor.builder()
                    .documentSplitter(documentSplitter)
                    .embeddingModel(embeddingModel)
                    .embeddingStore(embeddingStore)
                    .build();
            ingestor.ingest(document);
            log.info("Document ingestion complete.");
        } else {
            log.info("Document ingestion skipped (langchain4j.rag.ingest.enabled=false).");
        }

        return embeddingStore;
    }
}
