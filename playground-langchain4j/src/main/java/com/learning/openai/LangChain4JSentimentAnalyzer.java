package com.learning.openai;

import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiChatModelName;
import dev.langchain4j.service.AiServices;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LangChain4JSentimentAnalyzer {

    private static final Logger LOGGER = LoggerFactory.getLogger(LangChain4JSentimentAnalyzer.class);

    public static void main(String[] args) {

               OpenAiChatModel openAiChatModel = OpenAiChatModel.builder().apiKey("demo")
                       .modelName(OpenAiChatModelName.GPT_4_O_MINI)
                       .logRequests(true)
                       .logResponses(true)
                       .build();

        MessageWindowChatMemory chatMemory = MessageWindowChatMemory.withMaxMessages(20);

        SentimentAssistant sentimentAssistant = AiServices.builder(SentimentAssistant.class)
                .chatLanguageModel(openAiChatModel)
                .chatMemory(chatMemory)
                .build();

        Sentiment analyze = sentimentAssistant.analyze("I love java programing language");
        LOGGER.info("Response : {}", analyze);

        analyze = sentimentAssistant.analyze("I hate Java");
        LOGGER.info("Response : {}", analyze);

        analyze = sentimentAssistant.analyze("I love and Hate Agni");
        LOGGER.info("Response : {}", analyze);
    }
}
