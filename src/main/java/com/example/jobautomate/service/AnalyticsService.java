package com.example.jobautomate.service;

import com.example.jobautomate.dto.AnalyticsResponse;
import com.example.jobautomate.dto.JobFeedResponse;
import com.example.jobautomate.dto.SkillInsight;
import com.example.jobautomate.dto.UserPreferencesRequest;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class AnalyticsService {

    private final CachedJobAggregationService cachedJobAggregationService;
    private final JobAggregationService jobAggregationService;
    private final UserSemanticProfileService userSemanticProfileService;

    public AnalyticsService(
        CachedJobAggregationService cachedJobAggregationService,
        JobAggregationService jobAggregationService,
        UserSemanticProfileService userSemanticProfileService
    ) {
        this.cachedJobAggregationService = cachedJobAggregationService;
        this.jobAggregationService = jobAggregationService;
        this.userSemanticProfileService = userSemanticProfileService;
    }

    public AnalyticsResponse buildAnalytics(UserPreferencesRequest preferences) {
        String profileHash = userSemanticProfileService.buildProfileHash(preferences);
        JobFeedResponse feed = jobAggregationService.toFeedResponse(
            cachedJobAggregationService.getJobs(profileHash, jobAggregationService.toSearchRequest(preferences)),
            preferences
        );
        Map<String, Long> gapCounts = feed.availableJobs().stream()
            .flatMap(job -> job.missingSkills().stream())
            .collect(Collectors.groupingBy(skill -> skill, Collectors.counting()));

        List<SkillInsight> insights = gapCounts.entrySet().stream()
            .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
            .limit(5)
            .map(entry -> new SkillInsight(entry.getKey(), entry.getValue()))
            .toList();

        return new AnalyticsResponse(feed.totalAvailableJobs(), feed.totalAppliedJobs(), insights);
    }
}
