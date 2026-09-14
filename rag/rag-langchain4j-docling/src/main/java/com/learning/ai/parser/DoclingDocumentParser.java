package com.learning.ai.parser;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentParser;
import java.io.InputStream;
import java.time.Duration;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

public class DoclingDocumentParser implements DocumentParser {

    private final String doclingServerUrl;
    private final RestTemplate restTemplate;

    /** Creates a parser with default connection and read timeouts. */
    public DoclingDocumentParser(String doclingServerUrl) {
        this(doclingServerUrl, Duration.ofSeconds(5), Duration.ofMinutes(2));
    }

    /** Creates a parser with explicit connection and read timeouts. */
    public DoclingDocumentParser(String doclingServerUrl, Duration connectTimeout, Duration readTimeout) {
        this.doclingServerUrl = doclingServerUrl;
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(connectTimeout);
        requestFactory.setReadTimeout(readTimeout);
        this.restTemplate = new RestTemplate(requestFactory);
    }

    /** Converts the supplied document stream to a LangChain4j document. */
    @Override
    public Document parse(InputStream inputStream) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);

            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("files", new InputStreamResource(inputStream) {
                /** Supplies the filename required for the multipart upload. */
                @Override
                public String getFilename() {
                    return "document.pdf"; // Mock filename for parser
                }
                /** Leaves content length unknown so the client can stream the body. */
                @Override
                public long contentLength() {
                    return -1; // Let RestTemplate read it
                }
            });

            HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

            // Assuming standard docling-serve endpoint is /convert/file or similar, and returns markdown
            ResponseEntity<String> response = restTemplate.postForEntity(doclingServerUrl + "/v1/convert/file?to=md", requestEntity, String.class);
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                // Here we would ideally parse the JSON if using Docling JSON format.
                // Assuming it returns Markdown directly or a JSON with a markdown field.
                return Document.from(response.getBody());
            } else {
                throw new RuntimeException("Docling parser failed with status: " + response.getStatusCode());
            }
        } catch (Exception e) {
            throw new RuntimeException("Error communicating with Docling service: " + e.getMessage(), e);
        }
    }
}
