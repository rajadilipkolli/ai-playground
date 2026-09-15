package com.learning.ai.domain.response;

import java.util.List;

public record AICustomerSupportResponseWrapper(
        AICustomerSupportResponse response, List<RetrievalDiagnostic> diagnostics) {}
