package com.example.jobautomate.service;

import com.example.jobautomate.config.JobSourceProperties;
import com.example.jobautomate.dto.UnifiedJobDto;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import com.example.jobautomate.service.CountryMapper;
import org.springframework.web.reactive.function.client.WebClient;
import java.util.Map;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
public class AdzunaService implements ExternalJobSearchService {

    private static final Logger log = LoggerFactory.getLogger(AdzunaService.class);

    private final JobSourceProperties properties;
    private final WebClient webClient;

    public AdzunaService(JobSourceProperties properties, WebClient.Builder webClientBuilder) {
        this.properties = properties;
        this.webClient = webClientBuilder
            .baseUrl(properties.getAdzuna().getBaseUrl())
            .build();
    }

    @Override
    public boolean isConfigured() {
        JobSourceProperties.Adzuna adzuna = properties.getAdzuna();
        return StringUtils.hasText(adzuna.getAppId())
            && StringUtils.hasText(adzuna.getAppKey())
            && StringUtils.hasText(adzuna.getBaseUrl());
    }

@Override
    public Mono<List<UnifiedJobDto>> searchJobs(List<String> searchQueries) {
        if (!isConfigured()) {
            log.info("Adzuna live search skipped because credentials are not configured");
            return Mono.just(List.of());
        }

        List<String> countries = properties.getAdzuna().getSearchCountries();
        return Flux.fromIterable(sanitizeQueries(searchQueries))
            .flatMap(query -> Flux.fromIterable(countries)
                .flatMap(
                    country -> fetchByQuery(query, country).flatMapMany(Flux::fromIterable),
                    properties.getAggregation().getProviderConcurrency()
                ))
            .collectList();
    }
    @Override
    public Mono<List<UnifiedJobDto>> searchJobs(Map<String, List<String>> queriesByCountry) {
        if (!isConfigured()) {
            log.info("Adzuna live search skipped because credentials are not configured");
            return Mono.just(List.of());
        }

      return Flux.fromIterable(queriesByCountry.entrySet())
    .flatMap(entry -> 
        Flux.fromIterable(entry.getValue())
            .flatMap(query -> {
                List<String> countries = entry.getKey().isEmpty()
                    ? properties.getAdzuna().getSearchCountries()
                    : List.of(CountryMapper.getCode(entry.getKey()));

                return Flux.fromIterable(countries)
                    .flatMap(countryCode -> 
                        fetchByQuery(query, countryCode)
                            .flatMapMany(Flux::fromIterable)
                    );
            }),
        properties.getAggregation().getProviderConcurrency()
    )
    .collectList();
}

    private Mono<List<UnifiedJobDto>> fetchByQuery(String query, String countryCode) {
        return webClient.get()
            .uri(uriBuilder -> uriBuilder
                .pathSegment(countryCode, "search", "1")
                .queryParam("app_id", properties.getAdzuna().getAppId())
                .queryParam("app_key", properties.getAdzuna().getAppKey())
                .queryParam("results_per_page", properties.getAdzuna().getResultsPerPage())
                .queryParam("max_days_old", properties.getAdzuna().getMaxDaysOld())
                .queryParam("what", query)
                .build())
            .accept(MediaType.APPLICATION_JSON)
            .retrieve()
            .bodyToMono(AdzunaResponse.class)
            .timeout(Duration.ofSeconds(30))
            .map(response -> mapResponse(response, countryCode))
            .doOnError(exception -> log.warn("Adzuna request failed for query '{}': {}", query, exception.getMessage()))
            .onErrorReturn(List.of());
    }

    private List<UnifiedJobDto> mapResponse(AdzunaResponse response, String countryCode) {
        if (response == null || response.results() == null) {
            return List.of();
        }

        List<UnifiedJobDto> jobs = new ArrayList<>();
        for (AdzunaJobResult result : response.results()) {
            if (!StringUtils.hasText(result.title()) || !StringUtils.hasText(result.redirectUrl())) {
                continue;
            }

            String company = result.company() != null && StringUtils.hasText(result.company().displayName())
                ? result.company().displayName()
                : "Unknown company";
            String location = result.location() != null && StringUtils.hasText(result.location().displayName())
                ? result.location().displayName()
                : countryCode.toUpperCase();
            String description = StringUtils.hasText(result.description()) ? result.description().trim() : result.title();

            jobs.add(new UnifiedJobDto(
                result.id(),
                result.title().trim(),
                company,
                location,
                description,
                result.redirectUrl(),
                "Adzuna",
                result.created(),
                0.0,
                0.0
            ));
        }
        return jobs;
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

    public record AdzunaResponse(List<AdzunaJobResult> results) {
    }

    public record AdzunaJobResult(
        String title,
        String description,
        @JsonProperty("redirect_url") String redirectUrl,
        String created,
        AdzunaCompany company,
        AdzunaLocation location
    ) {
    }

    public record AdzunaCompany(@JsonProperty("display_name") String displayName) {
    }

    public record AdzunaLocation(@JsonProperty("display_name") String displayName) {
    }
}
