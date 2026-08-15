package com.learning.ai.modelregression.alert;

import com.learning.ai.modelregression.eval.RunComparator;
import com.learning.ai.modelregression.storage.EvaluationRun;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class SlackNotifier {

    private static final Logger log = LoggerFactory.getLogger(SlackNotifier.class);
    private final String webhookUrl;
    private final RestClient restClient;

    public SlackNotifier(@Value("${slack.webhook.url:}") String webhookUrl) {
        this.webhookUrl = webhookUrl;
        this.restClient = RestClient.create();
    }

    public void notify(
            EvaluationRun run,
            RunComparator.ComparisonResult comparisonResult,
            boolean driftDetected,
            String reportUrl) {
        if (webhookUrl == null || webhookUrl.isBlank()) {
            log.info("Slack webhook URL not configured, skipping notification.");
            return;
        }

        String statusEmoji = comparisonResult.status() == RunComparator.Status.PASS
                ? ":white_check_mark:"
                : comparisonResult.status() == RunComparator.Status.WARN ? ":warning:" : ":rotating_light:";

        String driftMsg = driftDetected ? "\n:chart_with_downwards_trend: *SLOW DRIFT DETECTED*" : "";

        String payload = String.format(
                """
                {
                    "text": "Model Regression Status: %s",
                    "blocks": [
                        {
                            "type": "header",
                            "text": {
                                "type": "plain_text",
                                "text": "%s Model Regression: %s"
                            }
                        },
                        {
                            "type": "section",
                            "text": {
                                "type": "mrkdwn",
                                "text": "*Model:* %s\\n*Pass Rate:* %.2f%% (%+.2f%%)\\n*Regressions:* %d%s"
                            }
                        },
                        {
                            "type": "actions",
                            "elements": [
                                {
                                    "type": "button",
                                    "text": {
                                        "type": "plain_text",
                                        "text": "View HTML Report"
                                    },
                                    "url": "%s"
                                }
                            ]
                        }
                    ]
                }
                """,
                comparisonResult.status().name(),
                statusEmoji,
                comparisonResult.status().name(),
                run.model(),
                run.passRatePercent(),
                comparisonResult.passRateDelta(),
                comparisonResult.regressedCases().size(),
                driftMsg,
                reportUrl != null ? reportUrl : "https://github.com");

        try {
            restClient
                    .post()
                    .uri(webhookUrl)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(payload)
                    .retrieve()
                    .toBodilessEntity();
            log.info("Sent Slack notification.");
        } catch (Exception e) {
            log.error("Failed to send Slack notification", e);
        }
    }
}
