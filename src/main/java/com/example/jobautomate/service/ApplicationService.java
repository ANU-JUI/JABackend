package com.example.jobautomate.service;

import com.example.jobautomate.dto.ApplyResponse;
import com.example.jobautomate.model.ApplicationRecord;
import com.example.jobautomate.repository.ApplicationRepository;
import com.example.jobautomate.repository.JobRepository;
import java.time.OffsetDateTime;
import org.springframework.stereotype.Service;

@Service
public class ApplicationService {

    private final ApplicationRepository applicationRepository;
    private final JobRepository jobRepository;

    public ApplicationService(ApplicationRepository applicationRepository, JobRepository jobRepository) {
        this.applicationRepository = applicationRepository;
        this.jobRepository = jobRepository;
    }

    public ApplyResponse apply(String userId, Long jobId) {
        jobRepository.findById(jobId).orElseThrow(() -> new IllegalArgumentException("Job not found"));

        ApplicationRecord application = applicationRepository.findByUserIdAndJobId(userId, jobId)
            .orElseGet(ApplicationRecord::new);
        application.setUserId(userId);
        application.setJobId(jobId);
        application.setStatus("APPLIED");
        application.setAppliedAt(OffsetDateTime.now());
        applicationRepository.save(application);
        return new ApplyResponse(application.getStatus(), application.getAppliedAt());
    }
}
