[![Open in Gitpod](https://gitpod.io/button/open-in-gitpod.svg)](https://gitpod.io/#https://github.com/rajadilipkolli/ai-playground)

# ai-playground

AI implementations using java, stores and either of Langchain4j or springai framework

| **_Module Name_**                                                         | **_Description_**                                                                                    |
|---------------------------------------------------------------------------|------------------------------------------------------------------------------------------------------|
| [playground](./playground)                                                | AI playground using Langchain4j                                                                      |
| [chatmodel springai](./chatmodel-springai)                                | AI chat playground using springai                                                                    |
| [pgvector lanchain4j](./embeddingstores/pgvector-langchain4j)                             | Embeddings implementation using langchain4j and pgvector                                             |
| [pgvector springai](./embeddingstores/pgvector-springai)                                  | Embeddings implementation using springai and pgvector                                                |
| [neo4j embedding Store using spring ai](./embeddingstores/neo4j-springai) | Embedding store implementation using springai and neo4j                                              |
| [opensearch langchain4j](./ai-opensearch-langchain4j)                     | Embeddings implementation using langchain4j and opensearch store                                     |
| [rag langchain4j AllMiniLmL6V2](./rag/rag-langchain4j-AllMiniLmL6V2-llm)      | RAG Implementation using Langchain4j, PGVector store and allMiniLmL6V2 LLM                                  |
| [rag springai ollama llm](./rag/rag-springai-ollama-llm)                      | RAG Implementation using springai, Redis store, PDF document reader and ollama LLM with llama2 model |
| [rag springai openai llm](./rag/rag-springai-openai-llm)                      | RAG Implementation using springai, PGVector store, Tika document reader and openai LLM               |

### Credits
Thanks to langchain4j for providing an openAI compatible API for learning and demo purposes.