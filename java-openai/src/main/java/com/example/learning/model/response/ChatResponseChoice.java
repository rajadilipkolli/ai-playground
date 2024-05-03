package com.example.learning.model.response;

import com.example.learning.model.request.Message;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ChatResponseChoice(
        int index,
        Message message,
        @JsonProperty("finish_reason") String finishReason) {
}
