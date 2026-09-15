# pgvector-langchain4j

This module demonstrates vector embedding and retrieval using LangChain4j and a PostgreSQL database extended with `pgvector`. A key highlight of this implementation is the use of a local ONNX embedding model (`AllMiniLmL6V2`) running directly in the JVM, and the ability to perform metadata filtering during searches.

## Architecture

```mermaid
flowchart TD
    %% Define Styles / Legend
    classDef userReq fill:#cc6699,stroke:#555555,stroke-width:2px;
    classDef coordinator fill:#6688cc,stroke:#555555,stroke-width:2px;
    classDef searchEngine fill:#66aa66,stroke:#555555,stroke-width:2px;
    classDef llm fill:#aa88cc,stroke:#555555,stroke-width:2px;

    subgraph Startup Flow
        Init["Initializer"]:::coordinator
        EmbedModelStartup["AllMiniLmL6V2 Model<br/>(Local ONNX)"]:::llm
        AddStore[("PgVectorEmbeddingStore.add()")]:::searchEngine
        
        Init --> EmbedModelStartup
        EmbedModelStartup --> AddStore
    end

    subgraph Query Flow
        Endpoint(["Query Endpoint"]):::userReq
        EmbedModelQuery["AllMiniLmL6V2 Model<br/>(Local ONNX)"]:::llm
        SearchStore[("PgVectorEmbeddingStore.search()<br/>(With Metadata Filtering)")]:::searchEngine
        Results(["Query Results"]):::userReq
        
        Endpoint --> EmbedModelQuery
        EmbedModelQuery --> SearchStore
        SearchStore --> Results
    end
```
