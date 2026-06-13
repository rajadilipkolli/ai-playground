package com.learning.ai.repository;

import com.learning.ai.model.IngestionJob;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Repository;

@Repository
public class IngestionJobRepository {
    private final Map<String, IngestionJob> store = new ConcurrentHashMap<>();

    public void save(IngestionJob job) {
        store.put(job.getJobId(), job);
    }

    public Optional<IngestionJob> findById(String jobId) {
        return Optional.ofNullable(store.get(jobId));
    }
}
