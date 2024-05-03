package com.example.ai.model.response;

import reactor.core.publisher.Flux;

public record AIStreamChatResponse(Flux<String> chatResponse) {}
