package com.learning.openai;

import dev.langchain4j.model.chat.listener.ChatModelErrorContext;
import dev.langchain4j.model.chat.listener.ChatModelListener;
import dev.langchain4j.model.chat.listener.ChatModelRequestContext;
import dev.langchain4j.model.chat.listener.ChatModelResponseContext;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiChatModelName;

import static java.util.Collections.singletonList;

public class LangChain4JObservabilityDemo {

    public static void main(String[] args) {

        ChatModelListener modelListener = new ChatModelListener() {
            @Override
            public void onRequest(ChatModelRequestContext requestContext) {
                System.out.println("Request: " + requestContext.chatRequest().messages());
            }

            @Override
            public void onResponse(ChatModelResponseContext responseContext) {
                System.out.println("Response: " + responseContext.chatRequest().messages());
            }

            @Override
            public void onError(ChatModelErrorContext errorContext) {
                errorContext.error().printStackTrace();
            }
        };

        OpenAiChatModel model = OpenAiChatModel.builder()
                .apiKey("demo")
                .modelName(OpenAiChatModelName.GPT_4_O_MINI)
                .listeners(singletonList(modelListener))
                .build();

        model.chat("Tell me a joke about Java");
    }

}
