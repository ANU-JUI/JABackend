package com.example.jobautomate.dto;

import java.util.List;

public record AnalyticsResponse(
    int totalAvailableJobs,
    int totalAppliedJobs,
    List<SkillInsight> topMissingSkills
) {
}
