package com.example.jobautomate.service;

import com.example.jobautomate.dto.UnifiedJobDto;
import java.util.List;
import java.util.Map;

import reactor.core.publisher.Mono;

public interface ExternalJobSearchService {

    boolean isConfigured();

Mono<List<UnifiedJobDto>> searchJobs(List<String> searchQueries);
    Mono<List<UnifiedJobDto>> searchJobs(Map<String, List<String>> queriesByCountry);
}
