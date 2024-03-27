package com.learning.ai.config;

import static dev.langchain4j.data.document.loader.FileSystemDocumentLoader.loadDocument;

import com.zaxxer.hikari.HikariDataSource;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.document.parser.apache.pdfbox.ApachePdfBoxDocumentParser;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.embedding.AllMiniLmL6V2EmbeddingModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.openai.OpenAiChatModelName;
import dev.langchain4j.model.openai.OpenAiTokenizer;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.EmbeddingStoreIngestor;
import dev.langchain4j.store.embedding.pgvector.PgVectorEmbeddingStore;
import java.io.IOException;
import java.net.URI;
import javax.sql.DataSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

@Configuration(proxyBeanMethods = false)
public class AIConfig {

    @Bean
    AICustomerSupportAgent customerSupportAgent(
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

    //    @Bean
    //    ChatMemory chatMemory(Tokenizer tokenizer) {
    //        return TokenWindowChatMemory.withMaxTokens(1000, tokenizer);
    //    }

    @Bean
    ContentRetriever contentRetriever(EmbeddingStore<TextSegment> embeddingStore, EmbeddingModel embeddingModel) {

        // You will need to adjust these parameters to find the optimal setting, which will depend on two main factors:
        // - The nature of your data
        // - The embedding model you are using
        int maxResults = 1;
        double minScore = 0.6;

        return EmbeddingStoreContentRetriever.builder()
                .embeddingStore(embeddingStore)
                .embeddingModel(embeddingModel)
                .maxResults(maxResults)
                .minScore(minScore)
                .build();
    }

    @Bean
    EmbeddingModel embeddingModel() {
        return new AllMiniLmL6V2EmbeddingModel();
    }

    @Bean
    EmbeddingStore<TextSegment> embeddingStore(
            EmbeddingModel embeddingModel, ResourceLoader resourceLoader, DataSource dataSource) throws IOException {

        // Normally, you would already have your embedding store filled with your data.
        // However, for the purpose of this demonstration, we will:

        HikariDataSource hikariDataSource = (HikariDataSource) dataSource;
        String jdbcUrl = hikariDataSource.getJdbcUrl();
        URI uri = URI.create(jdbcUrl.substring(5));
        String host = uri.getHost();
        int dbPort = uri.getPort();
        String path = uri.getPath();
        // 1. Create an postgres embedding store
        // dimension of the embedding is 384 (all-minilm) and 1536 (openai)
        EmbeddingStore<TextSegment> embeddingStore = PgVectorEmbeddingStore.builder()
                .host(host)
                .port(dbPort != -1 ? dbPort : 5432)
                .user(hikariDataSource.getUsername())
                .password(hikariDataSource.getPassword())
                .database(path.substring(1))
                .table("ai_vector_store")
                .dimension(384)
                .build();

        // 2. Load an example document (medicaid-wa-faqs.pdf)
        Resource pdfResource = resourceLoader.getResource("classpath:medicaid-wa-faqs.pdf");
        Document document = loadDocument(pdfResource.getFile().toPath(), new ApachePdfBoxDocumentParser());

        // 3. Split the document into segments 500 tokens each
        // 4. Convert segments into embeddings
        // 5. Store embeddings into embedding store
        // All this can be done manually, but we will use EmbeddingStoreIngestor to automate this:
        DocumentSplitter documentSplitter =
                DocumentSplitters.recursive(500, 0, new OpenAiTokenizer(OpenAiChatModelName.GPT_3_5_TURBO.toString()));
        EmbeddingStoreIngestor ingestor = EmbeddingStoreIngestor.builder()
                .documentSplitter(documentSplitter)
                .embeddingModel(embeddingModel)
                .embeddingStore(embeddingStore)
                .build();
        ingestor.ingest(document);

        return embeddingStore;
    }
}
