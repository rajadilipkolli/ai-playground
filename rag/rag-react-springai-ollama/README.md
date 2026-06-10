# ReAct RAG Spring AI Ollama

This module demonstrates an agentic Chat Service using Spring AI and Ollama, combining Retrieval-Augmented Generation (RAG) with multi-tool calling through the ReAct (Reasoning and Acting) pattern.

## Prerequisites
* Docker & Docker Compose
* Java 25
* Maven

## Setup & Running

1. **Start the Infrastructure**
   ```bash
   cd docker
   docker-compose up -d
   ```
   This will start Ollama, PostgreSQL (with pgvector), and optionally the LGTM observability stack.

2. **Pull the Ollama Models**
   Ensure the required models are pulled inside the Ollama container:
   ```bash
   docker exec -it react_rag_ollama ollama run mistral
   docker exec -it react_rag_ollama ollama pull nomic-embed-text
   ```

3. **Run the Application (Dev Mode)**
   Run the `TestReactRagApplication` class from your IDE, or use Maven:
   ```bash
   mvn spring-boot:test-run
   ```
   Alternatively, you can run the standard `ReactRagApplication`.

4. **Ingest Sample Data**
   Upload the sample data files to populate the knowledge base:
   ```bash
   curl -X POST -F "file=@src/main/resources/sample-data/company-policies.txt" http://localhost:8080/api/documents/upload
   curl -X POST -F "file=@src/main/resources/sample-data/events.txt" http://localhost:8080/api/documents/upload
   ```

## Example API Calls

**1. Factual Retrieval + Calculation**
```bash
curl -X POST -H "Content-Type: application/json" -d '{"query":"If a customer buys 6 Quantum Widgets, how much do they pay in total?"}' http://localhost:8080/api/chat?diagnostics=true
```
*Expected Behavior*: The LLM uses `knowledgeSearchTool` to find the price of a Quantum Widget ($1250) and the discount policy (10% off for >5). It then uses the `calculatorTool` to evaluate `(6 * 1250) * 0.9` and returns the final answer.

**2. Date-Dependent Questions**
```bash
curl -X POST -H "Content-Type: application/json" -d '{"query":"When is the deadline to submit dietary preferences for the retreat?"}' http://localhost:8080/api/chat?diagnostics=true
```
*Expected Behavior*: The LLM uses `currentDateTimeTool` to get today's date, searches the knowledge base to find the retreat is 30 days away and preferences are due 14 days before, calculates the target date, and returns it.

## The ReAct Pattern

This application implements the **ReAct (Reason and Act)** pattern. A specific system prompt instructs the LLM to iteratively:
1. **Analyze** the user's question.
2. **Decide** if any of its available tools are needed.
3. **Execute** tools using Spring AI's internal tool execution (`internal-tool-execution-enabled=true`).
4. **Synthesize** a final answer based on tool outputs.

### Available Tools:
*   `currentDateTimeTool`: Gets the current system date and time.
*   `calculatorTool`: Evaluates SpEL mathematical strings safely.
*   `knowledgeSearchTool`: Performs an HNSW similarity search on the pgvector store.

## Configuration Options

You can configure models and retrieval parameters in `src/main/resources/application.properties`:
*   `spring.ai.ollama.chat.model`: The LLM used for chatting and tool calling (e.g., `mistral`).
*   `spring.ai.ollama.embedding.model`: The embedding model used for vectorization (e.g., `nomic-embed-text`).
*   `spring.ai.vectorstore.pgvector.dimensions`: Ensure this matches your embedding model (768 for `nomic-embed-text`).

## Troubleshooting

*   **Error evaluating expression / Math errors**: Ensure the LLM is constructing valid SpEL expressions.
*   **No relevant information found**: Verify the documents were uploaded successfully and the `pgvector` container is healthy.
*   **Model not found errors**: Ensure you've pulled the models manually using `docker exec -it react_rag_ollama ollama pull <model_name>`.
*   **LLM responds with tool code instead of running it**: Make sure `internal-tool-execution-enabled=true` is set, and your model supports tool calling (e.g., `llama3.1`, `mistral`).
