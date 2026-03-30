package com.example.jobautomate.service;

import java.util.Set;

public record JobProfile(
    Set<String> skills,
    Integer minExperience,
    Integer maxExperience,
    RoleCategory roleCategory,
    QualificationLevel qualificationLevel,
    WorkMode workMode,
    String country,
    String normalizedLocation,
    Set<String> senioritySignals
) {
}
