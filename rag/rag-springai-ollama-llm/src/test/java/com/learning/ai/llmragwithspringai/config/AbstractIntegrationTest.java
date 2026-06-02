package com.learning.ai.llmragwithspringai.config;

import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;

@SpringBootTest(
        webEnvironment = RANDOM_PORT,
        classes = {TestcontainersConfiguration.class})
@AutoConfigureMockMvc
public abstract class AbstractIntegrationTest {}
