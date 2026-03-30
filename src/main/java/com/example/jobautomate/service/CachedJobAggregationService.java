package com.example.jobautomate.service;

import com.example.jobautomate.dto.AggregatedJobSearchRequest;
import com.example.jobautomate.dto.AggregatedJobSearchResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Service
public class CachedJobAggregationService {

    private static final Logger log = LoggerFactory.getLogger(CachedJobAggregationService.class);

    private final JobAggregationService jobAggregationService;
    private final CacheManager cacheManager;

    public CachedJobAggregationService(JobAggregationService jobAggregationService, CacheManager cacheManager) {
        this.jobAggregationService = jobAggregationService;
        this.cacheManager = cacheManager;
    }

    @Cacheable(value = "jobs", key = "'v4:' + #profileHash")
    public AggregatedJobSearchResponse getJobs(String profileHash, AggregatedJobSearchRequest request) {
        log.info("Redis cache miss for profileHash {}; calling job APIs", profileHash);
        AggregatedJobSearchResponse response = jobAggregationService.fetchJobs(profileHash, request);
        if(response.jobs().isEmpty()) {
            log.info("No jobs found for profileHash {}; not caching empty result", profileHash);
            return response;
        }
        log.info("Saved {} jobs in Redis cache for profileHash {} with TTL 1 day", response.jobs().size(), profileHash);
        return response;
    }

    public boolean hasCachedJobs(String profileHash) {
        Cache cache = cacheManager.getCache("jobs");
        return cache != null && cache.get("v4:" + profileHash) != null;
    }
}
