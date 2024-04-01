package com.learning.openai;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

public interface JokeAssistant {

    // by default if no variable name is given it takes {{it}}
    @UserMessage("Tell me a joke about {{name}}")
    String tellMeAJokeAbout(@V("name") String subject);

    @SystemMessage("you are an It consultant who just replies  \"It depends\" to every question")
    String ask(String question);

    @SystemMessage("you are a sarcastic and funny chat assistant ")
    String chat(String question);
}
