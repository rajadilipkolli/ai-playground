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
        List<EvaluationRun> last7Runs = runRepository.getRecentRuns(7);
        if (last7Runs.size() < 7) {
            return false; // Not enough history
        }

        double sum = 0;
        for (EvaluationRun r : last7Runs) {
            sum += r.passRatePercent();
        }
        double movingAverage = sum / last7Runs.size();

        return movingAverage < thresholds.getDriftThreshold();
    }
}
