package com.example.jobautomate.dto;

public record UnifiedJobDto(
    Long id,
    String title,
    String company,
    String location,
    String description,
    Integer experienceRequired,
    String applyUrl,
    String source,
    String publishedAt,
    double similarityScore,
    double finalScore
) {

    public UnifiedJobDto withSimilarityScore(double updatedSimilarityScore) {
        return new UnifiedJobDto(
            id,
            title,
            company,
            location,
            description,
            experienceRequired,
            applyUrl,
            source,
            publishedAt,
            updatedSimilarityScore,
            finalScore
        );
    }

    public UnifiedJobDto withFinalScore(double updatedFinalScore) {
        return new UnifiedJobDto(
            id,
            title,
            company,
            location,
            description,
            experienceRequired,
            applyUrl,
            source,
            publishedAt,
            similarityScore,
            updatedFinalScore
        );
    }

    public UnifiedJobDto withExperienceRequired(Integer updatedExperienceRequired) {
        return new UnifiedJobDto(
            id,
            title,
            company,
            location,
            description,
            updatedExperienceRequired,
            applyUrl,
            source,
            publishedAt,
            similarityScore,
            finalScore
        );
    }
}
