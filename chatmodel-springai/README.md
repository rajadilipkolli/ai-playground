# chatmodel-springai

This module showcases various chat interaction modes using the Spring AI framework. It exposes multiple REST endpoints to demonstrate plain text generation, prompt templating, streaming responses, and simple in-memory Retrieval-Augmented Generation (RAG).

## Endpoints
- `/chat`: Basic text-in, text-out interaction.
- `/chat/stream`: Streams the LLM response back to the client using Server-Sent Events (SSE).
- `/rag`: A simple in-memory RAG implementation using a local vector store.
- `/output`: Demonstrates structured output parsing from the LLM.

## Architecture

```mermaid
flowchart TD
    %% Define Styles / Legend
    classDef userReq fill:#cc6699,stroke:#555555,stroke-width:2px;
    classDef coordinator fill:#6688cc,stroke:#555555,stroke-width:2px;
    classDef searchEngine fill:#66aa66,stroke:#555555,stroke-width:2px;
    classDef llm fill:#aa88cc,stroke:#555555,stroke-width:2px;

    Endpoints(["REST Endpoints<br/>(/chat, /chat/stream, /rag, /output)"]):::userReq
    Controller["ChatController"]:::coordinator
    Service["ChatService"]:::coordinator
    
    Client["ChatClient"]:::llm
    Embed["EmbeddingModel"]:::searchEngine
    Store[("SimpleVectorStore<br/>(In-Memory)")]:::searchEngine
    
    Response(["LLM Responses"]):::userReq

    Endpoints --> Controller
    Controller --> Service
    Service --> Embed
    Embed --> Store
    Store -.->|Context| Service
    Service --> Client
    Client --> Response
```
