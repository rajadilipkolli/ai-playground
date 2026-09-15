package com.ai.opensearch;

import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(excludeName = {
        "org.opensearch.spring.boot.autoconfigure.OpenSearchRestClientAutoConfiguration",
        "org.opensearch.spring.boot.autoconfigure.OpenSearchClientAutoConfiguration",
        "org.opensearch.spring.boot.autoconfigure.OpenSearchRestHighLevelClientAutoConfiguration",
        "org.springframework.boot.autoconfigure.opensearch.OpenSearchClientAutoConfiguration",
        "org.springframework.boot.autoconfigure.data.opensearch.OpenSearchDataAutoConfiguration",
        "org.springframework.boot.autoconfigure.data.opensearch.OpenSearchRepositoriesAutoConfiguration"
})
public class TestApplication {
}
