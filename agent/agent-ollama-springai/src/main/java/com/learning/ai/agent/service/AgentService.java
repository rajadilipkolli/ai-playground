package com.learning.ai.agent.service;

import io.micrometer.core.instrument.MeterRegistry;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.stereotype.Service;

@Service
public class AgentService {

    private final ChatClient chatClient;
    private final Object[] toolCallbacks;
    private final MeterRegistry meterRegistry;

    public AgentService(ChatClient chatClient, List<ToolCallback> toolCallbacks, MeterRegistry meterRegistry) {
        this.chatClient = chatClient;
        this.toolCallbacks = toolCallbacks.toArray();
        this.meterRegistry = meterRegistry;
    }

    public String chat(String conversationId, String message) {
        meterRegistry.counter("agent.calls").increment();
        long startTime = System.nanoTime();
        try {
            return chatClient
                    .prompt()
                    .user(message)
                    .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, conversationId))
                    .tools(toolCallbacks)
                    .call()
                    .content();
        } catch (org.springframework.ai.retry.TransientAiException e) {
            meterRegistry.counter("agent.errors", "type", "transient").increment();
            throw e;
        } catch (Exception e) {
            meterRegistry.counter("agent.errors", "type", "non_transient").increment();
            throw e;
        } finally {
            meterRegistry.timer("agent.latency").record(System.nanoTime() - startTime, TimeUnit.NANOSECONDS);
        }
    }
}
