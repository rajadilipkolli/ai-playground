[![Open in Gitpod](https://gitpod.io/button/open-in-gitpod.svg)](https://gitpod.io/#https://github.com/rajadilipkolli/ai-playground)

# ai-playground

AI implementations using java, stores and either of Langchain4j or springai framework

## AI Verbiage
- LLM - Large Language Model
- Embedding Store - database equivalent to store vector embeddings
- Embeddings - Document converted to vector
- Document - information that is required for AI to process

## Pre-requisite
- Java 17+
- Docker Engine

## Implementations

Below is the summary of implementations in this repository

| Category                            | **_Module_**                                                                                      | **_Description_**                                                                                                                                                                                                                                                                         |
|-------------------------------------|---------------------------------------------------------------------------------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| playground                          | [langchain4j playground](./playground-langchain4j)                                                | AI playground using Langchain4j                                                                                                                                                                                                                                                           |
|                                     | [spring ai playground](./chatmodel-springai)                                                      | AI playground using SpringAI & SpringBoot                                                                                                                                                                                                                                                 |
| embeddingstores                     | [neo4j store with springai](./embeddingstores/neo4j-springai)                                     | Embedding store implementation using springai, spring boot and neo4j as store                                                                                                                                                                                                             |
|                                     | [opensearch store with langchain4j](./embeddingstores/opensearch-langchain4j)                     | Embedding store implementation using langchain4j and opensearch as store                                                                                                                                                                                                                  |
|                                     | [pgvector store with lanchain4j](./embeddingstores/pgvector-langchain4j)                          | Embedding store implementation using langchain4j and pgvector as store                                                                                                                                                                                                                    |
|                                     | [pgvector store with springai](./embeddingstores/pgvector-springai)                               | Embedding store implementation using springai, spring boot and pgvector as store                                                                                                                                                                                                          |
| RAG(Retrieval-Augmented-Generation) | [rag implementation using langchain4j and AllMiniLmL6V2](./rag/rag-langchain4j-AllMiniLmL6V2-llm) | RAG Implementation using Langchain4j, PGVector store and allMiniLmL6V2 LLM with spring boot                                                                                                                                                                                               |
|                                     | [rag implementation using springai with llama llm](./rag/rag-springai-ollama-llm)                 | RAG Implementation using springai, Redis store, PDF document reader and ollama LLM with llama2 model. <br/> Ollama LLM is offline version of LLM which will be downloaded once and can be used locally, as a result it will be very slow in responding based on your system configuration |
|                                     | [rag implementation using springai with openai llm](./rag/rag-springai-openai-llm)                | RAG Implementation using springai, PGVector store, Tika document reader and openai LLM with spring boot                                                                                                                                                                                   |
| ChatBot                             | [chatbox using ollama](./chatbot/chatbot-ollama-springai)                                         | ChatBox using Ollama3 LLM and chromadb                                                                                                                                                                                                                                                    |


### Credits
Thanks to langchain4j for providing an openAI compatible API for learning and demo purposes.

### How to run in local

Note All examples contains testcontainers, so can run directly in dev mode.
