package com.learning.openai;

import dev.langchain4j.chain.ConversationalChain;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.input.Prompt;
import dev.langchain4j.model.input.PromptTemplate;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiChatModelName;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LangChain4JOpenAIDemo {

    private static final Logger LOGGER = LoggerFactory.getLogger(LangChain4JOpenAIDemo.class);

    public static void main(String[] args) {
        // String openAIKey = System.getenv("OPEN_AI_KEY");
        // OpenAiChatModel openAiChatModel = OpenAiChatModel.withApiKey("demo");
        OpenAiChatModel openAiChatModel = OpenAiChatModel.builder()
                .apiKey("demo")
                .modelName(OpenAiChatModelName.GPT_4_O_MINI)
                .logRequests(false)
                .logResponses(false)
                .build();

        /////////        Stage 1

        String response = openAiChatModel.chat("List all the IPL Teams");
        LOGGER.info("response :: {}", response);
        response = openAiChatModel.chat("how old are they");
        LOGGER.info("response :: {}", response);

        // Two type of memory we have they are MessageWindowChatMemory and TokenWindowChatMemory
        MessageWindowChatMemory chatMemory = MessageWindowChatMemory.withMaxMessages(20);
        ConversationalChain conversationalChain = ConversationalChain.builder()
                .chatModel(openAiChatModel)
                .chatMemory(chatMemory)
                .build();

        response = conversationalChain.execute("List all the IPL Teams");
        LOGGER.info("all languages :: {}", response);
        response = conversationalChain.execute("which is the oldest IPL Team ?");
        LOGGER.info("oldest language :: {}", response);

        response = conversationalChain.execute("which is the oldest IPL Team from above");
        LOGGER.info("oldest language with context :: {}", response);

        PromptTemplate promptTemplate = PromptTemplate.from("What are all teams played by Rohit Sharma?");
        response = conversationalChain.execute(promptTemplate.template());
        LOGGER.info("response :: {}", response);
        promptTemplate = PromptTemplate.from("How old is he ?");
        response = conversationalChain.execute(promptTemplate.template());
        LOGGER.info("response :: {}", response);

        Prompt prompt =
                PromptTemplate.from("How old is he as of {{current_date}}").apply(Map.of());
        response = conversationalChain.execute(prompt.text());
        LOGGER.info("response :: {}", response);

        prompt = PromptTemplate.from("How old is {{name}} as of {{current_date}} ??")
                .apply(Map.of("name", "Rohit"));
        response = conversationalChain.execute(prompt.text());
        LOGGER.info("response :: {}", response);

        chatMemory.add(UserMessage.userMessage("What are all teams played by Rohit Sharma?"));
        AiMessage generatedResponse =
                openAiChatModel.chat(chatMemory.messages()).aiMessage();
        LOGGER.info("response :: {}", generatedResponse);
        chatMemory.add(generatedResponse);
    }
}
