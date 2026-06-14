package com.learning.ai.llmragwithspringai.rag.retrieval;

import static org.assertj.core.api.Assertions.assertThat;

import com.learning.ai.llmragwithspringai.config.AbstractIntegrationTest;
import com.learning.ai.llmragwithspringai.model.request.AIChatRequest;
import com.learning.ai.llmragwithspringai.model.response.AIChatResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class HybridDocumentRetrieverConcurrencyIntTest extends AbstractIntegrationTest {

    @Value("classpath:Rohit_Gurunath_Sharma.pdf")
    private Resource pdfResource;

    @BeforeAll
    void setUp() {
        if (dataIndexerService.isEmpty()) {
            dataIndexerService.loadData(pdfResource, "profile", "cricket_board", "sports");
        }
    }

    @Test
    void shouldHandleConcurrentRequestsWithoutExceptions() throws Exception {
        int numConcurrentRequests = 50;
        ExecutorService executorService = Executors.newFixedThreadPool(10);
        try {
            List<CompletableFuture<AIChatResponse>> futures = new ArrayList<>();

            for (int i = 0; i < numConcurrentRequests; i++) {
                CompletableFuture<AIChatResponse> future = CompletableFuture.supplyAsync(
                        () -> {
                            AIChatRequest request =
                                    new AIChatRequest("Who is Rohit Sharma?", null, "cricket_board", "sports", null);
                            return aiChatService.chat(request, false);
                        },
                        executorService);
                futures.add(future);
            }

            CompletableFuture<Void> allFutures = CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
            // Allow up to 15 minutes for 50 requests if they process sequentially
            // depending on Ollama container specs.
            allFutures.get(15, TimeUnit.MINUTES);

            for (CompletableFuture<AIChatResponse> future : futures) {
                AIChatResponse response = future.getNow(null);

                // Assert all requests complete without exceptions
                assertThat(response).isNotNull();
                assertThat(response.queryResponse()).isNotBlank();
            }
        } finally {
            executorService.shutdownNow();
            if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        }
    }
}
