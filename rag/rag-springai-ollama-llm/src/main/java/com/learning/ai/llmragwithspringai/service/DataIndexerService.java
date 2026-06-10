package com.learning.ai.llmragwithspringai.service;

import com.learning.ai.llmragwithspringai.model.response.IngestionResult;
import com.learning.ai.llmragwithspringai.model.response.IngestionStatus;
import com.learning.ai.llmragwithspringai.util.ContentHashUtil;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.observation.annotation.Observed;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.imageio.ImageIO;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDResources;
import org.apache.pdfbox.pdmodel.graphics.PDXObject;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.text.PDFTextStripper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.content.Media;
import org.springframework.ai.document.Document;
import org.springframework.ai.document.DocumentReader;
import org.springframework.ai.document.DocumentTransformer;
import org.springframework.ai.ollama.api.OllamaChatOptions;
import org.springframework.ai.reader.ExtractedTextFormatter;
import org.springframework.ai.reader.JsonReader;
import org.springframework.ai.reader.TextReader;
import org.springframework.ai.reader.pdf.PagePdfDocumentReader;
import org.springframework.ai.reader.pdf.config.PdfDocumentReaderConfig;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.MimeTypeUtils;
import org.springframework.util.StopWatch;

@Service
public class DataIndexerService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DataIndexerService.class);

    private final TokenTextSplitter tokenTextSplitter;
    private final VectorStore vectorStore;
    private final MeterRegistry meterRegistry;
    private final JdbcTemplate jdbcTemplate;
    private final ChatClient chatClient;
    private final boolean visionEnabled;
    private final String visionModel;

    public DataIndexerService(
            TokenTextSplitter tokenTextSplitter,
            VectorStore vectorStore,
            MeterRegistry meterRegistry,
            JdbcTemplate jdbcTemplate,
            ChatClient.Builder chatClientBuilder,
            @Value("${rag.ingestion.vision.enabled:false}") boolean visionEnabled,
            @Value("${rag.ingestion.vision.model:llava}") String visionModel) {
        this.tokenTextSplitter = tokenTextSplitter;
        this.vectorStore = vectorStore;
        this.meterRegistry = meterRegistry;
        this.jdbcTemplate = jdbcTemplate;
        this.chatClient = chatClientBuilder.build();
        this.visionEnabled = visionEnabled;
        this.visionModel = visionModel;
    }

    @Observed(name = "rag.ingest", contextualName = "rag-ingest")
    @Transactional
    public IngestionResult loadData(Resource documentResource) {
        String filename = documentResource.getFilename();
        if (filename == null) {
            filename = "unknown";
        }

        StopWatch stopWatch = new StopWatch("loadData");
        stopWatch.start();
        ContentHashUtil.HashResult hashResult = ContentHashUtil.calculateHash(documentResource);
        String contentHash = hashResult.hash();
        final Resource rereadableResource = hashResult.rereadableResource();

        List<String> existingByHash = findDocumentsByContentHash(contentHash);
        if (!existingByHash.isEmpty()) {
            LOGGER.info("Document {} with hash {} already exists. Skipping ingestion.", filename, contentHash);
            return new IngestionResult(IngestionStatus.SKIPPED_DUPLICATE, filename, 0, 0);
        }

        List<String> existingByFilename = findDocumentsByFilename(filename);
        int chunksDeleted = 0;
        if (!existingByFilename.isEmpty()) {
            LOGGER.info(
                    "Document {} exists with different hash. Replacing {} old chunks.",
                    filename,
                    existingByFilename.size());
            vectorStore.delete(existingByFilename);
            chunksDeleted = existingByFilename.size();
        }

        String ingestedAt = DateTimeFormatter.ISO_INSTANT.format(Instant.now());

        DocumentReader documentReader = null;
        List<Document> rawDocuments = null;

        if (filename.endsWith(".pdf")) {
            LOGGER.info("Loading PDF document");
            if (visionEnabled) {
                LOGGER.info("Vision-based ingestion is enabled. Extracting images from PDF.");
                rawDocuments = readPdfWithVision(rereadableResource);
            } else {
                PdfDocumentReaderConfig pdfDocumentReaderConfig = PdfDocumentReaderConfig.builder()
                        .withPageExtractedTextFormatter(ExtractedTextFormatter.builder()
                                .withNumberOfBottomTextLinesToDelete(3)
                                .withNumberOfTopPagesToSkipBeforeDelete(1)
                                .build())
                        .withPagesPerDocument(0)
                        .build();
                documentReader = new PagePdfDocumentReader(rereadableResource, pdfDocumentReaderConfig);
            }
        } else if (filename.endsWith(".txt")) {
            documentReader = new TextReader(rereadableResource);
        } else if (filename.endsWith(".json")) {
            documentReader = new JsonReader(rereadableResource);
        }

        if (documentReader != null) {
            rawDocuments = documentReader.get();
        }

        if (rawDocuments != null && !rawDocuments.isEmpty()) {
            LOGGER.info("Loading document content to vector database");
            DocumentTransformer metadataEnricher = documents -> {
                final String finalFilename =
                        rereadableResource.getFilename() != null ? rereadableResource.getFilename() : "unknown";
                documents.forEach(d -> {
                    Map<String, Object> metadata = d.getMetadata();
                    metadata.put("EXTERNAL_KNOWLEDGE", "true");
                    metadata.put("source_filename", finalFilename);
                    metadata.put("content_hash", contentHash);
                    metadata.put("ingested_at", ingestedAt);
                });
                return documents;
            };

            List<Document> docsToIngest = metadataEnricher.apply(tokenTextSplitter.apply(rawDocuments));
            vectorStore.accept(docsToIngest);

            stopWatch.stop();
            LOGGER.info(
                    "Loaded {} chunks to vector database in {} ms.",
                    docsToIngest.size(),
                    stopWatch.getTotalTimeMillis());
            meterRegistry.timer("rag.ingestion.latency").record(Duration.ofMillis(stopWatch.getTotalTimeMillis()));
            meterRegistry.counter("rag.documents.ingested").increment(docsToIngest.size());

            IngestionStatus status = chunksDeleted > 0 ? IngestionStatus.REPLACED : IngestionStatus.INGESTED;
            return new IngestionResult(status, filename, docsToIngest.size(), chunksDeleted);
        }

        return new IngestionResult(IngestionStatus.SKIPPED_DUPLICATE, filename, 0, 0); // fallback
    }

    private List<Document> readPdfWithVision(Resource resource) {
        List<Document> documents = new ArrayList<>();
        try (InputStream is = resource.getInputStream();
                PDDocument document = Loader.loadPDF(is.readAllBytes())) {
            PDFTextStripper stripper = new PDFTextStripper();

            int pageCount = document.getNumberOfPages();
            for (int i = 0; i < pageCount; i++) {
                PDPage page = document.getPage(i);

                stripper.setStartPage(i + 1);
                stripper.setEndPage(i + 1);
                String text = stripper.getText(document);

                StringBuilder pageContent = new StringBuilder(text);

                PDResources pdResources = page.getResources();
                for (org.apache.pdfbox.cos.COSName cosName : pdResources.getXObjectNames()) {
                    PDXObject xObject = pdResources.getXObject(cosName);
                    if (xObject instanceof PDImageXObject image) {
                        BufferedImage bufferedImage = image.getImage();

                        ByteArrayOutputStream baos = new ByteArrayOutputStream();
                        ImageIO.write(bufferedImage, "png", baos);
                        byte[] imageBytes = baos.toByteArray();

                        String promptText =
                                "Please describe this image in detail. Extract any textual content, tables, or structural data exactly as they appear.";
                        UserMessage userMessage = UserMessage.builder()
                                .text(promptText)
                                .media(new Media(MimeTypeUtils.IMAGE_PNG, new ByteArrayResource(imageBytes)))
                                .build();

                        Prompt prompt = new Prompt(
                                userMessage,
                                OllamaChatOptions.builder().model(visionModel).build());

                        LOGGER.info("Calling Ollama vision model ({}) for image on page {}", visionModel, i + 1);
                        String imageDescription =
                                chatClient.prompt(prompt).call().content();

                        pageContent.append("\n\n--- Image Content ---\n");
                        pageContent.append(imageDescription);
                        pageContent.append("\n---------------------\n");
                    }
                }

                Map<String, Object> metadata = new HashMap<>();
                metadata.put("page_number", i + 1);
                documents.add(new Document(pageContent.toString(), metadata));
            }
        } catch (Exception e) {
            LOGGER.error("Failed to extract text and images from PDF using vision model", e);
            throw new IllegalStateException("Vision-based PDF ingestion failed", e);
        }
        return documents;
    }

    private List<String> findDocumentsByContentHash(String hash) {
        return jdbcTemplate.queryForList(
                "SELECT id FROM vector_store WHERE metadata->>'content_hash' = ?", String.class, hash);
    }

    private List<String> findDocumentsByFilename(String filename) {
        return jdbcTemplate.queryForList(
                "SELECT id FROM vector_store WHERE metadata->>'source_filename' = ?", String.class, filename);
    }

    @Observed(name = "rag.count", contextualName = "rag-count")
    public long count() {
        Long count = this.jdbcTemplate.queryForObject("SELECT COUNT(1) FROM vector_store", Long.class);
        return count != null ? count : 0L;
    }

    public boolean isEmpty() {
        return count() == 0;
    }
}
