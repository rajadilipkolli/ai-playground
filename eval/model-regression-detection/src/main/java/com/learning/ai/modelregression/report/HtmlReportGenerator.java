package com.learning.ai.modelregression.report;

import com.learning.ai.modelregression.eval.RunComparator;
import com.learning.ai.modelregression.storage.CaseResult;
import com.learning.ai.modelregression.storage.EvaluationRun;
import com.learning.ai.modelregression.storage.RunRepository;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.web.util.HtmlUtils;

@Component
public class HtmlReportGenerator {

    private final RunRepository runRepository;

    public HtmlReportGenerator(RunRepository runRepository) {
        this.runRepository = runRepository;
    }

    public String generateReport(
            EvaluationRun currentRun, RunComparator.ComparisonResult comparison, String driftStatus) {
        List<EvaluationRun> recentRuns = runRepository.getRecentRuns(10);

        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html><html><head><title>Model Regression Report</title>");
        html.append(
                "<style>body{font-family:sans-serif;margin:20px;} table{border-collapse:collapse;width:100%;} th,td{border:1px solid #ccc;padding:8px;text-align:left;} .fail{color:red;} .pass{color:green;} .warn{color:orange;}</style>");
        html.append("</head><body>");

        html.append("<h1>Model Regression Detection Report</h1>");
        html.append("<h2>Run Metadata</h2>");
        html.append("<ul>");
        html.append("<li><strong>Run ID:</strong> ")
                .append(HtmlUtils.htmlEscape(currentRun.runId()))
                .append("</li>");
        html.append("<li><strong>Timestamp:</strong> ")
                .append(currentRun.timestamp())
                .append("</li>");
        html.append("<li><strong>Prompt Version:</strong> ")
                .append(HtmlUtils.htmlEscape(currentRun.promptVersion()))
                .append("</li>");
        html.append("<li><strong>Dataset Version:</strong> ")
                .append(HtmlUtils.htmlEscape(currentRun.datasetVersion()))
                .append("</li>");
        html.append("<li><strong>Model:</strong> ")
                .append(HtmlUtils.htmlEscape(currentRun.model()))
                .append("</li>");
        html.append("<li><strong>Overall Pass Rate:</strong> ")
                .append(String.format("%.2f%%", currentRun.passRatePercent()))
                .append("</li>");
        html.append("<li><strong>Status:</strong> ").append(comparison.status()).append("</li>");
        if (driftStatus != null) {
            html.append("<li><strong>Drift Alert:</strong> <span class='warn'>")
                    .append(HtmlUtils.htmlEscape(driftStatus))
                    .append("</span></li>");
        }
        html.append("</ul>");

        html.append("<h2>Trend (Last 10 Runs)</h2>");
        html.append("<table><tr><th>Run ID</th><th>Timestamp</th><th>Pass Rate</th></tr>");
        for (EvaluationRun r : recentRuns) {
            html.append("<tr>")
                    .append("<td>")
                    .append(HtmlUtils.htmlEscape(r.runId()))
                    .append("</td>")
                    .append("<td>")
                    .append(r.timestamp())
                    .append("</td>")
                    .append("<td>")
                    .append(String.format("%.2f%%", r.passRatePercent()))
                    .append("</td>")
                    .append("</tr>");
        }
        html.append("</table>");

        if (!comparison.regressedCases().isEmpty()) {
            html.append("<h2>Regressed Cases</h2>");
            html.append(
                    "<table><tr><th>Case ID</th><th>Email</th><th>Expected Cat/Sum</th><th>Actual Cat/Sum</th></tr>");
            for (CaseResult cr : comparison.regressedCases()) {
                html.append("<tr>")
                        .append("<td>")
                        .append(HtmlUtils.htmlEscape(cr.caseId()))
                        .append("</td>")
                        .append("<td>")
                        .append(HtmlUtils.htmlEscape(cr.originalEmail()))
                        .append("</td>")
                        .append("<td>")
                        .append(HtmlUtils.htmlEscape(cr.expectedCategory()))
                        .append("<br/>")
                        .append(HtmlUtils.htmlEscape(cr.expectedSummary()))
                        .append("</td>")
                        .append("<td>")
                        .append(HtmlUtils.htmlEscape(cr.actualCategory()))
                        .append("<br/>")
                        .append(HtmlUtils.htmlEscape(cr.actualSummary()))
                        .append("</td>")
                        .append("</tr>");
            }
            html.append("</table>");
        }

        html.append("</body></html>");

        String content = html.toString();
        try {
            Path path = Paths.get("target/regression-report.html");
            Files.createDirectories(path.getParent());
            Files.writeString(path, content);
            return path.toAbsolutePath().toString();
        } catch (IOException e) {
            throw new RuntimeException("Failed to write HTML report", e);
        }
    }
}
