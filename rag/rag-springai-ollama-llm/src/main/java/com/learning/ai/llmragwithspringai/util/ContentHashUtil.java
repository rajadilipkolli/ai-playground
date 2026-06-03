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

    public static HashResult calculateHash(Resource resource) {
        try (InputStream is = resource.getInputStream()) {
            byte[] bytes = is.readAllBytes();
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(bytes);
            String hash = HexFormat.of().formatHex(hashBytes);

            Resource rereadableResource = new ByteArrayResource(bytes) {
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
