package com.example.jobautomate.service;

import com.example.jobautomate.config.JobSourceProperties;
import com.example.jobautomate.dto.AggregatedJobSearchRequest;
import com.example.jobautomate.dto.UnifiedJobDto;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
public class LinkedinService implements ExternalJobSearchService {

    private static final Logger log = LoggerFactory.getLogger(LinkedinService.class);

    private final JobSourceProperties properties;
    private final WebClient webClient;

    public LinkedinService(JobSourceProperties properties, WebClient.Builder webClientBuilder) {
        this.properties = properties;
        this.webClient = webClientBuilder
            .baseUrl(properties.getLinkedin().getBaseUrl())
            .build();
    }

    @Override
    public boolean isConfigured() {
        JobSourceProperties.Linkedin linkedin = properties.getLinkedin();
        return linkedin.isEnabled() && StringUtils.hasText(linkedin.getBaseUrl());
    }

    @Override
    public Mono<List<UnifiedJobDto>> searchJobs(List<String> searchQueries) {
        if (!isConfigured()) {
            log.info("LinkedIn microservice search skipped because it is disabled or not configured");
            return Mono.just(List.of());
        }

        return Flux.fromIterable(sanitizeQueries(searchQueries))
            .flatMap(query -> fetchByQueryAndLocation(query, properties.getLinkedin().getDefaultLocation()).flatMapMany(Flux::fromIterable))
            .collectList();
    }

    @Override
    public Mono<List<UnifiedJobDto>> searchJobs(Map<String, List<String>> queriesByCountry) {
        return searchJobs(queriesByCountry, null);
    }

    public Mono<List<UnifiedJobDto>> searchJobs(
        Map<String, List<String>> queriesByCountry,
        AggregatedJobSearchRequest request
    ) {
        if (!isConfigured()) {
            log.info("LinkedIn microservice search skipped because it is disabled or not configured");
            return Mono.just(List.of());
        }

        LinkedinQueryOptions dynamicOptions = resolveDynamicOptions(request);
        int maxQueriesPerCountry = Math.max(1, properties.getLinkedin().getMaxQueriesPerCountry());
        int requestConcurrency = Math.max(1, properties.getLinkedin().getRequestConcurrency());

        return Flux.fromIterable(queriesByCountry.entrySet())
            .flatMap(entry -> Flux.fromIterable(sanitizeQueries(entry.getValue()).stream().limit(maxQueriesPerCountry).toList())
                .concatMap(query -> fetchByQueryAndLocation(query, entry.getKey(), dynamicOptions).flatMapMany(Flux::fromIterable)),
                requestConcurrency)
            .collectList();
    }

    private Mono<List<UnifiedJobDto>> fetchByQueryAndLocation(String query, String location) {
        return fetchByQueryAndLocation(query, location, resolveDynamicOptions(null));
    }

    private Mono<List<UnifiedJobDto>> fetchByQueryAndLocation(
        String query,
        String location,
        LinkedinQueryOptions options
    ) {
        JobSourceProperties.Linkedin linkedin = properties.getLinkedin();
        String normalizedLocation = StringUtils.hasText(location) ? location : linkedin.getDefaultLocation();

        return webClient.get()
            .uri(uriBuilder -> {
                var builder = uriBuilder
                    .path(linkedin.getEndpoint())
                    .queryParam("keyword", query)
                    .queryParam("location", normalizedLocation)
                    .queryParam("limit", linkedin.getLimit())
                    .queryParam("page", linkedin.getPage());

                addOptionalQuery(builder, "dateSincePosted", linkedin.getDateSincePosted());
                addOptionalQuery(builder, "jobType", options.jobType());
                addOptionalQuery(builder, "remoteFilter", options.remoteFilter());
                addOptionalQuery(builder, "salary", options.salary());
                addOptionalQuery(builder, "experienceLevel", options.experienceLevel());
                addOptionalQuery(builder, "sortBy", linkedin.getSortBy());
                builder.queryParam("has_verification", linkedin.isHasVerification());
                builder.queryParam("under_10_applicants", linkedin.isUnder10Applicants());
                return builder.build();
            })
            .accept(MediaType.APPLICATION_JSON)
            .retrieve()
            .bodyToMono(LinkedinJobResult[].class)
            .timeout(Duration.ofMillis(linkedin.getTimeoutMs()))
            .map(this::mapResponse)
            .doOnSuccess(jobs -> log.info("LinkedIn microservice fetched {} jobs for query '{}' location '{}'", jobs.size(), query, normalizedLocation))
            .doOnError(exception -> log.warn("LinkedIn microservice request failed for query '{}' location '{}': {}", query, normalizedLocation, exception.getMessage()))
            .onErrorReturn(List.of());
    }

    private List<UnifiedJobDto> mapResponse(LinkedinJobResult[] response) {
        if (response == null || response.length == 0) {
            return List.of();
        }

        List<UnifiedJobDto> jobs = new ArrayList<>();
        for (LinkedinJobResult result : response) {
            if (result == null || !StringUtils.hasText(result.position()) || !StringUtils.hasText(result.jobUrl())) {
                continue;
            }

            String description = buildDescription(result);
            jobs.add(new UnifiedJobDto(
                stableId(result.jobUrl()),
                result.position().trim(),
                StringUtils.hasText(result.company()) ? result.company().trim() : "Unknown company",
                StringUtils.hasText(result.location()) ? result.location().trim() : "Remote",
                description,
                null,
                result.jobUrl(),
                "LinkedIn",
                normalizeDate(result.date()),
                0.0,
                0.0
            ));
        }
        return jobs;
    }

    private String buildDescription(LinkedinJobResult result) {
        StringBuilder value = new StringBuilder();
        value.append("Position: ").append(result.position());
        if (StringUtils.hasText(result.company())) {
            value.append(". Company: ").append(result.company().trim());
        }
        if (StringUtils.hasText(result.location())) {
            value.append(". Location: ").append(result.location().trim());
        }
        if (StringUtils.hasText(result.salary())) {
            value.append(". Salary: ").append(result.salary().trim());
        }
        return value.toString();
    }

    private String normalizeDate(String source) {
        if (!StringUtils.hasText(source)) {
            return null;
        }
        return source.trim();
    }

    private long stableId(String source) {
        return Integer.toUnsignedLong(source.hashCode());
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

    private static void addOptionalQuery(org.springframework.web.util.UriBuilder builder, String key, String value) {
        if (StringUtils.hasText(value)) {
            builder.queryParam(key, value);
        }
    }

    private LinkedinQueryOptions resolveDynamicOptions(AggregatedJobSearchRequest request) {
        JobSourceProperties.Linkedin linkedin = properties.getLinkedin();
        if (request == null) {
            return new LinkedinQueryOptions(
                linkedin.getJobType(),
                linkedin.getRemoteFilter(),
                linkedin.getSalary(),
                linkedin.getExperienceLevel()
            );
        }

        String derivedJobType = deriveLinkedinJobType(request.preferredJobTypes());
        String derivedRemoteFilter = deriveRemoteFilter(request.preferredJobTypes());
        String derivedExperienceLevel = deriveExperienceLevel(request.experienceYears());

        return new LinkedinQueryOptions(
            StringUtils.hasText(derivedJobType) ? derivedJobType : linkedin.getJobType(),
            StringUtils.hasText(derivedRemoteFilter) ? derivedRemoteFilter : linkedin.getRemoteFilter(),
            linkedin.getSalary(),
            StringUtils.hasText(derivedExperienceLevel) ? derivedExperienceLevel : linkedin.getExperienceLevel()
        );
    }

    private String deriveLinkedinJobType(List<String> preferredJobTypes) {
        if (preferredJobTypes == null) {
            return "";
        }
        List<String> normalized = preferredJobTypes.stream()
            .filter(StringUtils::hasText)
            .map(value -> value.trim().toLowerCase(Locale.ROOT))
            .toList();

        if (normalized.stream().anyMatch(value -> value.contains("full"))) {
            return "full time";
        }
        if (normalized.stream().anyMatch(value -> value.contains("part"))) {
            return "part time";
        }
        if (normalized.stream().anyMatch(value -> value.contains("contract"))) {
            return "contract";
        }
        if (normalized.stream().anyMatch(value -> value.contains("intern"))) {
            return "internship";
        }
        if (normalized.stream().anyMatch(value -> value.contains("temporary"))) {
            return "temporary";
        }
        return "";
    }

    private String deriveRemoteFilter(List<String> preferredJobTypes) {
        if (preferredJobTypes == null) {
            return "";
        }
        List<String> normalized = preferredJobTypes.stream()
            .filter(StringUtils::hasText)
            .map(value -> value.trim().toLowerCase(Locale.ROOT))
            .toList();

        if (normalized.stream().anyMatch(value -> value.contains("remote"))) {
            return "remote";
        }
        if (normalized.stream().anyMatch(value -> value.contains("hybrid"))) {
            return "hybrid";
        }
        if (normalized.stream().anyMatch(value -> value.contains("onsite") || value.contains("on-site"))) {
            return "on-site";
        }
        return "";
    }

    private String deriveExperienceLevel(int experienceYears) {
        if (experienceYears <= 1) {
            return "entry level";
        }
        if (experienceYears <= 3) {
            return "associate";
        }
        if (experienceYears <= 6) {
            return "mid senior";
        }
        return "director";
    }

    private record LinkedinQueryOptions(
        String jobType,
        String remoteFilter,
        String salary,
        String experienceLevel
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record LinkedinJobResult(
        String position,
        String company,
        String location,
        String date,
        String salary,
        String jobUrl
    ) {
    }
}
