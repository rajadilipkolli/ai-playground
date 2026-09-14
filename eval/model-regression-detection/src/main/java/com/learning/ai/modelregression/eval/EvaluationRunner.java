package com.learning.ai.modelregression.eval;

import com.learning.ai.modelregression.dataset.GoldenCase;
import com.learning.ai.modelregression.dataset.GoldenDataset;
import com.learning.ai.modelregression.eval.scoring.SummaryRelevanceJudge;
import com.learning.ai.modelregression.model.PromptConfig;
import com.learning.ai.modelregression.service.EmailClassifierService;
import com.learning.ai.modelregression.storage.CaseResult;
import com.learning.ai.modelregression.storage.EvaluationRun;
import jakarta.annotation.PreDestroy;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class EvaluationRunner {
    private static final Logger log = LoggerFactory.getLogger(EvaluationRunner.class);

    private final EmailClassifierService classifierService;
    private final SummaryRelevanceJudge judge;
    private final ExecutorService executorService;
    private final String modelName;

    public EvaluationRunner(
            EmailClassifierService classifierService,
            SummaryRelevanceJudge judge,
            @Value("${spring.ai.ollama.chat.model:llama3.2}") String modelName) {
        this.classifierService = classifierService;
        this.judge = judge;
        this.executorService = Executors.newFixedThreadPool(4); // Bounded executor for parallel processing
        this.modelName = modelName;
    }

    @PreDestroy
    public void destroy() {
        this.executorService.shutdown();
    }

    public EvaluationRun runEvaluation(GoldenDataset dataset, PromptConfig promptConfig) {
        log.info("Starting evaluation run with {} cases", dataset.cases().size());

        List<CompletableFuture<CaseResult>> futures = dataset.cases().stream()
                .map(gc -> CompletableFuture.supplyAsync(() -> processCase(gc, promptConfig), executorService))
                .toList();

        List<CaseResult> results = new ArrayList<>();
        for (CompletableFuture<CaseResult> f : futures) {
            results.add(f.join());
        }

        long totalLatency = 0;
        int totalTokens = 0;
        int passCount = 0;

        for (CaseResult cr : results) {
            totalLatency += cr.latencyMs();
            totalTokens += cr.tokens();
            if (cr.pass()) {
                passCount++;
            }
        }

        double passRate = results.isEmpty() ? 0 : (double) passCount / results.size() * 100.0;
        double avgLatency = results.isEmpty() ? 0 : (double) totalLatency / results.size();

        return new EvaluationRun(
                UUID.randomUUID().toString(),
                LocalDateTime.now(),
                promptConfig.version(),
                dataset.datasetVersion(),
                modelName,
                passRate,
                avgLatency,
                totalTokens,
                results);
    }

    private CaseResult processCase(GoldenCase goldenCase, PromptConfig promptConfig) {
        EmailClassifierService.ClassificationResult result =
                classifierService.classify(goldenCase.email(), promptConfig);

        if (result.error() || result.classification() == null) {
            return new CaseResult(
                    goldenCase.id(),
                    false,
                    0,
                    result.latencyMs(),
                    result.inputTokens() + result.outputTokens(),
                    false,
                    goldenCase.email(),
                    goldenCase.expectedCategory().name(),
                    goldenCase.expectedSummary(),
                    "ERROR",
                    "ERROR");
        }

        boolean categoryMatch =
                goldenCase.expectedCategory() == result.classification().category();

        SummaryRelevanceJudge.JudgeResult judgeResult = judge.evaluate(
                goldenCase.email(),
                goldenCase.expectedSummary(),
                result.classification().summary());

        // Pass rule: exact category match AND minimum relevance score of 4
        boolean pass = categoryMatch && judgeResult.score() >= 4;

        return new CaseResult(
                goldenCase.id(),
                categoryMatch,
                judgeResult.score(),
                result.latencyMs(),
                result.inputTokens() + result.outputTokens(),
                pass,
                goldenCase.email(),
                goldenCase.expectedCategory().name(),
                goldenCase.expectedSummary(),
                result.classification().category().name(),
                result.classification().summary());
    }
}
