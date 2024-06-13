package com.example.chatbot.service;

<<<<<<< chatbot-openai
=======
import static org.springframework.ai.chat.client.advisor.AbstractChatMemoryAdvisor.CHAT_MEMORY_CONVERSATION_ID_KEY;
import static org.springframework.ai.chat.client.advisor.AbstractChatMemoryAdvisor.CHAT_MEMORY_RETRIEVE_SIZE_KEY;

>>>>>>> main
import com.example.chatbot.model.request.AIChatRequest;
import com.example.chatbot.model.response.AIChatResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
<<<<<<< chatbot-openai
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.transformer.ChatServiceContext;
import org.springframework.ai.chat.service.ChatService;
import org.springframework.ai.chat.service.ChatServiceResponse;
=======
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatResponse;
>>>>>>> main
import org.springframework.stereotype.Service;

@Service
public class ChatbotService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ChatbotService.class);

<<<<<<< chatbot-openai
    private final ChatService chatService;

    public ChatbotService(ChatService chatService) {
        this.chatService = chatService;
    }

    public AIChatResponse chat(AIChatRequest request) {
        Prompt prompt = new Prompt(new UserMessage(request.query()));
        String conversationId = request.conversationId() == null ? "default" : request.conversationId();
        ChatServiceResponse chatServiceResponse = this.chatService.call(new ChatServiceContext(prompt, conversationId));
        LOGGER.info("Response :{}", chatServiceResponse.getChatResponse().getResult());
        return new AIChatResponse(
                chatServiceResponse.getChatResponse().getResult().getOutput().getContent(),
                chatServiceResponse.getPromptContext().getConversationId());
=======
    private final ChatClient chatClient;

    public ChatbotService(ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    public AIChatResponse chat(AIChatRequest request) {

        ChatResponse chatResponse = this.chatClient
                .prompt()
                .user(request.query())
                .advisors(a -> a.param(CHAT_MEMORY_CONVERSATION_ID_KEY, request.conversationId())
                        .param(CHAT_MEMORY_RETRIEVE_SIZE_KEY, 100))
                .call()
                .chatResponse();
        LOGGER.info("Response :{}", chatResponse.getResult());
        return new AIChatResponse(chatResponse.getResult().getOutput().getContent(), request.conversationId());
>>>>>>> main
    }
}
