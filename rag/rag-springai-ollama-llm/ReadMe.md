# 🤖 RAG Spring AI Ollama LLM

Welcome to the **RAG (Retrieval-Augmented Generation)** application! This project uses **Spring AI**, **Ollama** (for local LLMs), and **PostgreSQL (pgvector)** to intelligently read your documents and answer questions based on them.

---

## 📑 Table of Contents
1. [Overview](#-overview)
2. [Architecture Flow](#-architecture-flow)
3. [Key Features](#-key-features)
4. [Getting Started](#-getting-started)
5. [API Reference](#-api-reference)
6. [Configuration Reference](#-configuration-reference)
7. [Guardrails](#-guardrails)
8. [Alternative Approaches](#-alternative-approaches)
9. [Performance Tuning](#-performance-tuning)

---

## 🌟 Overview

When you ask a generic AI a specific question about your private data, it often doesn't know the answer. This project solves that by **RAG**. 
1. **Ingestion**: You upload your private documents. The system chops them into bite-sized pieces and saves them in a database.
2. **Retrieval**: When you ask a question, the system searches the database for the most relevant pieces of information.
3. **Generation**: It hands those pieces to the AI (Ollama) and says, "Answer the user's question using *only* this information."

---

## 🏗️ Architecture Flow

Here is how data flows through the system, from uploading a document to receiving an AI-generated answer.

```mermaid
flowchart TD
    %% Define Styles
    classDef userReq fill:#cc6699,stroke:#555555,stroke-width:2px,color:#fff;
    classDef coordinator fill:#6688cc,stroke:#555555,stroke-width:2px,color:#fff;
    classDef searchEngine fill:#66aa66,stroke:#555555,stroke-width:2px,color:#fff;
    classDef fusion fill:#cc8855,stroke:#555555,stroke-width:2px,color:#fff;
    classDef llm fill:#aa88cc,stroke:#555555,stroke-width:2px,color:#fff;
    classDef cache fill:#eecc55,stroke:#555555,stroke-width:2px,color:#000;
    classDef ingestion fill:#44aaaa,stroke:#555555,stroke-width:2px,color:#fff;

    %% Nodes for Ingestion
    IngestApi(["Ingestion API<br/><i>Upload with Metadata</i>"]):::userReq
    DataIndexer["DataIndexerService<br/><i>Enriches Metadata</i>"]:::ingestion
    Splitter{"SectionTextSplitter<br/><i>Chunking Strategy</i>"}:::ingestion
    DB[("PgVector Database<br/><i>HNSW Index & tsvector</i>")]:::searchEngine

    %% Nodes for Retrieval
    User(["User Query + Metadata Filter"]):::userReq
    Analyzer["QueryAnalyzer<br/><i>Extracts Filters & Cleans Query</i>"]:::coordinator
    Advisor["RetrievalAugmentationAdvisor<br/><i>Coordinates the RAG process</i>"]:::coordinator
    MultiQuery["MultiQueryExpander<br/><i>Generates Query Variations</i>"]:::coordinator
    Cache["CachingDocumentRetriever<br/><i>Caffeine Cache Layer</i>"]:::cache
    HybridRetriever["HybridDocumentRetriever<br/><i>Runs searches in parallel</i>"]:::coordinator
    
    KeywordSearch[("KeywordDocumentRetriever<br/><i>Keyword Search</i>")]:::searchEngine
    VectorSearch[("VectorStoreDocumentRetriever<br/><i>Vector Similarity</i>")]:::searchEngine
    
    Joiner["RRFDocumentJoiner<br/><i>Reciprocal Rank Fusion</i>"]:::fusion
    Reranker["RelevanceDocumentReranker<br/><i>Keyword Overlap Reranker</i>"]:::fusion
    
    Ollama["ChatClient<br/><i>Large Language Model (LLM)</i>"]:::llm
    ToolCurrentDate(["currentDateTimeTool<br/><i>Date & Time</i>"]):::coordinator
    ToolCalculator(["calculatorTool<br/><i>Math Expressions</i>"]):::coordinator
    ToolKnowledge(["knowledgeSearchTool<br/><i>Retrieval Search</i>"]):::coordinator
    Response(["Generated Answer & Diagnostics"]):::userReq

    %% Ingestion Flow
    IngestApi -->|1. Upload File| DataIndexer
    DataIndexer -->|2. Hash & Enrich| Splitter
    Splitter -->|3. Store Chunked Data| DB
    
    %% Retrieval Flow
    User -->|4a. Asks question| Analyzer
    Analyzer -->|4b. Cleans Query & Adds Filters| Advisor
    Advisor -->|5. Transforms Query| MultiQuery
    MultiQuery -->|6. Requests context| Cache
    
    Cache -- 7a. Cache Hit --> Advisor
    Cache -->|7b. Cache Miss| Reranker
    
    Reranker -->|8. Calls Retriever| HybridRetriever
    
    HybridRetriever -- 9a. Check ScopedValue Cache --> HybridRetriever
    HybridRetriever -->|9b. Keyword Search on Cache Miss| KeywordSearch
    HybridRetriever -->|9c. Vector Search on Cache Miss| VectorSearch
    
    KeywordSearch -.->|10a. Matched docs| Joiner
    VectorSearch -.->|10b. Similar docs| Joiner
    
    Joiner -->|11. Combines results| HybridRetriever
    
    HybridRetriever -.->|12a. Stores in ScopedValue| HybridRetriever
    HybridRetriever -->|12b. Returns joined docs| Reranker
    Reranker -->|13. Scores and reranks| Cache
    
    Cache -.->|14. Stores in cache| Cache
    Cache -->|15. Returns context| Advisor
    
    Advisor -->|15. Sends query + context| Ollama
    
    %% Tool Calling
    Ollama <-->|16a. LLM invokes tools iteratively| ToolCurrentDate
    Ollama <-->|16b. LLM invokes tools iteratively| ToolCalculator
    Ollama <-->|16c. LLM invokes tools iteratively| ToolKnowledge
    ToolKnowledge -->|16d. Searches context| HybridRetriever
    
    Ollama -->|17. Generates final answer| Response
```

---

## ✨ Key Features

1. **Hybrid Search**: ON. Combines two types of search: Vector Search (meaning) and Keyword Search (exact words).
2. **Multi-Stage Retrieval (Reranking)**: ON. The system grabs a bunch of relevant documents, then double-checks and re-scores them via Keyword-overlap reranking.
3. **Caching Layer**: ON. Retrieves cached context for identical queries, skipping heavy database searches.
4. **HNSW Indexing**: ON. Uses a highly optimized indexing algorithm in PostgreSQL.
5. **Section-Aware Chunking**: Available but token-based chunking is the default. Intelligently splits your documents.
6. **Metadata Filtering**: ON. You can tag uploaded documents and search strictly within those categories.
7. **Self-Querying**: ON. Automatically analyzes user questions to extract metadata filters (like year, category, document type) before searching.
8. **Agentic Tool Calling (ReAct)**: ON. The LLM acts as an agent, autonomously deciding when to fetch the current date, perform math calculations, or trigger additional targeted searches.
9. **Guardrails**: ON. Intercepts queries on restricted topics (politics, violence, etc.) and halts the request gracefully.

---

## 🤖 ReAct / Agentic Capabilities

This application supports the ReAct (Reasoning and Acting) pattern. Instead of a traditional single-pass RAG (where context is retrieved once and appended to the prompt), the ChatClient acts as an intelligent agent. 

The LLM continuously evaluates the user's question, **reasons** about what information it needs, **acts** by invoking registered tools, observes the results, and synthesizes a final answer. This iterative loop complements automatic RAG context injection by granting the LLM agency over explicit searches, date retrieval, and mathematical calculations.

### Available Tools

When interacting with the `ChatClient`, the LLM has access to the following specialized tools:

- **`currentDateTimeTool`**: Returns the current date. The LLM invokes this when a user asks time-sensitive questions involving terms like "today", "now", or "yesterday".
- **`calculatorTool`**: Evaluates mathematical expressions using `exp4j`. The LLM invokes this to safely and accurately calculate numeric formulas (e.g., pricing, discounts, aggregates) instead of guessing the arithmetic.
- **`knowledgeSearchTool`**: Performs an explicit search against the knowledge base by delegating back to the existing `HybridDocumentRetriever`. The LLM invokes this tool when it determines it needs more domain-specific information than what was provided in the initial prompt context.

---

## 🚀 Getting Started

### Prerequisites
- Java 25+
- Docker (for Testcontainers)
- No manual database setup needed

### Running Locally
```bash
./mvnw spring-boot:run
```
Testcontainers automatically spins up PostgreSQL (pgvector) and Ollama.

### Running Tests
```bash
./mvnw verify
```

---

## 📡 API Reference

### Upload Document
```bash
curl -X POST -F "file=@manual.pdf" \
  "http://localhost:8080/api/data/v1/upload?documentType=manual&owner=support&category=hardware"
```
Supported formats: `.pdf`, `.txt`, `.json`

### Upload Sample Data (for ReAct Testing)
We provide sample data to test the agentic capabilities (calculations and date references).
```bash
curl -X POST -F "file=@src/main/resources/sample-data/company-policies.txt" \
  "http://localhost:8080/api/data/v1/upload?documentType=policy&category=hr"

curl -X POST -F "file=@src/main/resources/sample-data/events.txt" \
  "http://localhost:8080/api/data/v1/upload?documentType=event&category=general"
```

### Chat
```bash
curl -X POST http://localhost:8080/api/ai/chat \
  -H "Content-Type: application/json" \
  -d '{"question": "How to reboot?", "documentType": "manual", "category": "hardware"}'
```

### Chat with Diagnostics
```bash
curl -X POST "http://localhost:8080/api/ai/chat?includeDiagnostics=true" \
  -H "Content-Type: application/json" \
  -d '{"question": "How to reboot?"}'
```

### Agentic Tool Invocation: Date Example
Demonstrates the LLM invoking the `currentDateTimeTool` to determine today's date.
```bash
curl -X POST http://localhost:8080/api/ai/chat \
  -H "Content-Type: application/json" \
  -d '{"question": "What is the date today?"}'
```

### Agentic Tool Invocation: Calculation & Knowledge Search
Demonstrates the LLM invoking the `knowledgeSearchTool` to look up product pricing context, and then invoking the `calculatorTool` to apply the requested discount.
```bash
curl -X POST http://localhost:8080/api/ai/chat \
  -H "Content-Type: application/json" \
  -d '{"question": "If product X costs $1,250 and I need 3 units with 10% bulk discount, what is the total?"}'
```

### Clear Cache
```bash
curl -X DELETE http://localhost:8080/api/data/v1/cache
```

### Document Count
```bash
curl http://localhost:8080/api/data/v1/count
```

---

## ⚙️ Configuration Reference

All properties are configured in `application.properties` using `@ConfigurationProperties` binding.

### Tool Configuration
- **Auto-Discovery**: Tools are defined as standard Spring `@Bean`s returning `ToolCallback` (e.g., in `ToolConfiguration.java`). The `AIChatService` automatically discovers them from the Spring context.
- **Enabled by Default**: Tool calling is automatically enabled whenever these beans are present in the context.
- **RAG vs Explicit Tool**: `RetrievalAugmentationAdvisor` handles *automatic* background context injection for every prompt. In contrast, `knowledgeSearchTool` enables *explicit*, LLM-initiated searches when the LLM decides it needs more context.

### Retrieval Pipeline (`rag.retrieval.*`)

| Property                             | Default  | Description                                          |
|--------------------------------------|----------|------------------------------------------------------|
| `rag.retrieval.mode`                 | `hybrid` | Retrieval strategy: `vector`, `keyword`, or `hybrid` |
| `rag.retrieval.top-k`                | `3`      | Number of documents for vector search                |
| `rag.retrieval.similarity-threshold` | `0.6`    | Minimum cosine similarity for vector results         |
| `rag.retrieval.keyword.top-k`        | `3`      | Number of documents for keyword search               |
| `rag.retrieval.rrf.k`                | `60`     | RRF constant (higher = more uniform weighting)       |
| `rag.retrieval.hybrid.top-k`         | `3`      | Final result count after fusion                      |
| `rag.retrieval.rerank.enabled`       | `true`   | Enable keyword-overlap reranking                     |
| `rag.retrieval.rerank.top-k`         | `3`      | Documents to keep after reranking                    |

### Chunking (`rag.chunking.*`)

| Property                       | Default               | Description                                          |
|--------------------------------|-----------------------|------------------------------------------------------|
| `rag.chunking.strategy`        | `token`               | `token` or `section`                                 |
| `rag.chunking.size`            | `300`                 | Max chunk size in tokens                             |
| `rag.chunking.min-size`        | `100`                 | Min chunk size in characters                         |
| `rag.chunking.section.pattern` | `(^#+\s+.*$)\|(\n\n)` | Regex for section boundaries (when strategy=section) |

### Cache (`rag.cache.*`)

| Property                | Default | Description                     |
|-------------------------|---------|---------------------------------|
| `rag.cache.enabled`     | `true`  | Enable Caffeine retrieval cache |
| `rag.cache.ttl-seconds` | `3600`  | Cache entry time-to-live        |
| `rag.cache.max-size`    | `1000`  | Maximum cache entries           |

### Query Transformation (`rag.query.*`)

| Property                          | Default | Description                                                 |
|-----------------------------------|---------|-------------------------------------------------------------|
| `rag.query.multiquery.enabled`    | `false` | Enable Multi-Query generation via LLM                       |
| `rag.query.multiquery.variations` | `3`     | Number of variations to generate                            |
| `rag.query.self-querying-enabled` | `false` | Enable Self-Querying / QueryAnalyzer feature                |
| `rag.query.model`                 | `null`  | Optional override for the LLM used in query transformations |

### Guardrails (`guardrails.*`)

| Property                     | Default                 | Description                    |
|------------------------------|-------------------------|--------------------------------|
| `guardrails.sensitive-words` | `politics,religion,...` | Comma-separated blocked words  |
| `guardrails.failure-message` | `I'm sorry, but I...`   | Response when query is blocked |
| `guardrails.logging.enabled` | `true`                  | Enable prompt/response logging |

### Observability

| Property                                    | Default                          | Description                |
|---------------------------------------------|----------------------------------|----------------------------|
| `management.endpoints.web.exposure.include` | `health,info,metrics,prometheus` | Exposed actuator endpoints |
| `management.tracing.sampling.probability`   | `1.0`                            | Trace sampling rate        |

---

## 🛡️ Guardrails

| Feature                   | Implemented | Description                                                                   |
|---------------------------|-------------|-------------------------------------------------------------------------------|
| Input Validation          | Yes         | Rejects empty, overly long, or malformed queries.                             |
| Sensitive Topic Filtering | Yes         | Blocks queries containing defined restricted words (e.g. politics, violence). |
| PII Redaction             | No          | Does not yet detect or strip Personally Identifiable Information from inputs. |
| Output Content Filtering  | No          | Does not review the LLM's response for safety violations.                     |

---

## 🧠 Alternative Approaches

1. **SQL-Level Hybrid Fusion:** Write a single, complex PostgreSQL query that calculates both vector distance and keyword text-match score. Great for pagination, but harder to tune.
2. **LLM-Based Re-ranking:** Pass all candidate documents back into an AI (Cross-Encoder) to accurately score relevance. Extremely accurate but very slow and computationally expensive.
3. **External Re-ranking Services:** Use an API like Cohere Rerank. Very fast and accurate, but introduces a dependency on an external vendor.
4. **Pure BM25 / Keyword-Only Retrieval:** Rely entirely on traditional text search. Extremely fast, but cannot understand synonyms or the "meaning" of a query.

---

## 🔧 Performance Tuning

1. **HNSW:** Increase `m` (default 24) and `ef_construction` (default 128) for better recall at the cost of RAM and build time.
2. **Caching:** Lower `rag.cache.ttl-seconds` for frequently changing data. Cache is auto-cleared on document upload only when the ingestion process completes (the controller clears the cache based on ingestion status).
3. **Monitoring:** Track `rag.cache.hits`/`misses`, `rag.rerank.latency`, `rag.llm.calls` via `/actuator/metrics`.
