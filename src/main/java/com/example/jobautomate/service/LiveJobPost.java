package com.example.jobautomate.service;

import java.time.LocalDate;

public record LiveJobPost(
    String externalId,
    String title,
    String company,
    String jobType,
    String country,
    String state,
    String city,
    String locationLabel,
    Integer experienceYears,
    String description,
    String applyLink,
    LocalDate fallbackDeadline
) {
}
