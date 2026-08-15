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

        List<CaseResult> regressed = new ArrayList<>();
        List<CaseResult> improved = new ArrayList<>();

        for (CaseResult currentCase : currentRun.caseResults()) {
            Optional<CaseResult> previousCaseOpt = previousRun.caseResults().stream()
                    .filter(pc -> pc.caseId().equals(currentCase.caseId()))
                    .findFirst();

            if (previousCaseOpt.isPresent()) {
                CaseResult previousCase = previousCaseOpt.get();
                if (previousCase.pass() && !currentCase.pass()) {
                    regressed.add(currentCase);
                } else if (!previousCase.pass() && currentCase.pass()) {
                    improved.add(currentCase);
                }
            }
        }

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
