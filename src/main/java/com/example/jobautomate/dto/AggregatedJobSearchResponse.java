package com.example.jobautomate.dto;

import java.util.List;

public record AggregatedJobSearchResponse(
    List<UnifiedJobDto> jobs,
    int totalFetched,
    int totalAfterDeduplication,
    int totalAfterFiltering,
    double similarityThreshold,
    boolean useFallback
) {
}
