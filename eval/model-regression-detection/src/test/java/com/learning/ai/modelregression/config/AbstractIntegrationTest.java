package com.learning.ai.modelregression.config;

import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import tools.jackson.databind.json.JsonMapper;

@SpringBootTest(
        webEnvironment = RANDOM_PORT,
        properties = {"spring.ai.ollama.init.timeout=15m", "eval.pipeline.enabled=false"},
        classes = {TestcontainersConfiguration.class})
public abstract class AbstractIntegrationTest {

    @Autowired
    protected ChatClient.Builder chatClientBuilder;

    @Autowired
    protected JsonMapper jsonMapper;

    @Autowired
    protected JdbcTemplate jdbcTemplate;
}
