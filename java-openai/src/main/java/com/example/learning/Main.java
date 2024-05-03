package com.example.learning;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;

import com.example.learning.model.request.ChatRequest;
import com.example.learning.model.response.ChatResponse;
import com.example.learning.model.request.Message;
import com.fasterxml.jackson.databind.ObjectMapper;

public class Main {

    // public static final String OPENAI_API_KEY = System.getenv("OPENAI_API_KEY");
    // public static final String CHAT_URL =
    // "https://api.openai.com/v1/chat/completions";
    public static final String OPENAI_API_KEY = "demo";
    public static final String CHAT_URL = "http://langchain4j.dev/demo/openai/v1/chat/completions";
    public final static String MODEL = "gpt-3.5-turbo";
    public final static double TEMPERATURE = 0.7;

    static HttpClient client = HttpClient.newHttpClient();
    static ObjectMapper mapper = new ObjectMapper();

    public static void main(String[] args) throws IOException, InterruptedException {

        ChatRequest chatRequest = new ChatRequest(
                MODEL, List.of(new Message("user", "List all the movies directed by SS Rajamowli")),
                TEMPERATURE);
        String requestPayload = mapper.writeValueAsString(chatRequest);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(CHAT_URL))
                .header("Authorization", "Bearer %s".formatted(OPENAI_API_KEY))
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestPayload))
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        String body = response.body();
        ChatResponse chatResponse = mapper.readValue(body, ChatResponse.class);
        System.out.println(chatResponse.choices().get(0).message().content());
    }
}