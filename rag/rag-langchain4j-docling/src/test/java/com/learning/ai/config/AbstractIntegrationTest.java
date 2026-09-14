package com.learning.ai.config;

import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

import com.learning.ai.RagLangchain4jDoclingApplication;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

@SpringBootTest(
        webEnvironment = RANDOM_PORT,
        properties = {
            "spring.security.user.name=admin",
            "spring.security.user.password=admin123"
        },
        classes = {RagLangchain4jDoclingApplication.class, TestcontainersConfiguration.class})
public abstract class AbstractIntegrationTest {

    @Autowired
    protected JdbcTemplate jdbcTemplate;
}
