package com.learning.openai;

import dev.langchain4j.model.input.structured.StructuredPrompt;

@StructuredPrompt("Give summary of {{name}} as of {{current_date}} using the following information: \n\n {{info}}")
public record PersonSummaryPrompt(String name, String info) {}
