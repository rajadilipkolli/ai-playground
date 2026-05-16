package com.learning.ai.config;

import dev.langchain4j.agent.tool.Tool;
import java.time.LocalTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
class ChatTools {

    private static final Logger log = LoggerFactory.getLogger(ChatTools.class);

    /**
     * This tool is available to {@link AICustomerSupportAgent}
     */
    @Tool(name = "currentTime", value = "the current time is")
    String currentTime() {
        log.info("Inside ChatTools");
        return LocalTime.now().toString();
    }
}
