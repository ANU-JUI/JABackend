package com.example.jobautomate.service;

import com.example.jobautomate.config.JobSourceProperties;
import com.example.jobautomate.dto.UnifiedJobDto;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
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
public class JoobleService implements ExternalJobSearchService {

    private static final Logger log = LoggerFactory.getLogger(JoobleService.class);

    private final JobSourceProperties properties;
    private final WebClient webClient;

    public JoobleService(JobSourceProperties properties, WebClient.Builder webClientBuilder) {
        this.properties = properties;
        this.webClient = webClientBuilder
            .baseUrl(properties.getJooble().getBaseUrl())
            .build();
    }

    @Override
    public boolean isConfigured() {
        JobSourceProperties.Jooble jooble = properties.getJooble();
        return StringUtils.hasText(jooble.getBaseUrl()) && StringUtils.hasText(jooble.getApiKey());
    }

@Override
    public Mono<List<UnifiedJobDto>> searchJobs(List<String> searchQueries) {
        if (!isConfigured()) {
            log.info("Jooble live search skipped because the API key is not configured");
            return Mono.just(List.of());
        }

        return Flux.fromIterable(sanitizeQueries(searchQueries))
            .flatMap(
                query -> fetchByQuery(query).flatMapMany(Flux::fromIterable),
                properties.getAggregation().getProviderConcurrency()
            )
            .collectList()
            .doOnSuccess(jobs -> log.info(
                "Jooble fetched {} jobs across {} queries",
                jobs.size(),
                sanitizeQueries(searchQueries).size()
            ));
    }
@Override
    public Mono<List<UnifiedJobDto>> searchJobs(Map<String, List<String>> queriesByCountry) {
        if (!isConfigured()) {
            log.info("Jooble live search skipped because the API key is not configured");
            return Mono.just(List.of());
        }

        return Flux.fromIterable(queriesByCountry.entrySet())
            .flatMap(entry -> Flux.fromIterable(entry.getValue())
                .flatMap(query -> fetchByQueryWithCountry(query, entry.getKey())
                    .flatMapMany(Flux::fromIterable)),
                properties.getAggregation().getProviderConcurrency()
            )
            .collectList();
    }

    private Mono<List<UnifiedJobDto>> fetchByQueryWithCountry(String query, String country) {
        String locationCode = StringUtils.hasText(country) ? CountryMapper.toCountryCode(country) : properties.getJooble().getDefaultLocation();
        JoobleRequest request = new JoobleRequest(
            query,
            locationCode,
            properties.getJooble().getPage(),
            properties.getJooble().getResultsPerPage()
        );

        return webClient.post()
            .uri(uriBuilder -> uriBuilder.pathSegment(properties.getJooble().getApiKey()).build())
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(request)
            .retrieve()
            .bodyToMono(JoobleResponse.class)
            .timeout(Duration.ofMillis(properties.getJooble().getTimeoutMs()))
            .map(this::mapResponse)
            .doOnSuccess(jobs -> log.info("Jooble fetched {} jobs for query '{}' location '{}'", jobs.size(), query, locationCode))
            .doOnError(exception -> log.warn("Jooble request failed for query '{}' location '{}': {}", query, locationCode, exception.getMessage()))
            .onErrorReturn(List.of());
    }

    private Mono<List<UnifiedJobDto>> fetchByQuery(String query) {
        return fetchByQueryWithCountry(query, properties.getJooble().getDefaultLocation());
    }

    private List<UnifiedJobDto> mapResponse(JoobleResponse response) {
        if (response == null || response.jobs() == null) {
            return List.of();
        }

        List<UnifiedJobDto> jobs = new ArrayList<>();
        for (JoobleJobResult result : response.jobs()) {
            if (!StringUtils.hasText(result.title()) || !StringUtils.hasText(result.link())) {
                continue;
            }

            jobs.add(new UnifiedJobDto(
                result.title().trim(),
                StringUtils.hasText(result.company()) ? result.company().trim() : "Unknown company",
                StringUtils.hasText(result.location()) ? result.location().trim() : "Remote",
                StringUtils.hasText(result.snippet()) ? result.snippet().trim() : result.title().trim(),
                result.link(),
                "Jooble",
                result.updated(),
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

    public record JoobleRequest(
        String keywords,
        String location,
        int page,
        @JsonProperty("ResultOnPage") int resultOnPage
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record JoobleResponse(List<JoobleJobResult> jobs) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record JoobleJobResult(
        String title,
        String location,
        String snippet,
        String link,
        String company,
        String updated
    ) {
    }
}
