package com.learning.ai.util;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.InputStream;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.AbstractResource;

class ContentHashUtilTests {

    @Test
    void preservesOversizedFileException() {
        AbstractResource oversized = new AbstractResource() {
            @Override
            public String getDescription() {
                return "oversized test resource";
            }

            @Override
            public InputStream getInputStream() {
                return new InputStream() {
                    private int remaining = 50 * 1024 * 1024 + 1;

                    @Override
                    public int read() {
                        return remaining-- > 0 ? 0 : -1;
                    }

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
