package com.learning.ai.reactrag.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;

public class ContentHashUtil {

    private static final int MAX_BYTES = 10 * 1024 * 1024; // 10 MB limit

    private ContentHashUtil() {
        // Stateless utility class
    }

    public record HashResult(String hash, Resource rereadableResource) {}

    public static HashResult calculateHash(Resource resource) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            byte[] buffer = new byte[8192];
            int bytesRead;
            int totalBytes = 0;
            try (InputStream is = resource.getInputStream()) {
                while ((bytesRead = is.read(buffer)) != -1) {
                    totalBytes += bytesRead;
                    if (totalBytes > MAX_BYTES) {
                        throw new IllegalArgumentException(
                                "Resource exceeds maximum allowed size of " + MAX_BYTES + " bytes");
                    }
                    digest.update(buffer, 0, bytesRead);
                    baos.write(buffer, 0, bytesRead);
                }
            }
            String hash = HexFormat.of().formatHex(digest.digest());

            Resource rereadableResource = new ByteArrayResource(baos.toByteArray()) {
                @Override
                public String getFilename() {
                    return resource.getFilename();
                }
            };

            return new HashResult(hash, rereadableResource);
        } catch (IOException e) {
            throw new RuntimeException("Failed to read resource for hashing: " + resource.getFilename(), e);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }
}
