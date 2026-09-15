package com.learning.ai.parser;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentParser;
import java.io.InputStream;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

public class DoclingDocumentParser implements DocumentParser {

    private final String doclingServerUrl;
    private final Duration connectTimeout;
    private final Duration readTimeout;

    public DoclingDocumentParser(String doclingServerUrl, Duration connectTimeout, Duration readTimeout) {
        this.doclingServerUrl = doclingServerUrl;
        this.connectTimeout = connectTimeout;
        this.readTimeout = readTimeout;
    }

    /**
     * Sends the supplied stream to Docling and wraps the response body as a LangChain4j document.
     *
     * @throws RuntimeException if Docling cannot be reached or does not return a successful response body
     */
    @Override
    public Document parse(InputStream inputStream) {
        try {
            byte[] fileBytes = inputStream.readAllBytes();
            String boundary = "----WebKitFormBoundary" + System.currentTimeMillis();

            String header = "--" + boundary + "\r\n"
                    + "Content-Disposition: form-data; name=\"files\"; filename=\"document.pdf\"\r\n"
                    + "Content-Type: application/pdf\r\n\r\n";
            String footer = "\r\n--" + boundary + "--\r\n";

            byte[] headerBytes = header.getBytes(java.nio.charset.StandardCharsets.UTF_8);
            byte[] footerBytes = footer.getBytes(java.nio.charset.StandardCharsets.UTF_8);
            byte[] body = new byte[headerBytes.length + fileBytes.length + footerBytes.length];
            System.arraycopy(headerBytes, 0, body, 0, headerBytes.length);
            System.arraycopy(fileBytes, 0, body, headerBytes.length, fileBytes.length);
            System.arraycopy(footerBytes, 0, body, headerBytes.length + fileBytes.length, footerBytes.length);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(java.net.URI.create(doclingServerUrl + "/v1/convert/file?to=md"))
                    .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                    .header("Accept", "application/json")
                    .timeout(readTimeout)
                    .POST(HttpRequest.BodyPublishers.ofByteArray(body))
                    .build();

            // Force HTTP/1.1 to avoid Uvicorn 400 Bad Request on HTTP/2 preface
            HttpResponse<String> response;
            try (HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(connectTimeout)
                    .version(HttpClient.Version.HTTP_1_1)
                    .build()) {

                response = client.send(request, HttpResponse.BodyHandlers.ofString());
            }

            if (response.statusCode() != 200) {
                throw new RuntimeException(
                        "Docling parser failed with status: " + response.statusCode() + " body: " + response.body());
            }

            ObjectMapper mapper = new ObjectMapper();

            String responseStr = response.body();
            // it's raw markdown
            if (responseStr.trim().startsWith("{")) {
                JsonNode node = mapper.readTree(responseStr);
                if (node.has("document")
                        && node.path("document").has("md_content")
                        && !node.path("document").path("md_content").isNull()) {
                    return Document.from(
                            node.path("document").path("md_content").asString());
                } else if (node.has("markdown")) {
                    return Document.from(node.path("markdown").asString());
                }
            }
            return Document.from(responseStr); // fallback to raw JSON

        } catch (Exception e) {
            throw new RuntimeException("Error communicating with Docling service: " + e.getMessage(), e);
        }
    }
}
