package com.example.chatbot.config;

import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.ChatMemoryChatServiceListener;
import org.springframework.ai.chat.memory.ChatMemoryRetriever;
import org.springframework.ai.chat.memory.InMemoryChatMemory;
import org.springframework.ai.chat.memory.LastMaxTokenSizeContentTransformer;
import org.springframework.ai.chat.memory.SystemPromptChatMemoryAugmentor;
import org.springframework.ai.chat.memory.VectorStoreChatMemoryChatServiceListener;
import org.springframework.ai.chat.memory.VectorStoreChatMemoryRetriever;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.transformer.QuestionContextAugmentor;
import org.springframework.ai.chat.prompt.transformer.TransformerContentType;
import org.springframework.ai.chat.prompt.transformer.VectorStoreRetriever;
import org.springframework.ai.chat.service.ChatService;
import org.springframework.ai.chat.service.PromptTransformingChatService;
import org.springframework.ai.tokenizer.JTokkitTokenCountEstimator;
import org.springframework.ai.tokenizer.TokenCountEstimator;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class ChatConfig {

    @Bean
    ChatMemory chatHistory() {
        return new InMemoryChatMemory();
    }

    @Bean
    TokenCountEstimator tokenCountEstimator() {
        return new JTokkitTokenCountEstimator();
    }

    @Bean
    ChatService chatService(
            ChatModel chatModel,
            ChatMemory chatMemory,
            TokenCountEstimator tokenCountEstimator,
            VectorStore vectorStore) {
        return PromptTransformingChatService.builder(chatModel)
                .withRetrievers(List.of(
                        new VectorStoreRetriever(vectorStore, SearchRequest.defaults()),
                        ChatMemoryRetriever.builder()
                                .withChatHistory(chatMemory)
                                .withMetadata(Map.of(TransformerContentType.SHORT_TERM_MEMORY, ""))
                                .build(),
                        new VectorStoreChatMemoryRetriever(
                                vectorStore, 10, Map.of(TransformerContentType.LONG_TERM_MEMORY, ""))))
                .withContentPostProcessors(List.of(
                        new LastMaxTokenSizeContentTransformer(
                                tokenCountEstimator, 1000, Set.of(TransformerContentType.SHORT_TERM_MEMORY)),
                        new LastMaxTokenSizeContentTransformer(
                                tokenCountEstimator, 1000, Set.of(TransformerContentType.LONG_TERM_MEMORY)),
                        new LastMaxTokenSizeContentTransformer(
                                tokenCountEstimator, 2000, Set.of(TransformerContentType.EXTERNAL_KNOWLEDGE))))
                .withAugmentors(List.of(
                        new QuestionContextAugmentor(),
                        new SystemPromptChatMemoryAugmentor(
                                """
                                Use the long term conversation history from the LONG TERM HISTORY section to provide accurate answers.

                                LONG TERM HISTORY:
                                {history}
                                    """,
                                Set.of(TransformerContentType.LONG_TERM_MEMORY)),
                        new SystemPromptChatMemoryAugmentor(Set.of(TransformerContentType.SHORT_TERM_MEMORY))))
                .withChatServiceListeners(List.of(
                        new ChatMemoryChatServiceListener(chatMemory),
                        new VectorStoreChatMemoryChatServiceListener(
                                vectorStore, Map.of(TransformerContentType.LONG_TERM_MEMORY, ""))))
                .build();
    }
}
