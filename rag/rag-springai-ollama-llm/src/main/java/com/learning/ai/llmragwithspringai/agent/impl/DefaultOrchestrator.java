package com.learning.ai.llmragwithspringai.agent.impl;

import com.learning.ai.llmragwithspringai.agent.api.AgentGoal;
import com.learning.ai.llmragwithspringai.agent.api.AgentQuery;
import com.learning.ai.llmragwithspringai.agent.api.AgentResult;
import com.learning.ai.llmragwithspringai.agent.api.MemoryEntry;
import com.learning.ai.llmragwithspringai.agent.api.MemoryStore;
import com.learning.ai.llmragwithspringai.agent.api.Orchestrator;
import com.learning.ai.llmragwithspringai.agent.api.PlanStep;
import com.learning.ai.llmragwithspringai.agent.api.Planner;
import com.learning.ai.llmragwithspringai.agent.api.ToolRegistry;
import com.learning.ai.llmragwithspringai.agent.api.ToolResult;
import com.learning.ai.llmragwithspringai.config.properties.AgentProperties;
import com.learning.ai.llmragwithspringai.model.response.RetrievalDiagnostic;
import com.learning.ai.llmragwithspringai.rag.retrieval.FilterContext;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.rag.retrieval.search.DocumentRetriever;

public class DefaultOrchestrator implements Orchestrator {

    private static final Logger log = LoggerFactory.getLogger(DefaultOrchestrator.class);

    private final Planner planner;
    private final ToolRegistry toolRegistry;
    private final MemoryStore memoryStore;
    private final RetrievalAction retrievalAction;
    private final AgentProperties.Orchestrator orchestratorProps;

    public DefaultOrchestrator(
            Planner planner,
            ToolRegistry toolRegistry,
            MemoryStore memoryStore,
            DocumentRetriever documentRetriever,
            AgentProperties properties) {
        this.planner = planner;
        this.toolRegistry = toolRegistry;
        this.memoryStore = memoryStore;
        this.retrievalAction =
                new RetrievalAction(documentRetriever, properties.getRetrieval().getTopK());
        this.orchestratorProps = properties.getOrchestrator();
    }

    @Override
    public AgentResult run(AgentQuery query) {
        try {
            FilterContext.clearRetrievedDocuments();
            return ScopedValue.where(FilterContext.FILTER_EXPRESSION, null).call(() -> {
                String sessionId =
                        query.sessionId() != null && !query.sessionId().isBlank()
                                ? query.sessionId()
                                : UUID.randomUUID().toString();

                long startTime = System.currentTimeMillis();
                long timeoutMs = orchestratorProps.getStepTimeoutSeconds() * 1000;

                List<RetrievalDiagnostic> allProvenance = new ArrayList<>();
                memoryStore.add(sessionId, new MemoryEntry("user", query.text()));

                int stepCount = 0;
                int maxToolCalls = orchestratorProps.getMaxToolCallsPerStep();

                while (true) {
                    long remainingMs = timeoutMs - (System.currentTimeMillis() - startTime);
                    if (remainingMs <= 0) {
                        log.warn("Orchestrator timed out for session {}", sessionId);
                        return new AgentResult("Execution timed out.", allProvenance);
                    }

                    String context = buildContext(sessionId);
                    List<PlanStep> plan;
                    try {
                        plan = CompletableFuture.supplyAsync(() -> planner.plan(new AgentGoal(query.text()), context))
                                .get(remainingMs, TimeUnit.MILLISECONDS);
                    } catch (TimeoutException e) {
                        log.warn("Orchestrator timed out for session {}", sessionId);
                        return new AgentResult("Execution timed out.", allProvenance);
                    } catch (Exception e) {
                        log.error("Planning failed", e);
                        return new AgentResult(
                                "I encountered an error while planning: " + e.getMessage(), allProvenance);
                    }

                    if (plan == null || plan.isEmpty()) {
                        return new AgentResult("I don't know how to proceed.", allProvenance);
                    }

                    boolean finished = false;
                    String finalAnswer = "I'm sorry, I was unable to complete the task.";
                    int toolCallsThisStep = 0;

                    for (PlanStep step : plan) {
                        log.info("Executing step: {}", step);
                        if ("finish".equalsIgnoreCase(step.type())) {
                            finalAnswer = step.prompt();
                            finished = true;
                            memoryStore.add(sessionId, new MemoryEntry("assistant", finalAnswer));
                            break;
                        } else if ("retrieval".equalsIgnoreCase(step.type())) {
                            remainingMs = timeoutMs - (System.currentTimeMillis() - startTime);
                            if (remainingMs <= 0) {
                                log.warn("Orchestrator timed out for session {}", sessionId);
                                return new AgentResult("Execution timed out.", allProvenance);
                            }
                            try {
                                List<Document> docs = CompletableFuture.supplyAsync(
                                                () -> retrievalAction.retrieve(step.prompt()))
                                        .get(remainingMs, TimeUnit.MILLISECONDS);
                                FilterContext.setRetrievedDocuments(docs); // update global context if needed
                                allProvenance.addAll(RetrievalAction.mapToDiagnostics(docs));
                                String content =
                                        docs.stream().map(Document::getText).collect(Collectors.joining("\n"));
                                memoryStore.add(sessionId, new MemoryEntry("system", "Retrieval Result: " + content));
                            } catch (TimeoutException e) {
                                log.warn("Orchestrator timed out for session {}", sessionId);
                                return new AgentResult("Execution timed out.", allProvenance);
                            } catch (Exception e) {
                                memoryStore.add(
                                        sessionId, new MemoryEntry("system", "Retrieval Error: " + e.getMessage()));
                            }
                        } else if ("tool".equalsIgnoreCase(step.type())) {
                            if (toolCallsThisStep >= maxToolCalls) {
                                log.warn("Max tool calls reached for this step.");
                                memoryStore.add(
                                        sessionId,
                                        new MemoryEntry("system", "Tool execution failed: Max tool calls reached."));
                                continue;
                            }
                            toolCallsThisStep++;
                            String toolName = step.toolName();
                            if (toolRegistry.hasTool(toolName)) {
                                remainingMs = timeoutMs - (System.currentTimeMillis() - startTime);
                                if (remainingMs <= 0) {
                                    log.warn("Orchestrator timed out for session {}", sessionId);
                                    return new AgentResult("Execution timed out.", allProvenance);
                                }
                                try {
                                    Optional<ToolResult> result = CompletableFuture.supplyAsync(
                                                    () -> toolRegistry.execute(toolName, step.args()))
                                            .get(remainingMs, TimeUnit.MILLISECONDS);
                                    if (result.isPresent()) {
                                        memoryStore.add(
                                                sessionId,
                                                new MemoryEntry(
                                                        "system",
                                                        "Tool Result (" + toolName + "): "
                                                                + result.get().result()));
                                    } else {
                                        memoryStore.add(
                                                sessionId,
                                                new MemoryEntry(
                                                        "system",
                                                        "Tool Result (" + toolName + "): Execution returned empty."));
                                    }
                                } catch (TimeoutException e) {
                                    log.warn("Orchestrator timed out for session {}", sessionId);
                                    return new AgentResult("Execution timed out.", allProvenance);
                                } catch (Exception e) {
                                    memoryStore.add(
                                            sessionId,
                                            new MemoryEntry(
                                                    "system", "Tool Error (" + toolName + "): " + e.getMessage()));
                                }
                            } else {
                                memoryStore.add(
                                        sessionId,
                                        new MemoryEntry("system", "Tool Error: Tool " + toolName + " not found."));
                            }
                        } else if ("reason".equalsIgnoreCase(step.type())) {
                            memoryStore.add(sessionId, new MemoryEntry("assistant", "Thinking: " + step.prompt()));
                        }
                    }

                    if (finished) {
                        return new AgentResult(finalAnswer, allProvenance);
                    }

                    stepCount++;
                    if (stepCount >= orchestratorProps.getMaxPlanningCycles()) {
                        log.warn("Max orchestrator loops reached.");
                        return new AgentResult(
                                "I reached the maximum number of steps without finishing.", allProvenance);
                    }
                }
            });
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            FilterContext.clearRetrievedDocuments();
        }
    }

    private String buildContext(String sessionId) {
        List<MemoryEntry> entries = memoryStore.get(sessionId);
        return entries.stream().map(e -> e.role() + ": " + e.content()).collect(Collectors.joining("\n"));
    }
}
