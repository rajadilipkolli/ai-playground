package com.learning.ai.config;

import dev.langchain4j.agent.tool.Tool;
import java.time.LocalTime;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
class ChatTools {

    /**
     * This tool is available to {@link AICustomerSupportAgent}
     */
    @Tool(name = "currentTime", value = "the current time is")
    String currentTime() {
        log.info("Inside ChatTools");
        return LocalTime.now().toString();
    }
}
