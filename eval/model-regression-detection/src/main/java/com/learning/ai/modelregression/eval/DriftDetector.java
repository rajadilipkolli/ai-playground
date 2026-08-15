package com.learning.ai.modelregression.eval;

import com.learning.ai.modelregression.config.RegressionThresholdProperties;
import com.learning.ai.modelregression.storage.EvaluationRun;
import com.learning.ai.modelregression.storage.RunRepository;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class DriftDetector {

    private final RunRepository runRepository;
    private final RegressionThresholdProperties thresholds;

    public DriftDetector(RunRepository runRepository, RegressionThresholdProperties thresholds) {
        this.runRepository = runRepository;
        this.thresholds = thresholds;
    }

    public boolean hasSlowDrift(EvaluationRun currentRun) {
        List<EvaluationRun> previousRuns = runRepository.getRecentRuns(6);
        if (previousRuns.size() < 6) {
            return false; // Not enough history
        }

        double sum = currentRun.passRatePercent();
        for (EvaluationRun r : previousRuns) {
            sum += r.passRatePercent();
        }
        double movingAverage = sum / 7.0;

        return movingAverage < thresholds.getDriftThreshold();
    }
}
