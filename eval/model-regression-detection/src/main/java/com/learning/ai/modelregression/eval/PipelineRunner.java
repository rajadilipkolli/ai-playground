package com.learning.ai.modelregression.eval;

import com.learning.ai.modelregression.alert.SlackNotifier;
import com.learning.ai.modelregression.dataset.GoldenDataset;
import com.learning.ai.modelregression.dataset.GoldenDatasetLoader;
import com.learning.ai.modelregression.model.PromptConfig;
import com.learning.ai.modelregression.prompt.PromptConfigLoader;
import com.learning.ai.modelregression.report.HtmlReportGenerator;
import com.learning.ai.modelregression.storage.EvaluationRun;
import com.learning.ai.modelregression.storage.RunRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("pipeline")
public class PipelineRunner implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(PipelineRunner.class);

    private final PromptConfigLoader promptConfigLoader;
    private final GoldenDatasetLoader goldenDatasetLoader;
    private final EvaluationRunner evaluationRunner;
    private final RunRepository runRepository;
    private final RunComparator runComparator;
    private final DriftDetector driftDetector;
    private final HtmlReportGenerator htmlReportGenerator;
    private final SlackNotifier slackNotifier;
    private final ApplicationContext applicationContext;

    public PipelineRunner(
            PromptConfigLoader promptConfigLoader,
            GoldenDatasetLoader goldenDatasetLoader,
            EvaluationRunner evaluationRunner,
            RunRepository runRepository,
            RunComparator runComparator,
            DriftDetector driftDetector,
            HtmlReportGenerator htmlReportGenerator,
            SlackNotifier slackNotifier,
            ApplicationContext applicationContext) {
        this.promptConfigLoader = promptConfigLoader;
        this.goldenDatasetLoader = goldenDatasetLoader;
        this.evaluationRunner = evaluationRunner;
        this.runRepository = runRepository;
        this.runComparator = runComparator;
        this.driftDetector = driftDetector;
        this.htmlReportGenerator = htmlReportGenerator;
        this.slackNotifier = slackNotifier;
        this.applicationContext = applicationContext;
    }

    @Override
    public void run(String... args) {
        log.info("Starting Evaluation Pipeline...");
        try {
            PromptConfig promptConfig = promptConfigLoader.loadConfig(null);
            GoldenDataset dataset = goldenDatasetLoader.loadDataset();

            EvaluationRun currentRun = evaluationRunner.runEvaluation(dataset, promptConfig);

            RunComparator.ComparisonResult comparison = runComparator.compareWithPrevious(currentRun);
            boolean isDrift = driftDetector.hasSlowDrift(currentRun);
            String driftStatus = isDrift ? "SLOW_DRIFT" : null;

            runRepository.saveRun(currentRun);

            String reportPath = htmlReportGenerator.generateReport(currentRun, comparison, driftStatus);
            slackNotifier.notify(currentRun, comparison, isDrift, reportPath);

            log.info(
                    "Pipeline finished. Status: {}, Drift: {}, Pass Rate: {}%",
                    comparison.status(), isDrift, currentRun.passRatePercent());

            if (comparison.status() == RunComparator.Status.FAIL) {
                log.error("Critical regression detected. Exiting with error code 1.");
                System.exit(SpringApplication.exit(applicationContext, () -> 1));
            } else {
                System.exit(SpringApplication.exit(applicationContext, () -> 0));
            }

        } catch (Exception e) {
            log.error("Pipeline failed", e);
            System.exit(SpringApplication.exit(applicationContext, () -> 1));
        }
    }
}
