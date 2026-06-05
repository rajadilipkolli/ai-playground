# playground-langchain4j

This module serves as an exploratory playground for LangChain4j. It demonstrates how to utilize powerful concepts like `AiServices` to declaratively define AI behavior through Java interfaces, and how to maintain conversational context using chat memory.

## Architecture

```mermaid
flowchart TD
    %% Define Styles / Legend
    classDef userReq fill:#cc6699,stroke:#555555,stroke-width:2px;
    classDef coordinator fill:#6688cc,stroke:#555555,stroke-width:2px;
    classDef processing fill:#cc8855,stroke:#555555,stroke-width:2px;
    classDef llm fill:#aa88cc,stroke:#555555,stroke-width:2px;

    Entry(["Demo Entry Points"]):::userReq
    
    subgraph AiServices Interfaces
        ChatAssistant["ChatAssistant"]:::coordinator
        JokeAssistant["JokeAssistant"]:::coordinator
        SentimentAssistant["SentimentAssistant"]:::coordinator
    end

    Proxy["AiServices Proxy<br/><i>(Dynamic Implementation)</i>"]:::processing
    Memory["MessageWindowChatMemory<br/><i>(Optional Context)</i>"]:::processing
    Model["OpenAiChatModel"]:::llm
    Response(["Typed Responses"]):::userReq

    Entry --> ChatAssistant
    Entry --> JokeAssistant
    Entry --> SentimentAssistant

    ChatAssistant --> Proxy
    JokeAssistant --> Proxy
    SentimentAssistant --> Proxy

    Proxy <--> Memory
    Proxy --> Model
    Model --> Response
```
