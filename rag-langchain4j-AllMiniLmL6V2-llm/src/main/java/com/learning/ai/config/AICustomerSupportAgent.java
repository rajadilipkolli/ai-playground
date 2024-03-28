package com.learning.ai.config;

import com.learning.ai.domain.response.AICustomerSupportResponse;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

public interface AICustomerSupportAgent {

    @UserMessage({
        """
            Tell me about {{question}}? as of {{current_date}}

            Use the following information to answer the question:
            {{information}}
        """
    })
    AICustomerSupportResponse chat(@V("question") String question, @V("information") String information);
}
