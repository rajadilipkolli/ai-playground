package com.learning.ai.agent;

import com.learning.ai.agent.config.TestcontainersConfiguration;
import com.learning.ai.agent.service.AgentService;
import io.restassured.RestAssured;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {"spring.ai.ollama.init.timeout=10m"},
        classes = {AgentApplication.class, TestcontainersConfiguration.class})
public abstract class AbstractIntegrationTest {

    @Autowired
    protected AgentService agentService;

    @Autowired
    protected ChatMemory chatMemory;

    @LocalServerPort
    private int port;

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
    }
}
