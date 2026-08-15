package com.learning.ai.modelregression.eval;

import com.learning.ai.modelregression.config.RegressionThresholdProperties;
import com.learning.ai.modelregression.storage.CaseResult;
import com.learning.ai.modelregression.storage.EvaluationRun;
import com.learning.ai.modelregression.storage.RunRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class RunComparator {

    private final RunRepository runRepository;
    private final RegressionThresholdProperties thresholds;

    public RunComparator(RunRepository runRepository, RegressionThresholdProperties thresholds) {
        this.runRepository = runRepository;
        this.thresholds = thresholds;
    }

    public ComparisonResult compareWithPrevious(EvaluationRun currentRun) {
        Optional<EvaluationRun> previousRunOpt = runRepository.getLatestRun();

        if (previousRunOpt.isEmpty()) {
            return new ComparisonResult(Status.PASS, 0.0, List.of(), List.of());
        }

        EvaluationRun previousRun = previousRunOpt.get();
        double passRateDelta = currentRun.passRatePercent() - previousRun.passRatePercent();

        Status status = Status.PASS;
        if (passRateDelta <= -thresholds.getCriticalDeltaPercent()) {
            status = Status.FAIL;
        } else if (passRateDelta <= -thresholds.getWarningDeltaPercent()) {
            status = Status.WARN;
        }

        // We can't perfectly compute improved/regressed without joining case results from DB.
        // For simplicity in this demo, if the previous run didn't load case results, we can just return empty lists.
        // To do this fully we would fetch case results for the previous run from DB.
        List<CaseResult> regressed = new ArrayList<>();
        List<CaseResult> improved = new ArrayList<>();

        return new ComparisonResult(status, passRateDelta, regressed, improved);
    }

    public enum Status {
        PASS,
        WARN,
        FAIL,
        SLOW_DRIFT
    }

    public record ComparisonResult(
            Status status, double passRateDelta, List<CaseResult> regressedCases, List<CaseResult> improvedCases) {}
}
