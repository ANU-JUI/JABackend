package com.example.jobautomate.service;

import com.example.jobautomate.dto.ApplyResponse;
import com.example.jobautomate.model.ApplicationRecord;
import com.example.jobautomate.repository.ApplicationRepository;
import java.time.OffsetDateTime;
import org.springframework.stereotype.Service;

@Service
public class ApplicationService {

    private final ApplicationRepository applicationRepository;

    public ApplicationService(ApplicationRepository applicationRepository) {
        this.applicationRepository = applicationRepository;
    }

    public ApplyResponse apply(String userId, Long jobId) {
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
