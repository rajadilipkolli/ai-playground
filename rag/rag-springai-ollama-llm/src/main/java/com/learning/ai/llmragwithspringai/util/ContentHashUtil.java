package com.learning.ai.llmragwithspringai.util;

import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;

public class ContentHashUtil {

    private ContentHashUtil() {
        // Stateless utility class
    }

    public record HashResult(String hash, Resource rereadableResource) {}

    public static String getSha256Hash(String text) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(text.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashBytes);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }

    public static HashResult calculateHash(Resource resource) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = null;
            if (resource.isOpen()) {
                try (InputStream is = resource.getInputStream()) {
                    bytes = is.readAllBytes();
                }
                digest.update(bytes);
            } else {
                byte[] buffer = new byte[8192];
                int bytesRead;
                try (InputStream is = resource.getInputStream()) {
                    while ((bytesRead = is.read(buffer)) != -1) {
                        digest.update(buffer, 0, bytesRead);
                    }
                }
            }
            String hash = HexFormat.of().formatHex(digest.digest());

            Resource rereadableResource = bytes != null
                    ? new ByteArrayResource(bytes) {
                        @Override
                        public String getFilename() {
                            return resource.getFilename();
                        }
                    }
                    : resource;

            return new HashResult(hash, rereadableResource);
        } catch (IOException e) {
            throw new RuntimeException("Failed to read resource for hashing: " + resource.getFilename(), e);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }
}
