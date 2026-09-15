package com.learning.ai.modelregression;

import com.learning.ai.modelregression.config.TestcontainersConfiguration;
import org.springframework.boot.SpringApplication;

public class TestModelRegressionDetectionApplication {

    public static void main(String[] args) {
        SpringApplication.from(ModelRegressionDetectionApplication::main)
                .with(TestcontainersConfiguration.class)
                .run(args);
    }
}
