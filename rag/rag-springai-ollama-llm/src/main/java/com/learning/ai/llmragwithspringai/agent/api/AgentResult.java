package com.learning.ai.llmragwithspringai.agent.api;

import com.learning.ai.llmragwithspringai.model.response.RetrievalDiagnostic;
import java.util.List;

public record AgentResult(String answer, List<RetrievalDiagnostic> provenance) {}
