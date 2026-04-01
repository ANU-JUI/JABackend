package com.example.jobautomate.dto;

public record UnifiedJobDto(
    Long id,
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
        return new UnifiedJobDto(id,title, company, location, description, applyUrl, source, publishedAt, updatedSimilarityScore, finalScore);
    }

    public UnifiedJobDto withFinalScore(double updatedFinalScore) {
        return new UnifiedJobDto(id,title, company, location, description, applyUrl, source, publishedAt, similarityScore, updatedFinalScore);
    }
}
