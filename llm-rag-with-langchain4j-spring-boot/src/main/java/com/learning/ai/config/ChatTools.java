package com.learning.ai.config;

import dev.langchain4j.agent.tool.Tool;
import java.time.LocalTime;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class ChatTools {

    /**
     * This tool is available to {@link AICustomerSupportAgent}
     */
    @Tool
    String currentTime() {
        log.info("Inside ChatTools");
        return LocalTime.now().toString();
    }
}
