package com.example.jobautomate.controller;

import com.example.jobautomate.dto.AnalyticsResponse;
import com.example.jobautomate.dto.AggregatedJobSearchRequest;
import com.example.jobautomate.dto.AggregatedJobSearchResponse;
import com.example.jobautomate.dto.ApplyRequest;
import com.example.jobautomate.dto.ApplyResponse;
import com.example.jobautomate.dto.JobDetailResponse;
import com.example.jobautomate.dto.JobFeedResponse;
import com.example.jobautomate.dto.JobView;
import com.example.jobautomate.dto.UnifiedJobDto;
import com.example.jobautomate.dto.UserPreferencesRequest;
import com.example.jobautomate.model.Job;
import com.example.jobautomate.repository.JobRepository;
import com.example.jobautomate.service.AnalyticsService;
import com.example.jobautomate.service.ApplicationService;
import com.example.jobautomate.service.CachedJobAggregationService;
import com.example.jobautomate.service.JobAggregationService;
import com.example.jobautomate.service.JobIngestionService;
import com.example.jobautomate.service.JobMatchingService;
import com.example.jobautomate.service.UserSemanticProfileService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Optional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class JobController {

    private final JobMatchingService jobMatchingService;
    private final CachedJobAggregationService cachedJobAggregationService;
    private final JobAggregationService jobAggregationService;
    private final UserSemanticProfileService userSemanticProfileService;
    private final AnalyticsService analyticsService;
    private final ApplicationService applicationService;
    private final JobIngestionService jobIngestionService;
    private final JobRepository jobRepository;

    public JobController(
        JobMatchingService jobMatchingService,
        CachedJobAggregationService cachedJobAggregationService,
        JobAggregationService jobAggregationService,
        UserSemanticProfileService userSemanticProfileService,
        AnalyticsService analyticsService,
        ApplicationService applicationService,
        JobIngestionService jobIngestionService, JobRepository jobRepository
    ) {
        this.jobMatchingService = jobMatchingService;
        this.cachedJobAggregationService = cachedJobAggregationService;
        this.jobAggregationService = jobAggregationService;
        this.userSemanticProfileService = userSemanticProfileService;
        this.analyticsService = analyticsService;
        this.applicationService = applicationService;
        this.jobIngestionService = jobIngestionService;
        this.jobRepository = jobRepository;
    }

    @PostMapping("/jobs/feed")
    public JobFeedResponse feed(@Valid @RequestBody UserPreferencesRequest preferences) {
        String profileHash = userSemanticProfileService.buildProfileHash(preferences);
        if (cachedJobAggregationService.hasCachedJobs(profileHash)) {
            org.slf4j.LoggerFactory.getLogger(JobController.class)
                .info("Serving /jobs/feed from Redis for profileHash {}", profileHash);
        }
        AggregatedJobSearchResponse response =
            cachedJobAggregationService.getJobs(profileHash, jobAggregationService.toSearchRequest(preferences));
        return jobAggregationService.toFeedResponse(response, preferences);
    }
    @PostMapping("/jobs/live-search")
    public AggregatedJobSearchResponse liveSearch(@Valid @RequestBody AggregatedJobSearchRequest request) {
        String profileHash = userSemanticProfileService.buildProfileHash(request.toUserPreferences());
        if (cachedJobAggregationService.hasCachedJobs(profileHash)) {
            org.slf4j.LoggerFactory.getLogger(JobController.class)
                .info("Serving /jobs/live-search from Redis for profileHash {}", profileHash);
        }
        return cachedJobAggregationService.getJobs(profileHash, request);
    }

    @PostMapping("/analytics")
    public AnalyticsResponse analytics(@Valid @RequestBody UserPreferencesRequest preferences) {
        return analyticsService.buildAnalytics(preferences);
    }

    @PostMapping("/jobs/{jobId}/apply")
    public ApplyResponse apply(@PathVariable String jobId, @Valid @RequestBody ApplyRequest request) {
        Long resolvedJobId = resolveJobIdForApply(jobId);
        return applicationService.apply(request.userId(), resolvedJobId);
    }

    @PostMapping("/jobs/refresh")
    public void refresh() {
        jobIngestionService.refreshJobs();
    }

    @GetMapping("/jobs/{jobId}")
    public JobDetailResponse detail(
        @PathVariable("jobId") String jobId,
        @RequestParam String userId,
        @RequestParam List<String> preferredRoles,
        @RequestParam List<String> preferredJobTypes,
        @RequestParam int experienceYears,
        @RequestParam List<String> countries,
        @RequestParam List<String> skills
    ) {
        UserPreferencesRequest preferences =
            new UserPreferencesRequest(userId, preferredRoles, preferredJobTypes, experienceYears, countries, skills, null);

        Optional<Job> dbJob = findJob(jobId);
        if (dbJob.isPresent()) {
            return jobMatchingService.getJobDetail(dbJob.get().getId(), preferences);
        }

        String profileHash = userSemanticProfileService.buildProfileHash(preferences);
        AggregatedJobSearchResponse aggregated = cachedJobAggregationService.getJobs(
            profileHash,
            jobAggregationService.toSearchRequest(preferences)
        );

        Optional<UnifiedJobDto> liveJob = aggregated.jobs().stream()
            .filter(job -> job.id() != null && job.id().toString().equals(jobId))
            .findFirst();
        if (liveJob.isEmpty()) {
            throw new IllegalArgumentException("Job not found");
        }

        JobFeedResponse feedResponse = jobAggregationService.toFeedResponse(aggregated, preferences);
        Optional<JobView> matchedView = feedResponse.availableJobs().stream()
            .filter(job -> job.id() != null && job.id().toString().equals(jobId))
            .findFirst();

        UnifiedJobDto unifiedJob = liveJob.get();
        return new JobDetailResponse(
            unifiedJob.id(),
            unifiedJob.title(),
            unifiedJob.company(),
            unifiedJob.location(),
            unifiedJob.experienceRequired() != null
                ? unifiedJob.experienceRequired()
                : matchedView.map(JobView::experienceRequired).orElse(null),
            matchedView.map(JobView::jobType).orElse("Source"),
            matchedView.map(JobView::deadline).orElse(null),
            unifiedJob.applyUrl(),
            unifiedJob.description(),
            matchedView.map(JobView::requiredSkills).orElse(List.of()),
            matchedView.map(JobView::matchScore).orElse((int) Math.round(unifiedJob.finalScore() * 100)),
            matchedView.map(JobView::missingSkills).orElse(List.of()),
            matchedView.map(JobView::gapMessage).orElse(""),
            false
        );
    }

    private Long resolveJobIdForApply(String identifier) {
        if (identifier == null || identifier.isBlank()) {
            throw new IllegalArgumentException("Job id is required");
        }

        return findJob(identifier).map(Job::getId)
            .or(() -> parseLong(identifier))
            .orElseThrow(() -> new IllegalArgumentException("Job not found"));
    }

    private Optional<Job> findJob(String identifier) {
        if (identifier == null || identifier.isBlank()) {
            return Optional.empty();
        }

        return jobRepository.findByExternalId(identifier)
            .or(() -> parseLong(identifier).flatMap(jobRepository::findById));
    }

    private java.util.Optional<Long> parseLong(String value) {
        try {
            return java.util.Optional.of(Long.parseLong(value));
        } catch (NumberFormatException ignored) {
            return java.util.Optional.empty();
        }
    }
}
