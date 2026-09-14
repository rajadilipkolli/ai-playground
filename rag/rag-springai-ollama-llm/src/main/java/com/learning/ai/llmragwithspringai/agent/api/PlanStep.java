package com.learning.ai.llmragwithspringai.agent.api;

import java.util.Map;

public record PlanStep(String type, String prompt, String toolName, Map<String, Object> args) {}
