# neo4j-springai

## Architecture

```mermaid
flowchart TD
    %% Define Styles / Legend
    classDef userReq fill:#cc6699,stroke:#555555,stroke-width:2px;
    classDef coordinator fill:#6688cc,stroke:#555555,stroke-width:2px;
    classDef searchEngine fill:#66aa66,stroke:#555555,stroke-width:2px;

    subgraph Startup Flow
        Init["Initializer"]:::coordinator
        AddStore[("VectorStore.add()<br/>(Neo4j)")]:::searchEngine
        
        Init --> AddStore
    end

    subgraph Query Flow
        Endpoint(["POST /api/ai/query"]):::userReq
        Service["Neo4jVectorStoreService"]:::coordinator
        SearchStore[("VectorStore.similaritySearch()<br/>(Neo4j Vector Index)")]:::searchEngine
        Results(["Response"]):::userReq
        
        Endpoint --> Service
        Service --> SearchStore
        SearchStore --> Results
    end
```

### Run tests

```shell
./mvnw clean verify
```

### Run locally

```shell
docker-compose -f docker/docker-compose.yml up -d
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```
### Using Testcontainers at Development Time
You can run `TestApplication.java` from your IDE directly.
You can also run the application using Maven as follows:

```shell
./mvnw spotless:apply spring-boot:test-run
```

### Useful Links
* Swagger UI: http://localhost:8080/swagger-ui.html
* Actuator Endpoint: http://localhost:8080/actuator
