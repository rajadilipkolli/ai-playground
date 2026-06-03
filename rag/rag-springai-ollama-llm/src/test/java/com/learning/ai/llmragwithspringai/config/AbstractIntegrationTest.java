package com.learning.ai.llmragwithspringai.config;

import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

import com.learning.ai.llmragwithspringai.service.AIChatService;
import com.learning.ai.llmragwithspringai.service.DataIndexerService;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

@SpringBootTest(
        webEnvironment = RANDOM_PORT,
        properties = {"spring.ai.ollama.init.timeout=15m"},
        classes = {TestcontainersConfiguration.class})
public abstract class AbstractIntegrationTest {

    @Autowired
    protected ChatClient.Builder chatClientBuilder;

    @Autowired
    protected AIChatService aiChatService;

    @Autowired
    protected DataIndexerService dataIndexerService;

    @Autowired
    protected VectorStore vectorStore;

    @Autowired
    protected JdbcTemplate jdbcTemplate;
}
