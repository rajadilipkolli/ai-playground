package com.learning.ai.llmragwithspringai.agent.api;

import java.util.List;

public interface Planner {
    List<PlanStep> plan(AgentGoal goal, String context);
}
