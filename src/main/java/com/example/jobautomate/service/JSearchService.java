package com.example.jobautomate.service;

import com.example.jobautomate.config.JobSourceProperties;
import com.example.jobautomate.dto.UnifiedJobDto;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
public class JSearchService implements ExternalJobSearchService {

    private static final Logger log = LoggerFactory.getLogger(JSearchService.class);

    private final JobSourceProperties properties;
    private final WebClient webClient;

    public JSearchService(JobSourceProperties properties, WebClient.Builder webClientBuilder) {
        this.properties = properties;
        this.webClient = webClientBuilder
            .baseUrl(properties.getJsearch().getBaseUrl())
            .defaultHeader("X-RapidAPI-Host", properties.getJsearch().getRapidApiHost())
            .build();
    }

    @Override
    public boolean isConfigured() {
        JobSourceProperties.JSearch jsearch = properties.getJsearch();
        return StringUtils.hasText(jsearch.getBaseUrl())
            && StringUtils.hasText(jsearch.getRapidApiHost())
            && StringUtils.hasText(jsearch.getRapidApiKey());
    }

    @Override
    public Mono<List<UnifiedJobDto>> searchJobs(List<String> searchQueries) {
        if (!isConfigured()) {
            log.info("JSearch live search skipped because RapidAPI credentials are not configured");
            return Mono.just(List.of());
        }

        List<String> limitedQueries = sanitizeQueries(searchQueries).stream()
            .limit(2)
            .toList();
        AtomicInteger failureCount = new AtomicInteger();

        log.info("JSearch will execute {} sequential calls", limitedQueries.size());

        return Flux.fromIterable(limitedQueries)
            .concatMap(query -> fetchByQueryWithDelay(query, failureCount).flatMapMany(Flux::fromIterable))
            .collectList()
            .doOnSuccess(jobs -> log.info(
                "JSearch fetched {} jobs across {} calls with {} failures",
                jobs.size(),
                limitedQueries.size(),
                failureCount.get()
            ));
    }

    private Mono<List<UnifiedJobDto>> fetchByQueryWithDelay(String query, AtomicInteger failureCount) {
        return Mono.fromCallable(() -> {
                Thread.sleep(400);
                return query;
            })
            .flatMap(delayedQuery -> fetchByQuery(delayedQuery, failureCount));
    }

    private Mono<List<UnifiedJobDto>> fetchByQuery(String query, AtomicInteger failureCount) {
        return webClient.get()
            .uri(uriBuilder -> uriBuilder
                .path("/search")
                .queryParam("query", query)
                .queryParam("page", properties.getJsearch().getPage())
                .queryParam("num_pages", properties.getJsearch().getNumPages())
                .queryParamIfPresent("country", StringUtils.hasText(properties.getJsearch().getCountry())
                    ? java.util.Optional.of(properties.getJsearch().getCountry())
                    : java.util.Optional.empty())
                .build())
            .header("X-RapidAPI-Key", properties.getJsearch().getRapidApiKey())
            .accept(MediaType.APPLICATION_JSON)
            .retrieve()
            .bodyToMono(JSearchResponse.class)
            .timeout(Duration.ofMillis(properties.getJsearch().getTimeoutMs()))
            .map(this::mapResponse)
            .doOnSuccess(jobs -> log.info("JSearch fetched {} jobs for query '{}'", jobs.size(), query))
            .doOnError(exception -> {
                failureCount.incrementAndGet();
                log.warn("JSearch request failed for query '{}': {}", query, exception.getMessage());
            })
            .onErrorReturn(List.of());
    }

    private List<UnifiedJobDto> mapResponse(JSearchResponse response) {
        if (response == null || response.data() == null) {
            return List.of();
        }

        List<UnifiedJobDto> jobs = new ArrayList<>();
        for (JSearchJobResult result : response.data()) {
            if (!StringUtils.hasText(result.jobTitle()) || !StringUtils.hasText(resolveApplyUrl(result))) {
                continue;
            }

            jobs.add(new UnifiedJobDto(
                result.jobTitle().trim(),
                StringUtils.hasText(result.employerName()) ? result.employerName().trim() : "Unknown company",
                buildLocation(result.jobCity(), result.jobState(), result.jobCountry()),
                StringUtils.hasText(result.jobDescription()) ? result.jobDescription().trim() : result.jobTitle().trim(),
                resolveApplyUrl(result),
                "JSearch",
                result.jobPostedAtDatetimeUtc(),
                0.0,
                0.0
            ));
        }
        return jobs;
    }

    private String resolveApplyUrl(JSearchJobResult result) {
        if (StringUtils.hasText(result.jobApplyLink())) {
            return result.jobApplyLink();
        }
        if (result.applyOptions() == null || result.applyOptions().isEmpty()) {
            return "";
        }
        return result.applyOptions().stream()
            .map(JSearchApplyOption::applyLink)
            .filter(StringUtils::hasText)
            .findFirst()
            .orElse("");
    }

    private String buildLocation(String city, String state, String country) {
        return List.of(city, state, country).stream()
            .filter(StringUtils::hasText)
            .map(String::trim)
            .reduce((left, right) -> left + ", " + right)
            .orElse("Remote");
    }

    private List<String> sanitizeQueries(List<String> searchQueries) {
        if (searchQueries == null) {
            return List.of();
        }
        return searchQueries.stream()
            .filter(StringUtils::hasText)
            .map(String::trim)
            .distinct()
            .toList();
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record JSearchResponse(List<JSearchJobResult> data) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record JSearchJobResult(
        @JsonProperty("job_title") String jobTitle,
        @JsonProperty("employer_name") String employerName,
        @JsonProperty("job_city") String jobCity,
        @JsonProperty("job_state") String jobState,
        @JsonProperty("job_country") String jobCountry,
        @JsonProperty("job_description") String jobDescription,
        @JsonProperty("job_apply_link") String jobApplyLink,
        @JsonProperty("job_posted_at_datetime_utc") String jobPostedAtDatetimeUtc,
        @JsonProperty("apply_options") List<JSearchApplyOption> applyOptions
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record JSearchApplyOption(@JsonProperty("apply_link") String applyLink) {
    }
}
