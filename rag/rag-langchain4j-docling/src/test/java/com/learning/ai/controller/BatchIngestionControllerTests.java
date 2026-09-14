package com.learning.ai.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.learning.ai.repository.IngestionJobRepository;
import com.learning.ai.service.BatchIngestionService;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;

class BatchIngestionControllerTests {

    @TempDir
    Path tempDirectory;

    /** Verifies that directory traversal outside the configured base is rejected. */
    @Test
    void rejectsDirectoriesOutsideConfiguredBase() throws Exception {
        Path allowed = Files.createDirectory(tempDirectory.resolve("allowed"));
        Path outside = Files.createDirectory(tempDirectory.resolve("outside"));
        BatchIngestionService service = mock(BatchIngestionService.class);
        BatchIngestionController controller =
                new BatchIngestionController(service, new IngestionJobRepository(), allowed.toString());

        var response = controller.ingestDirectory(outside.toString());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        verifyNoInteractions(service);
    }

    /** Verifies that PDF files under the configured base are accepted. */
    @Test
    void acceptsPdfFilesUnderConfiguredBase() throws Exception {
        Path allowed = Files.createDirectory(tempDirectory.resolve("allowed"));
        Files.writeString(allowed.resolve("document.pdf"), "test");
        BatchIngestionService service = mock(BatchIngestionService.class);
        BatchIngestionController controller =
                new BatchIngestionController(service, new IngestionJobRepository(), allowed.toString());

        var response = controller.ingestDirectory(allowed.toString());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
        verify(service).processDirectory(anyString(), anyList());
    }

    /** Verifies that directory ingestion requires the administrator role. */
    @Test
    void requiresAdminRoleForDirectoryIngestion() throws Exception {
        Method method = BatchIngestionController.class.getMethod("ingestDirectory", String.class);

        assertThat(method.getAnnotation(PreAuthorize.class).value()).isEqualTo("hasRole('ADMIN')");
    }
}
