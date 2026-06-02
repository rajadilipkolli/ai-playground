package com.learning.ai.service;

import com.learning.ai.domain.response.AICustomerSupportResponse;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;
import dev.langchain4j.service.spring.AiService;

@AiService(tools = "currentTime")
public interface AICustomerSupportAgent {

    @UserMessage({"""
            Tell me about {{question}}?

            Use the following numbered information segments to synthesize your answer:
            {{information}}
        """})
    AICustomerSupportResponse chat(@V("question") String question, @V("information") String information);
}
