package com.learning.ai.modelregression.dataset;

import java.util.List;

public record GoldenDataset(String datasetVersion, List<GoldenCase> cases) {}
