package com.example.jobautomate.dto;

public record UnifiedJobDto(
    String title,
    String company,
    String location,
    String description,
    String applyUrl,
    String source,
    String publishedAt,
    double similarityScore,
    double finalScore
) {

    public UnifiedJobDto withSimilarityScore(double updatedSimilarityScore) {
        return new UnifiedJobDto(title, company, location, description, applyUrl, source, publishedAt, updatedSimilarityScore, finalScore);
    }

    public UnifiedJobDto withFinalScore(double updatedFinalScore) {
        return new UnifiedJobDto(title, company, location, description, applyUrl, source, publishedAt, similarityScore, updatedFinalScore);
    }
}
