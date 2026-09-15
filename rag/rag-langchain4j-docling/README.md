# RAG Langchain4j with Docling

This project is a comprehensive Document Ingestion and Structured Retrieval system.
It leverages LangChain4j, Docling (for advanced layout-aware PDF parsing), PgVector, and ONNX local embeddings (AllMiniLmL6V2).

## Architecture
```mermaid
graph LR
A[PDF Files] --> B(Batch Ingestion API)
B --> C(DoclingDocumentParser)
C --> D(StructureAwareChunker)
D --> E(MetadataEnricher)
E --> F(ONNX Embeddings)
F --> G[(PgVector)]
H[Retrieval API] --> I(StructuredRetrievalService)
I --> G
```

## Components
- **Docling**: Parses PDFs into structured Markdown, retaining table boundaries and heading hierarchy.
- **PgVector**: PostgreSQL extension for storing embeddings and performing vector similarity search.
- **ONNX Model**: `AllMiniLmL6V2` generates 384-dimensional embeddings locally.

## Configuration Reference
| Property | Default | Description |
|---|---|---|
| `docling.server.url` | `http://localhost:5001` | URL for the `docling-serve` container |
| `docling.connect-timeout` | `5s` | Timeout for connecting to Docling |
| `docling.read-timeout` | `2m` | Timeout for a Docling conversion response |
| `ingestion.allowed-base-directory` | `./ingestion` | Filesystem subtree allowed for directory ingestion |
| `langchain4j.rag.chunking.size` | `300` | Max characters per text chunk |
| `langchain4j.rag.chunking.overlap` | `50` | Overlap characters between chunks |
| `ingestion.parallelism` | `4` | Concurrency for batch ingestion |
| `spring.threads.virtual.enabled` | `true` | Use Java virtual threads |

Set `DB_URL`, `DB_USERNAME`, and `DB_PASSWORD` before starting the application. The
credentials in `docker-compose.yml` are development-only defaults. Directory ingestion
also requires an authenticated user with the `ADMIN` role; for local use, configure
`SPRING_SECURITY_USER_NAME`, `SPRING_SECURITY_USER_PASSWORD`, and
`SPRING_SECURITY_USER_ROLES=ADMIN`.

## API Examples

### Batch Ingestion
```bash
curl -X POST "http://localhost:8080/api/ingest/batch" -F "files=@document.pdf"
```

### Retrieval
```bash
curl -X POST "http://localhost:8080/api/retrieve" \
     -H "Content-Type: application/json" \
     -d '{"query": "What is the revenue?", "hasTable": true}'
```

### Benchmarks
```bash
curl "http://localhost:8080/api/benchmark/results"
```
