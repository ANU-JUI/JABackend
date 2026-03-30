package com.example.jobautomate.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record UserPreferencesRequest(
    @jakarta.validation.constraints.NotBlank String userId,
    @NotEmpty List<String> preferredRoles,
    @NotEmpty List<String> preferredJobTypes,
    @Min(0) int experienceYears,
    @NotEmpty List<String> countries,
    @NotEmpty List<String> skills,
    String resumeSummary
) {
}
