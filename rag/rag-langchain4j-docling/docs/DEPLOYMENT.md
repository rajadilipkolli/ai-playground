# Deployment Guide

This document describes how to deploy the RAG Docling service locally and in production.

## Local Development (Docker Compose)

The project includes a `docker-compose.yml` for required infrastructure:
```bash
docker-compose up -d
```
This starts:
- `docling-serve` on port 5001.
- `pgvector` (PostgreSQL 16) on port 5432.

Once running, Spring Boot uses Liquibase to auto-create the `vector_store` table and triggers.

## Production Considerations

### Scaling Docling
Docling parsing is CPU and memory intensive. In production, consider deploying `docling-serve` in a Kubernetes cluster with autoscaling based on CPU utilization. Set `WORKERS` appropriately in the Docker environment.

### Database Sizing
PgVector requires sufficient RAM for indexing. Use `HNSW` or `IVFFlat` indexes if your dataset grows beyond 1 million vectors. The current Liquibase schema does not specify an HNSW index, which is fine for small datasets but should be added for scale.

### Performance Tuning
- Monitor ingestion throughput using `GET /api/benchmark/results`.
- Adjust `ingestion.parallelism` to balance throughput with Docling server load.
- Ensure `spring.threads.virtual.enabled=true` is set to utilize virtual threads for blocking REST calls to Docling.
