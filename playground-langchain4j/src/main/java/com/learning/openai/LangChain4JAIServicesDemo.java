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

        PromptTemplate promptTemplate = PromptTemplate.from("What are all IPL teams played by Ravichandran Ashwin?");
        String answer = chatAssistant.chat(promptTemplate.template());
        LOGGER.info("response :: {}", answer);

        JokeAssistant jokeAssistant = AiServices.builder(JokeAssistant.class)
                .chatLanguageModel(openAiChatModel)
                .chatMemory(chatMemory)
                .build();

        answer = jokeAssistant.ask("Should we use play cricket ?");
        LOGGER.info("response : {}", answer);
        answer = jokeAssistant.chat("Should we use red ball or white ball ?");
        LOGGER.info("response : {}", answer);

        String personInfo =
                """
                Dhoni, born on July 07, 1981, Ranchi, Bihar (now Jharkhand), is a former Indian Cricketer.
                He has started his career as a wicket keeper for india on December 23, 2004 and played for INDIA, CSK, Asia XI, Raising Pune Super Gaints.
                He has retired from all formats on 15th Aug 2020 while retired from Tests on 30 Dec 2015

                He has captained india in Tests, ODI and T20
                """;
        //        String summarized = chatAssistant.summarize("Dhoni", personInfo);
        //        LOGGER.info("response :: {}", summarized);
        //        summarized = chatAssistant.summarizeInFormat(personInfo);
        //        LOGGER.info("response :: {}", summarized);
        String summarized = chatAssistant.summarizeInJson(personInfo);
        LOGGER.info("response :: {}", summarized);
        Person person = chatAssistant.summarizeAsBean(personInfo);
        LOGGER.info("response :: {}", person);

        person = chatAssistant.summarize(new PersonSummaryPrompt("Dhoni", personInfo));
        LOGGER.info("response :: {}", person);
    }
}
