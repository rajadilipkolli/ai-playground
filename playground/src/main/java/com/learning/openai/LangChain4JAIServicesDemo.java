package com.learning.openai;

import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.input.PromptTemplate;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiChatModelName;
import dev.langchain4j.service.AiServices;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LangChain4JAIServicesDemo {

    private static final Logger LOGGER = LoggerFactory.getLogger(LangChain4JAIServicesDemo.class);

    public static void main(String[] args) {
        //        String openAIKey = System.getenv("OPEN_AI_KEY");
        //        OpenAiChatModel openAiChatModel = OpenAiChatModel.withApiKey("demo");
        OpenAiChatModel openAiChatModel = OpenAiChatModel.builder()
                .apiKey("demo")
                .modelName(OpenAiChatModelName.GPT_3_5_TURBO)
                .logRequests(false)
                .logResponses(false)
                .build();

        MessageWindowChatMemory chatMemory = MessageWindowChatMemory.withMaxMessages(20);

        ChatAssistant chatAssistant = AiServices.builder(ChatAssistant.class)
                .chatLanguageModel(openAiChatModel)
                .chatMemory(chatMemory)
                .build();

        PromptTemplate promptTemplate = PromptTemplate.from("What are all movies by rajamouli");
        String answer = chatAssistant.chat(promptTemplate.template());
        LOGGER.info("response :: {}", answer);

        JokeAssistant jokeAssitant = AiServices.builder(JokeAssistant.class)
                .chatLanguageModel(openAiChatModel)
                .chatMemory(chatMemory)
                .build();

        answer = jokeAssitant.ask("Should we use microservices ?");
        LOGGER.info("response : {}", answer);
        answer = jokeAssitant.chat("Should we use phyton ?");
        LOGGER.info("response : {}", answer);

        String personInfo =
                """
                Sid, born on 20 March 1991, is a software developer working in India.
                He has started his career as a Java developer on  20 June 2016 and worked with languages like Java, Angular, Html.

                He was certified in AWS Developer, OCJP professional
                """;
        String summarized = chatAssistant.summarizeInFormat(personInfo);
        LOGGER.info("response :: {}", summarized);
        summarized = chatAssistant.summarizeInJson(personInfo);
        LOGGER.info("response :: {}", summarized);
        Person person = chatAssistant.summarizeAsBean(personInfo);
        LOGGER.info("response :: {}", person);

        person = chatAssistant.summarize(new PersonSummaryPrompt("sid", personInfo));
        LOGGER.info("response :: {}", person);
    }
}
