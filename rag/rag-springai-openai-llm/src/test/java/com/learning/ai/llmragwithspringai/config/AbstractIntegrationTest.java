package com.learning.ai.llmragwithspringai.config;

import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(
        webEnvironment = RANDOM_PORT,
        classes = {ContainersConfig.class})
public abstract class AbstractIntegrationTest {}
