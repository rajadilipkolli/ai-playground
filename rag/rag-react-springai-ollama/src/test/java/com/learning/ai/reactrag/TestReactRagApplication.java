package com.learning.ai.reactrag;

import com.learning.ai.reactrag.common.ContainerConfig;
import org.springframework.boot.SpringApplication;

public class TestReactRagApplication {

    static void main(String[] args) {
        SpringApplication.from(ReactRagApplication::main)
                .with(ContainerConfig.class)
                .run(args);
    }
}
