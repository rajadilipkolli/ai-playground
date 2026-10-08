package com.learning.ai.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ai.docling.serve.api.DoclingServeApi;
import ai.docling.serve.api.convert.request.ConvertDocumentRequest;
import ai.docling.serve.api.convert.request.options.OutputFormat;
import ai.docling.serve.api.convert.response.DocumentResponse;
import ai.docling.serve.api.convert.response.InBodyConvertDocumentResponse;
import com.learning.ai.service.DocumentParserService;
import com.learning.ai.service.MetadataEnricher;
import com.learning.ai.service.StructureAwareChunker;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.core.io.ClassPathResource;

class LangChainConfigTests {

    @Test
    void usesAsyncConversionAndPreservesDocumentStructure() throws IOException {
        DoclingServeApi client = mock(DoclingServeApi.class);
        String markdown = "# Sample\n\nSample text.\n\n| Name | Value |\n| --- | --- |\n| Revenue | 100 |";
        when(client.convertSourceAsync(any(ConvertDocumentRequest.class)))
                .thenReturn(CompletableFuture.completedFuture(InBodyConvertDocumentResponse.builder()
                        .document(DocumentResponse.builder()
                                .markdownContent(markdown)
                                .build())
                        .build()));

        var service = new DocumentParserService(new LangChainConfig().doclingDocumentParser(client));
        try (var input = new ClassPathResource("file-sample_150kB.pdf").getInputStream()) {
            var document = service.parse(input);
            assertThat(document.text()).isEqualTo(markdown);

            var segments = new MetadataEnricher()
                    .enrich(new StructureAwareChunker(300, 50).chunk(document), "file-sample_150kB.pdf", "hash");
            assertThat(segments)
                    .extracting(segment -> segment.metadata().getString("element_type"))
                    .containsExactly("heading", "text", "table");
            assertThat(segments)
                    .allSatisfy(segment -> assertThat(segment.metadata().getString("section_path"))
                            .isEqualTo("root/Sample"));
        }

        var request = ArgumentCaptor.forClass(ConvertDocumentRequest.class);
        verify(client).convertSourceAsync(request.capture());
        assertThat(request.getValue().getOptions().getToFormats()).containsExactly(OutputFormat.MARKDOWN);
        assertThat(request.getValue().getSources()).hasSize(1);
        verify(client, never()).convertSource(any(ConvertDocumentRequest.class));
    }

    @Test
    void fallsBackToPdfBoxWhenAsyncConversionFails() throws IOException {
        DoclingServeApi client = mock(DoclingServeApi.class);
        when(client.convertSourceAsync(any(ConvertDocumentRequest.class)))
                .thenReturn(CompletableFuture.failedFuture(new IllegalStateException("Conversion failed")));

        var service = new DocumentParserService(new LangChainConfig().doclingDocumentParser(client));
        try (var input = new ClassPathResource("file-sample_150kB.pdf").getInputStream()) {
            assertThat(service.parse(input).text()).contains("Lorem ipsum");
        }

        verify(client).convertSourceAsync(any(ConvertDocumentRequest.class));
    }
}
