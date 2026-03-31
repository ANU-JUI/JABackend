package com.example.jobautomate.service;

import com.example.jobautomate.dto.JobDetailResponse;
import com.example.jobautomate.dto.JobFeedResponse;
import com.example.jobautomate.dto.JobView;
import com.example.jobautomate.dto.UserPreferencesRequest;
import com.example.jobautomate.model.ApplicationRecord;
import com.example.jobautomate.model.Job;
import com.example.jobautomate.repository.ApplicationRepository;
import com.example.jobautomate.repository.JobRepository;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class JobMatchingService {

    private static final int RETURN_LIMIT = 20;

    private final JobRepository jobRepository;
    private final ApplicationRepository applicationRepository;
    private final JobSemanticDataService jobSemanticDataService;

    public JobMatchingService(
            JobRepository jobRepository,
            ApplicationRepository applicationRepository,
            JobSemanticDataService jobSemanticDataService
    ) {
        this.jobRepository = jobRepository;
        this.applicationRepository = applicationRepository;
        this.jobSemanticDataService = jobSemanticDataService;
    }

    public JobFeedResponse buildFeed(UserPreferencesRequest preferences) {

        Set<Long> appliedIds = applicationRepository.findByUserId(preferences.userId())
                .stream()
                .map(ApplicationRecord::getJobId)
                .collect(Collectors.toSet());

        List<Job> jobs = jobRepository
                .findByActiveTrueAndApplicationDeadlineGreaterThanEqual(LocalDate.now());

        JobSemanticDataService.SemanticUserProfile userProfile =
                jobSemanticDataService.buildUserSemanticProfile(preferences);

        List<JobView> ranked = jobs.stream()
                .map(job -> score(job, userProfile, preferences, appliedIds.contains(job.getId())))
                .sorted(Comparator.comparingInt(JobView::matchScore).reversed())
                .limit(RETURN_LIMIT)
                .toList();

        List<JobView> available = ranked.stream().filter(j -> !j.applied()).toList();
        List<JobView> applied = ranked.stream().filter(JobView::applied).toList();

        return new JobFeedResponse(available, applied, available.size(), applied.size(),false);
    }

    public JobDetailResponse getJobDetail(Long jobId, UserPreferencesRequest preferences) {

        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new RuntimeException("Job not found"));

        boolean applied = applicationRepository
                .findByUserIdAndJobId(preferences.userId(), jobId)
                .isPresent();

        JobSemanticDataService.SemanticUserProfile userProfile =
                jobSemanticDataService.buildUserSemanticProfile(preferences);

        JobView view = score(job, userProfile, preferences, applied);

        return new JobDetailResponse(
                view.id(),
                view.title(),
                view.company(),
                view.location(),
                view.experienceRequired(),
                view.jobType(),
                view.deadline(),
                view.applyLink(),
                job.getDescription(),
                List.of(),
                view.matchScore(),
                List.of(),
                "",
                view.applied()
        );
    }

    private JobView score(
            Job job,
            JobSemanticDataService.SemanticUserProfile userProfile,
            UserPreferencesRequest preferences,
            boolean applied
    ) {

        List<Double> userVec = userProfile.embedding();
        List<Double> jobVec = job.getEmbeddingValues();

        int score = 0;

        if (!userVec.isEmpty() && !jobVec.isEmpty() && userVec.size() == jobVec.size()) {

            double sim = cosineSimilarity(userVec, jobVec);
            score = (int) Math.round(sim * 100);

            Integer required = job.getRequiredExperienceYears();

            if (required != null) {
                int gap = preferences.experienceYears() - required;

                if (gap < -3) score -= 25;
                else if (gap < -1) score -= 12;
            }

            score += freshnessBoost(job.getLastFetchedAt());

            if (preferences.countries() != null &&
                    preferences.countries().stream()
                            .anyMatch(c -> c.equalsIgnoreCase(job.getCountry()))) {
                score += 4;
            }

            score = clamp(score);
        }

        return new JobView(
                job.getId(),
                job.getTitle(),
                job.getCompany(),
                job.getLocationLabel(),
                job.getRequiredExperienceYears(),
                job.getJobType(),
                job.getApplicationDeadline(),
                job.getApplyLink(),
                List.of(),
                score,
                List.of(),
                "",
                "StoredJob",
                applied
        );
    }

    private int freshnessBoost(OffsetDateTime fetchedAt) {
        if (fetchedAt == null) return 0;

        long days = ChronoUnit.DAYS.between(fetchedAt.toLocalDate(), LocalDate.now());

        if (days <= 1) return 6;
        if (days <= 3) return 4;
        if (days <= 7) return 2;

        return 0;
    }

    private double cosineSimilarity(List<Double> a, List<Double> b) {

        double dot = 0;
        double na = 0;
        double nb = 0;

        for (int i = 0; i < a.size(); i++) {
            dot += a.get(i) * b.get(i);
            na += a.get(i) * a.get(i);
            nb += b.get(i) * b.get(i);
        }

        if (na == 0 || nb == 0) return 0;

        return dot / (Math.sqrt(na) * Math.sqrt(nb));
    }

    private int clamp(int v) {
        return Math.max(0, Math.min(100, v));
    }
}
