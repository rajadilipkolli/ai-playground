package com.learning.ai.modelregression.storage;

import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class RunRepository {

    private final JdbcTemplate jdbcTemplate;

    public RunRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Transactional
    public void saveRun(EvaluationRun run) {
        jdbcTemplate.update(
                "INSERT INTO runs (run_id, timestamp, prompt_version, dataset_version, model, pass_rate_percent, average_latency_ms, total_tokens) VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                run.runId(),
                Timestamp.valueOf(run.timestamp()),
                run.promptVersion(),
                run.datasetVersion(),
                run.model(),
                run.passRatePercent(),
                run.averageLatencyMs(),
                run.totalTokens());

        for (CaseResult cr : run.caseResults()) {
            // Note: We're not persisting the expected/actual category and summary to the DB for brevity,
            // but we could if we wanted to fully support the diff report from DB alone.
            // In Phase 4, the diff report will use the current run's in-memory data for the report,
            // while the DB handles historical scores.
            jdbcTemplate.update(
                    "INSERT INTO case_results (run_id, case_id, category_match, relevance_score, latency_ms, tokens, pass) VALUES (?, ?, ?, ?, ?, ?, ?)",
                    run.runId(),
                    cr.caseId(),
                    cr.categoryMatch(),
                    cr.relevanceScore(),
                    cr.latencyMs(),
                    cr.tokens(),
                    cr.pass());
        }
    }

    public List<EvaluationRun> getRecentRuns(int limit) {
        return jdbcTemplate.query(
                "SELECT * FROM runs ORDER BY timestamp DESC LIMIT ?",
                (rs, rowNum) -> new EvaluationRun(
                        rs.getString("run_id"),
                        rs.getTimestamp("timestamp").toLocalDateTime(),
                        rs.getString("prompt_version"),
                        rs.getString("dataset_version"),
                        rs.getString("model"),
                        rs.getDouble("pass_rate_percent"),
                        rs.getDouble("average_latency_ms"),
                        rs.getInt("total_tokens"),
                        List.of() // We don't fetch case results for historical runs in this simple implementation
                        // unless needed
                        ),
                limit);
    }

    public Optional<EvaluationRun> getLatestRun() {
        List<EvaluationRun> recent = getRecentRuns(1);
        if (recent.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(recent.get(0));
    }
}
