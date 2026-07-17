package com.learning.ai.agent.memory;

import static org.assertj.core.api.Assertions.assertThat;

import com.learning.ai.agent.AbstractIntegrationTest;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.test.annotation.DirtiesContext;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class PersistentChatMemoryIT extends AbstractIntegrationTest {

    @Test
    @Order(1)
    @DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
    void step1_writeMemory() {
        String conversationId = "persist-session-1";
        String reply1 = agentService.chat(conversationId, "Remember the secret word: APPLE.");
        assertThat(reply1).isNotNull();
    }

    @Test
    @Order(2)
    void step2_readMemory() {
        String conversationId = "persist-session-1";
        String reply2 = agentService.chat(conversationId, "What was the secret word?");
        assertThat(reply2).containsIgnoringCase("APPLE");
    }
}
