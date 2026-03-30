package com.example.jobautomate.dto;

import java.time.LocalDate;
import java.util.List;

public record JobView(
    Long id,
    String title,
    String company,
    String location,
    Integer experienceRequired,
    String jobType,
    LocalDate deadline,
    String applyLink,
    List<String> requiredSkills,
    int matchScore,
    List<String> missingSkills,
    String gapMessage,
    String source,
    boolean applied
) {
}
