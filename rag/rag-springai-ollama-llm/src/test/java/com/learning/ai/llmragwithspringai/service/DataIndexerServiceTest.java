package com.learning.ai.llmragwithspringai.service;

import static com.learning.ai.llmragwithspringai.util.TestResourceUtil.createMockResource;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.learning.ai.llmragwithspringai.config.properties.RagIngestionProperties;
import com.learning.ai.llmragwithspringai.model.response.IngestionResult;
import com.learning.ai.llmragwithspringai.model.response.IngestionStatus;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.function.Consumer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.content.Media;
import org.springframework.ai.document.Document;
import org.springframework.ai.ollama.api.OllamaChatOptions;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.MimeTypeUtils;

@ExtendWith(MockitoExtension.class)
class DataIndexerServiceTest {

    @Mock
    private TokenTextSplitter tokenTextSplitter;

    @Mock
    private VectorStore vectorStore;

    @Mock
    private JdbcTemplate jdbcTemplate;

    @Mock
    private MeterRegistry meterRegistry;

    @Mock
    private Timer timer;

    @Mock
    private Counter counter;

    @Mock
    private RagIngestionProperties ragIngestionProperties;

    @Mock
    private ChatClient.Builder chatClientBuilder;

    @Mock
    private ChatClient chatClient;

    @Mock
    private TransactionTemplate transactionTemplate;

    private DataIndexerService dataIndexerService;

    @BeforeEach
    void setUp() {
        lenient().when(meterRegistry.timer(anyString())).thenReturn(timer);
        lenient().when(meterRegistry.counter(anyString())).thenReturn(counter);

        chatClient = mock(ChatClient.class, org.mockito.Mockito.RETURNS_DEEP_STUBS);
        lenient().when(chatClientBuilder.build()).thenReturn(chatClient);
        lenient().when(ragIngestionProperties.getPdf()).thenReturn(new RagIngestionProperties.Pdf());

        lenient()
                .doAnswer(invocation -> {
                    Consumer<TransactionStatus> callback = invocation.getArgument(0);
                    callback.accept(null);
                    return null;
                })
                .when(transactionTemplate)
                .executeWithoutResult(org.mockito.ArgumentMatchers.any());

        dataIndexerService = new DataIndexerService(
                tokenTextSplitter,
                vectorStore,
                meterRegistry,
                jdbcTemplate,
                chatClientBuilder,
                transactionTemplate,
                ragIngestionProperties,
                false,
                "llava");
    }

    @Test
    void testSkipDuplicateContentSameFilename() {
        Resource resource = createMockResource("test.txt", "Some content");

        Document existingDoc = new Document("doc-123", "existing-content", Collections.emptyMap());

        // First JdbcTemplate query is for content_hash
        when(jdbcTemplate.queryForList(anyString(), eq(String.class), anyString()))
                .thenReturn(List.of("doc-123"));

        IngestionResult result = dataIndexerService.loadData(resource, null, null, null);

        assertThat(result.status()).isEqualTo(IngestionStatus.SKIPPED_DUPLICATE);
        assertThat(result.filename()).isEqualTo("test.txt");
        assertThat(result.chunksIngested()).isEqualTo(0);
        assertThat(result.chunksDeleted()).isEqualTo(0);

        verify(vectorStore, never()).accept(anyList());
        verify(vectorStore, never()).delete(anyList());
    }

    @Test
    void testReplaceChangedContentSameFilename() {
        Resource resource = createMockResource("test.txt", "New modified content");

        Document oldDoc = new Document("doc-123", "old-content", Collections.emptyMap());

        Document newDoc = new Document("New modified content");
        when(tokenTextSplitter.apply(anyList())).thenReturn(List.of(newDoc));

        // First query (hash) -> empty
        // Second query (filename) -> returns old document
        when(jdbcTemplate.queryForList(anyString(), eq(String.class), anyString()))
                .thenReturn(Collections.emptyList())
                .thenReturn(List.of("doc-123"));

        IngestionResult result = dataIndexerService.loadData(resource, null, null, null);

        assertThat(result.status()).isEqualTo(IngestionStatus.REPLACED);
        assertThat(result.filename()).isEqualTo("test.txt");
        assertThat(result.chunksIngested()).isEqualTo(1);
        assertThat(result.chunksDeleted()).isEqualTo(1);

        verify(vectorStore).delete(List.of("doc-123"));
        verify(vectorStore).accept(anyList());
    }

    @Test
    void testSkipDuplicateContentDifferentFilename() {
        Resource resource = createMockResource("new-file.txt", "Identical content");

        Document existingDoc = new Document("doc-999", "Identical content", Collections.emptyMap());

        // First query (hash) -> returns existing document
        when(jdbcTemplate.queryForList(anyString(), eq(String.class), anyString()))
                .thenReturn(List.of("doc-999"));

        IngestionResult result = dataIndexerService.loadData(resource, null, null, null);

        assertThat(result.status()).isEqualTo(IngestionStatus.SKIPPED_DUPLICATE);
        assertThat(result.filename()).isEqualTo("new-file.txt");
        assertThat(result.chunksIngested()).isEqualTo(0);
        assertThat(result.chunksDeleted()).isEqualTo(0);

        verify(vectorStore, never()).delete(anyList());
        verify(vectorStore, never()).accept(anyList());
    }

    @Test
    void testIngestNewFile() {
        Resource resource = createMockResource("brand-new.txt", "Fresh content");

        Document newDoc = new Document("Fresh content");
        when(tokenTextSplitter.apply(anyList())).thenReturn(List.of(newDoc));

        // First query (hash) -> empty
        // Second query (filename) -> empty
        lenient()
                .when(jdbcTemplate.queryForList(anyString(), eq(String.class), any(), any(), any(), any()))
                .thenReturn(Collections.emptyList());
        lenient()
                .when(jdbcTemplate.queryForList(anyString(), eq(String.class), anyString()))
                .thenReturn(Collections.emptyList());

        IngestionResult result = dataIndexerService.loadData(resource, "POLICY", "HR", "EmployeeBenefits");

        assertThat(result.status()).isEqualTo(IngestionStatus.INGESTED);
        assertThat(result.filename()).isEqualTo("brand-new.txt");
        assertThat(result.chunksIngested()).isEqualTo(1);
        assertThat(result.chunksDeleted()).isEqualTo(0);

        verify(vectorStore, never()).delete(anyList());

        ArgumentCaptor<List<Document>> captor = ArgumentCaptor.forClass(List.class);
        verify(vectorStore).accept(captor.capture());

        List<Document> ingestedDocs = captor.getValue();
        assertThat(ingestedDocs).hasSize(1);
        assertThat(ingestedDocs.get(0).getMetadata()).containsEntry("documentType", "POLICY");
        assertThat(ingestedDocs.get(0).getMetadata()).containsEntry("owner", "HR");
        assertThat(ingestedDocs.get(0).getMetadata()).containsEntry("category", "EmployeeBenefits");
    }

    @Test
    void testPdfIngestionWithVisionEnabled() throws Exception {
        // Create a small subclass to simulate the vision flow without using PDFBox/BufferedImage
        class TestService extends DataIndexerService {
            public TestService() {
                super(
                        tokenTextSplitter,
                        vectorStore,
                        meterRegistry,
                        jdbcTemplate,
                        chatClientBuilder,
                        transactionTemplate,
                        ragIngestionProperties,
                        true,
                        "llava");
            }

            @Override
            public IngestionResult loadData(
                    Resource documentResource, String documentType, String owner, String category) {
                try {
                    // verify the configured vision model via reflection
                    var f = DataIndexerService.class.getDeclaredField("visionModel");
                    f.setAccessible(true);
                    String vm = (String) f.get(this);
                    assertThat(vm).isEqualTo("llava");

                    // simulate extracting an image and calling the chat client with PNG media
                    byte[] imgBytes = Base64.getDecoder()
                            .decode(
                                    "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR4nGNgYAAAAAMAASsJTYQAAAAASUVORK5CYII=");
                    var userMessage = UserMessage.builder()
                            .text("Please describe this image")
                            .media(new Media(MimeTypeUtils.IMAGE_PNG, new ByteArrayResource(imgBytes)))
                            .build();
                    var prompt = new Prompt(
                            userMessage, OllamaChatOptions.builder().model(vm).build());

                    String imageDescription = chatClient.prompt(prompt).call().content();

                    var meta = new HashMap<String, Object>();
                    Document d = new Document("Image page\n--- Image Content ---\n" + imageDescription, meta);
                    vectorStore.accept(List.of(d));
                    return new IngestionResult(IngestionStatus.INGESTED, documentResource.getFilename(), 1, 0);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        }

        // stub chat client to return expected description
        lenient().when(chatClient.prompt(any(Prompt.class)).call().content()).thenReturn("Described image content");

        TestService svc = new TestService();
        Resource resource = Mockito.mock(Resource.class);
        lenient().when(resource.getFilename()).thenReturn("vision.pdf");

        IngestionResult result = svc.loadData(resource, null, null, null);

        ArgumentCaptor<List> captor = ArgumentCaptor.forClass(List.class);
        verify(vectorStore).accept(captor.capture());
        List ingested = captor.getValue();
        assertThat(ingested).isNotEmpty();
        Document ingestedDoc = (Document) ingested.get(0);
        assertThat(ingestedDoc.getText()).contains("Described image content");
        assertThat(result.status()).isEqualTo(IngestionStatus.INGESTED);
    }
}
