package com.example.jobautomate.dto;

import java.util.List;

public record JobFeedResponse(
    List<JobView> availableJobs,
    List<JobView> appliedJobs,
    int totalAvailableJobs,
    int totalAppliedJobs
) {
}
