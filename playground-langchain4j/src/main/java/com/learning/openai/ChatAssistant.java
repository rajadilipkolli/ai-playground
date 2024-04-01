package com.learning.openai;

import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

public interface ChatAssistant {
    String chat(String chatMessage);

    @UserMessage("Give me a summary of {{name}} in 3 bullet points using the following information : \n \n  {{info}}")
    String summarize(@V("name") String name, @V("info") String info);

    @UserMessage(
            """
            Give a person summary in the following format :
            Name : ...
            Data of Birth : ...
            Profession: ...
            Captaincy: ...

            Use the following information:
            {{info}}
            """)
    String summarizeInFormat(@V("info") String info);

    @UserMessage(
            """
            Summarize the following information in JSON format having name, date of birth, experience in years as of {{current_date}}, captaincy as keys :
            {{info}}
            """)
    String summarizeInJson(@V("info") String info);

    @UserMessage(
            """
            Give a person summary as of {{current_date}}

            Using the following information:

            {{info}}
            """)
    Person summarizeAsBean(@V("info") String info);

    Person summarize(PersonSummaryPrompt personSummaryPrompt);
}
