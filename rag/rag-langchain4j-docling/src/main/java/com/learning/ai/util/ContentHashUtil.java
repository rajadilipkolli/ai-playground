package com.learning.ai.util;

import java.io.InputStream;
import java.security.MessageDigest;
import org.springframework.core.io.Resource;

public class ContentHashUtil {

    private static final int MAX_BYTES = 50 * 1024 * 1024; // 50 MB limit

    public static String calculateHash(Resource resource) {
        try (InputStream is = resource.getInputStream()) {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] buffer = new byte[8192];
            int bytesRead;
            long totalBytes = 0;
            while ((bytesRead = is.read(buffer)) != -1) {
                totalBytes += bytesRead;
                if (totalBytes > MAX_BYTES) {
                    throw new IllegalArgumentException("File exceeds maximum allowed size of " + MAX_BYTES + " bytes");
                }
                digest.update(buffer, 0, bytesRead);
            }
            byte[] hashBytes = digest.digest();
            StringBuilder sb = new StringBuilder();
            for (byte b : hashBytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException("Failed to calculate content hash", e);
        }
    }
}
