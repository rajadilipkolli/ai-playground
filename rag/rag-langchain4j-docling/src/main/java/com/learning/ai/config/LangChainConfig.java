package com.learning.ai.config;

import ai.docling.serve.api.DoclingServeApi;
import ai.docling.serve.api.convert.request.ConvertDocumentRequest;
import ai.docling.serve.api.convert.request.options.ConvertDocumentOptions;
import ai.docling.serve.api.convert.request.options.OutputFormat;
import dev.langchain4j.data.document.DocumentParser;
import dev.langchain4j.data.document.parser.docling.DoclingDocumentParser;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.embedding.onnx.allminilml6v2.AllMiniLmL6V2EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.pgvector.PgVectorEmbeddingStore;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import javax.sql.DataSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class LangChainConfig {

    /** Creates the local embedding model used for indexing and retrieval. */
    @Bean
    EmbeddingModel embeddingModel() {
        return new AllMiniLmL6V2EmbeddingModel();
    }

    /** Creates the PostgreSQL vector store from datasource. */
    @Bean
    EmbeddingStore<TextSegment> embeddingStore(DataSource dataSource) {
        return PgVectorEmbeddingStore.datasourceBuilder()
                .datasource(dataSource)
                .table("vector_store")
                .createTable(false)
                .dimension(384)
                .build();
    }

    @Bean
    DoclingServeApi doclingServeApi(DoclingProperties doclingProperties) {
        return DoclingServeApi.builder()
                .baseUrl(doclingProperties.getServerUrl())
                .logRequests()
                .logResponses()
                .prettyPrint()
                .build();
    }

    @Bean
    DocumentParser doclingDocumentParser(DoclingServeApi doclingServeApi) {
        Executor executor = Executors.newVirtualThreadPerTaskExecutor();
        return DoclingDocumentParser.builder()
                .doclingClient(doclingServeApi)
                .documentRequest(ConvertDocumentRequest.builder()
                        .options(ConvertDocumentOptions.builder()
                                .toFormat(OutputFormat.MARKDOWN)
                                .build())
                        .build())
                .requestExecutor((client, request) -> CompletableFuture.supplyAsync(
                        () -> client.convertSource((ConvertDocumentRequest) request), executor))
                .build();
    }
}
