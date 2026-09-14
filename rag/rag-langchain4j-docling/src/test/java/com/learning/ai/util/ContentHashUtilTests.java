package com.learning.ai.util;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.InputStream;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.AbstractResource;

class ContentHashUtilTests {

    /** Verifies that oversized resources retain the size-limit exception. */
    @Test
    void preservesOversizedFileException() {
        AbstractResource oversized = new AbstractResource() {
            /** Describes the synthetic oversized resource. */
            @Override
            public String getDescription() {
                return "oversized test resource";
            }

            /** Opens the synthetic oversized input stream. */
            @Override
            public InputStream getInputStream() {
                return new InputStream() {
                    private int remaining = 50 * 1024 * 1024 + 1;

                    /** Reads one synthetic byte until the configured size is exhausted. */
                    @Override
                    public int read() {
                        return remaining-- > 0 ? 0 : -1;
                    }

                    /** Reads a bounded block of synthetic bytes. */
                    @Override
                    public int read(byte[] bytes, int offset, int length) {
                        if (remaining <= 0) {
                            return -1;
                        }
                        int count = Math.min(length, remaining);
                        remaining -= count;
                        return count;
                    }
                };
            }
        };

        assertThatThrownBy(() -> ContentHashUtil.calculateHash(oversized))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("File exceeds maximum allowed size");
    }
}
