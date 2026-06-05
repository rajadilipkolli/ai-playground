# pgvector-springai

This module demonstrates vector similarity search using Spring AI's robust abstraction layer over `pgvector`. Contrasting with the LangChain4j approach, this module leverages Spring Boot autoconfiguration and Spring AI's standardized `VectorStore` interface, making it easy to swap underlying vector databases with minimal code changes.

## Architecture

```mermaid
flowchart TD
    %% Define Styles / Legend
    classDef userReq fill:#cc6699,stroke:#555555,stroke-width:2px;
    classDef coordinator fill:#6688cc,stroke:#555555,stroke-width:2px;
    classDef searchEngine fill:#66aa66,stroke:#555555,stroke-width:2px;

    subgraph Startup Flow
        Init["Initializer"]:::coordinator
        AddStore[("VectorStore.add()")]:::searchEngine
        
        Init --> AddStore
    end

    subgraph Query Flow
        Endpoint(["Query Endpoint"]):::userReq
        SearchStore[("VectorStore.similaritySearch()<br/>(With FilterExpression)")]:::searchEngine
        Results(["Query Results"]):::userReq
        
        Endpoint --> SearchStore
        SearchStore --> Results
    end
```
