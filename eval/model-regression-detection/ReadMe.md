# Model Regression Detection

A Spring Boot module for detecting regressions in LLM outputs.

## High Level Design (HLD)
This diagram shows the system from a bird's-eye view. It explains how different external triggers and systems interact with the Model Regression Detection framework.

```mermaid
graph TD
    Trigger[CI/CD Pipeline / Manual Run] -->|Triggers| EvalEngine[Evaluation Engine]
    EvalEngine -->|Fetches| Config[Prompts & Golden Dataset]
    EvalEngine <-->|Interacts via Spring AI| LLM[Ollama Local LLM]
    EvalEngine -->|Saves Results| DB[(PostgreSQL Database)]
    EvalEngine -->|Generates| Report[HTML Diff Report]
    EvalEngine -->|Alerts| Slack[Slack Webhook]
```

## Architecture Diagram
This diagram outlines the major components, boundaries, and technologies used within the module.

```mermaid
architecture-beta
    group api(cloud)[Spring Boot App]
    
    service runner(server)[Pipeline Runner] in api
    service classifier(server)[Email Classifier] in api
    service judge(server)[LLM Judge] in api
    service storage(database)[Run Repository] in api
    
    service db(database)[PostgreSQL]
    service llm(server)[Ollama Container]
    
    runner:R --> L:classifier
    runner:R --> L:judge
    runner:R --> L:storage
    
    classifier:R --> L:llm
    judge:R --> L:llm
    storage:R --> L:db
```

*Note: The module uses Spring AI to interface with the Ollama model and Spring JDBC to persist run metadata to PostgreSQL.*

## Low Level Design (LLD)
This diagram details the sequence of internal classes and how they collaborate to evaluate a single regression run.

```mermaid
sequenceDiagram
    participant PipelineRunner
    participant DatasetLoader
    participant EvalRunner as EvaluationRunner
    participant Classifier as EmailClassifierService
    participant Judge as SummaryRelevanceJudge
    participant LLM as Spring AI ChatClient
    participant Comparator as RunComparator
    participant DB as RunRepository

    PipelineRunner->>DatasetLoader: loadDataset()
    DatasetLoader-->>PipelineRunner: List<GoldenCase>
    PipelineRunner->>EvalRunner: runEvaluation(Dataset, PromptConfig)
    
    loop Parallel per Case (ExecutorService)
        EvalRunner->>Classifier: classify(emailText)
        Classifier->>LLM: prompt(email)
        LLM-->>Classifier: EmailClassification
        Classifier-->>EvalRunner: Result
        
        EvalRunner->>Judge: evaluate(actualSummary, expectedSummary)
        Judge->>LLM: prompt(evaluate)
        LLM-->>Judge: JudgeResult (1-5 score)
        Judge-->>EvalRunner: Score
    end
    
    EvalRunner-->>PipelineRunner: EvaluationRun (Pass Rate, Stats)
    
    PipelineRunner->>Comparator: compareWithPrevious(EvaluationRun)
    Comparator->>DB: getLatestRun()
    DB-->>Comparator: PreviousRun
    Comparator-->>PipelineRunner: ComparisonResult (Diffs, Status)
    
    PipelineRunner->>DB: saveRun(currentRun)
    PipelineRunner->>PipelineRunner: generate HTML & Slack Notify
```

## Configuration Reference

| Property | Description | Default |
|---|---|---|
| `eval.thresholds.warning-delta-percent` | Warning threshold for pass rate drop | 3.0 |
| `eval.thresholds.critical-delta-percent` | Critical threshold for pass rate drop | 8.0 |
| `eval.thresholds.drift-threshold` | 7-run moving average pass rate threshold | 85.0 |

## Adding Golden Cases
Add cases to `src/main/resources/golden-dataset.json`. Ensure diverse examples, especially edge cases.

## Adjusting Thresholds
Change the threshold properties in `application.properties` or environment variables to tune sensitivity.

## Architecture Decisions
- **PostgreSQL over SQLite**: Standardized on Postgres via PGvector to align with other repo modules.
- **Async Execution**: Used CompletableFuture with bounded thread pool to accelerate the evaluation process.
- **LLM-as-a-judge**: Used a low temperature LLM client to score summaries since exact-matching summaries is fragile.
