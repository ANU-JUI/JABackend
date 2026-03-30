package com.example.jobautomate.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record AggregatedJobSearchRequest(
    List<String> searchQueries,
    @NotEmpty List<String> preferredRoles,
    @NotEmpty List<String> preferredJobTypes,
    @Min(0) int experienceYears,
    @NotEmpty List<String> countries,
    @NotEmpty List<String> skills,
    String resumeSummary,
    @DecimalMin("0.0") @DecimalMax("1.0") Double similarityThreshold
) {

    public UserPreferencesRequest toUserPreferences() {
        return new UserPreferencesRequest(
            "live-search-user",
            preferredRoles,
            preferredJobTypes,
            experienceYears,
            countries,
            skills,
            resumeSummary
        );
    }
}
