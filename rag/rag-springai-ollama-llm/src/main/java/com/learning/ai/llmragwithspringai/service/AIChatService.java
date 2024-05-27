package com.learning.ai.llmragwithspringai.service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.SystemPromptTemplate;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

@Service
public class AIChatService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AIChatService.class);

    private static final String template =
            """
            You are a helpful assistant, conversing with a user about the subjects contained in a set of documents.
            Use the information from the DOCUMENTS section to provide accurate answers. If unsure or if the answer
            isn't found in the DOCUMENTS section, simply state that you don't know the answer.

            DOCUMENTS:
            {documents}

            """;

    private final ChatClient aiClient;
    private final VectorStore vectorStore;

    public AIChatService(ChatClient.Builder builder, VectorStore vectorStore) {
        this.aiClient = builder.build();
        this.vectorStore = vectorStore;
    }

    public String chat(String query) {
        // Querying the VectorStore using natural language looking for the information about info asked.
        LOGGER.debug("Querying vector store with query :{}", query);
        List<Document> listOfSimilarDocuments = this.vectorStore.similaritySearch(query);
        String documents = listOfSimilarDocuments.stream()
                .map(Document::getContent)
                .collect(Collectors.joining(System.lineSeparator()));
        LOGGER.info("Response from vector store :{}", documents);
        // Constructing the systemMessage to indicate the AI model to use the passed information
        // to answer the question.
        Message systemMessage = new SystemPromptTemplate(template).createMessage(Map.of("documents", documents));
        UserMessage userMessage = new UserMessage(query);
        Prompt prompt = new Prompt(List.of(systemMessage, userMessage));
        LOGGER.info("Calling ai with prompt :{}", prompt);
        ChatResponse aiResponse = aiClient.prompt(prompt).call().chatResponse();
        LOGGER.info("Response received from call :{}", aiResponse);
        Generation generation = aiResponse.getResult();
        return (generation != null) ? generation.getOutput().getContent() : "";
    }
}
