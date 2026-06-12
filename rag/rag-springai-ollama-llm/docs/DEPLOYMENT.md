# Deployment Guide

This document describes how to deploy the `rag-springai-ollama-llm` application to a production or staging environment.

## Prerequisites

- **Java 25+**: Ensure Java 25 is installed.
- **PostgreSQL 15+**: A PostgreSQL instance with the `pgvector` extension installed.
- **Ollama**: An Ollama server running with the necessary models pulled (e.g., `llama3.2` and `all-minilm`).

## Environment Variables

The application relies on the following environment variables. You must set these before starting the application:

```properties
SPRING_DATASOURCE_URL=jdbc:postgresql://<db-host>:<db-port>/<db-name>
SPRING_DATASOURCE_USERNAME=<db-username>
SPRING_DATASOURCE_PASSWORD=<db-password>
SPRING_AI_OLLAMA_BASE_URL=http://<ollama-host>:<ollama-port>
```

## Running the Application

### 1. Build the Application

Build the executable JAR using Maven:

```bash
./mvnw clean package -DskipTests
```

### 2. Start the Application

Run the generated JAR file:

```bash
java -jar target/rag-springai-ollama-llm-0.0.1-SNAPSHOT.jar
```

## Docker Deployment

You can also deploy the application using Docker.

### 1. Build the Docker Image

Use Spring Boot's built-in image builder:

```bash
./mvnw spring-boot:build-image
```

### 2. Run with Docker Compose

A `docker-compose.yml` is provided for running the application alongside its dependencies:

```yaml
version: '3.8'

services:
  app:
    image: rag-springai-ollama-llm:0.0.1-SNAPSHOT
    ports:
      - "8080:8080"
    environment:
      - SPRING_DATASOURCE_URL=jdbc:postgresql://db:5432/appdb
      - SPRING_DATASOURCE_USERNAME=appuser
      - SPRING_DATASOURCE_PASSWORD=secret
      - SPRING_AI_OLLAMA_BASE_URL=http://ollama:11434
    depends_on:
      - db
      - ollama

  db:
    image: pgvector/pgvector:pg16
    environment:
      POSTGRES_USER: appuser
      POSTGRES_PASSWORD: secret
      POSTGRES_DB: appdb

  ollama:
    image: ollama/ollama:latest
    ports:
      - "11434:11434"
```

## Production Considerations

- **Security**: Secure your Ollama endpoints if deploying to a public network.
- **Monitoring**: Ensure Actuator endpoints are properly secured.
- **Database Backups**: Regularly back up the PostgreSQL database.
