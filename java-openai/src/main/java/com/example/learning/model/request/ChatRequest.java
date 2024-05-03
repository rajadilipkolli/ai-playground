package com.example.learning.model.request;

import java.util.List;

public record ChatRequest(String model, List<Message> messages, double temperature) {
}
