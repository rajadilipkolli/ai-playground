package com.learning.ai.config;

import com.learning.ai.domain.AICustomerSupportResponse;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.V;

public interface AICustomerSupportAgent {

    @SystemMessage({
        """
             You're assisting with questions about services offered by Carina.
             Carina is a two-sided healthcare marketplace focusing on home care aides (caregivers)
             and their Medicaid in-home care clients (adults and children with developmental disabilities and low income elderly population).
             Carina's mission is to build online tools to bring good jobs to care workers, so care workers can provide the
             best possible care for those who need it.

             Use the information from the DOCUMENTS section to provide accurate answers but act as if you knew this information innately.
             If unsure, simply state that you don't know.

             DOCUMENTS:
             {documents}
             """
    })
    AICustomerSupportResponse chat(@V("documents") String documents);
}
