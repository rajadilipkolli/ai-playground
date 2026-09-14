CREATE TABLE IF NOT EXISTS runs (
    run_id VARCHAR(36) PRIMARY KEY,
    timestamp TIMESTAMP NOT NULL,
    prompt_version VARCHAR(255) NOT NULL,
    dataset_version VARCHAR(255) NOT NULL,
    model VARCHAR(255) NOT NULL,
    pass_rate_percent DOUBLE PRECISION NOT NULL,
    average_latency_ms DOUBLE PRECISION NOT NULL,
    total_tokens INTEGER NOT NULL
);

CREATE TABLE IF NOT EXISTS case_results (
    id SERIAL PRIMARY KEY,
    run_id VARCHAR(36) NOT NULL,
    case_id VARCHAR(255) NOT NULL,
    category_match BOOLEAN NOT NULL,
    relevance_score INTEGER NOT NULL,
    latency_ms BIGINT NOT NULL,
    tokens INTEGER NOT NULL,
    pass BOOLEAN NOT NULL,
    FOREIGN KEY (run_id) REFERENCES runs(run_id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_case_results_run_case ON case_results (run_id, case_id);
CREATE INDEX IF NOT EXISTS idx_runs_timestamp ON runs(timestamp);
