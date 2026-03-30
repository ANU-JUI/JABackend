package com.example.jobautomate.service;

import com.example.jobautomate.dto.UnifiedJobDto;
import java.util.List;
import reactor.core.publisher.Mono;

public interface ExternalJobSearchService {

    boolean isConfigured();

    Mono<List<UnifiedJobDto>> searchJobs(List<String> searchQueries);
}
