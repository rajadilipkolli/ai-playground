package com.learning.ai.llmragwithspringai.agent.api;

public interface Orchestrator {
    AgentResult run(AgentQuery query);
}
