package com.example.jobautomate.service;

import com.example.jobautomate.config.JobSourceProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.net.URI;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

@Service
public class AdzunaJobProvider implements LiveJobProvider {

    private static final Logger log = LoggerFactory.getLogger(AdzunaJobProvider.class);

    private static final Map<String, String> COUNTRY_CODE_MAP = Map.ofEntries(
        Map.entry("Australia", "au"),
        Map.entry("Brazil", "br"),
        Map.entry("Canada", "ca"),
        Map.entry("France", "fr"),
        Map.entry("Germany", "de"),
        Map.entry("India", "in"),
        Map.entry("Italy", "it"),
        Map.entry("Mexico", "mx"),
        Map.entry("Netherlands", "nl"),
        Map.entry("New Zealand", "nz"),
        Map.entry("Poland", "pl"),
        Map.entry("Singapore", "sg"),
        Map.entry("South Africa", "za"),
        Map.entry("United Kingdom", "gb"),
        Map.entry("United States", "us")
    );

    private final RestClient restClient;
    private final JobSourceProperties properties;
    private final JobParsingService jobParsingService;

    public AdzunaJobProvider(JobSourceProperties properties, JobParsingService jobParsingService) {
        this.properties = properties;
        this.jobParsingService = jobParsingService;
        this.restClient = RestClient.builder().build();
    }

    @Override
    public boolean isConfigured() {
        JobSourceProperties.Adzuna adzuna = properties.getAdzuna();
        return StringUtils.hasText(adzuna.getAppId()) && StringUtils.hasText(adzuna.getAppKey());
    }

    @Override
    public List<LiveJobPost> fetchJobs() {
        if (!isConfigured()) {
            return List.of();
        }

        List<LiveJobPost> jobs = new ArrayList<>();
        for (Map.Entry<String, String> entry : COUNTRY_CODE_MAP.entrySet()) {
            for (int page = 1; page <= properties.getAdzuna().getPagesPerCountry(); page++) {
                URI uri = UriComponentsBuilder
                    .fromUriString(properties.getAdzuna().getBaseUrl())
                    .pathSegment(entry.getValue(), "search", String.valueOf(page))
                    .queryParam("app_id", properties.getAdzuna().getAppId())
                    .queryParam("app_key", properties.getAdzuna().getAppKey())
                    .queryParam("results_per_page", properties.getAdzuna().getResultsPerPage())
                    .queryParam("max_days_old", properties.getAdzuna().getMaxDaysOld())
                    .build(true)
                    .toUri();

                AdzunaResponse response = restClient.get()
                    .uri(uri)
                    .retrieve()
                    .body(AdzunaResponse.class);

                if (response == null || response.results() == null) {
                    continue;
                }

                response.results().stream()
                    .map(result -> toLiveJobPost(entry.getKey(), result))
                    .flatMap(Optional::stream)
                    .forEach(jobs::add);
            }
        }

        log.info("Fetched {} live jobs from Adzuna", jobs.size());
        return jobs;
    }

    private Optional<LiveJobPost> toLiveJobPost(String countryName, AdzunaJobResult result) {
        if (!StringUtils.hasText(result.id()) || !StringUtils.hasText(result.title()) || !StringUtils.hasText(result.redirectUrl())) {
            return Optional.empty();
        }

        String description = StringUtils.hasText(result.description()) ? result.description().trim() : result.title();
        String company = result.company() != null && StringUtils.hasText(result.company().displayName())
            ? result.company().displayName()
            : "Unknown company";

        List<String> locationArea = result.location() != null && result.location().area() != null
            ? result.location().area()
            : List.of();
        String city = locationArea.isEmpty() ? "" : locationArea.get(locationArea.size() - 1);
        String state = locationArea.size() >= 2 ? locationArea.get(locationArea.size() - 2) : "";
        String locationLabel = result.location() != null && StringUtils.hasText(result.location().displayName())
            ? result.location().displayName()
            : buildLocationLabel(countryName, state, city);

        Integer experienceYears = jobParsingService.extractExperienceYears(result.title(), description).orElse(null);
        LocalDate fallbackDeadline = LocalDate.now().plusDays(14);

        return Optional.of(new LiveJobPost(
            "adzuna-" + result.id(),
            result.title(),
            company,
            inferJobType(description, result.contractTime(), result.contractType()),
            countryName,
            state,
            city,
            locationLabel,
            experienceYears,
            description,
            result.redirectUrl(),
            fallbackDeadline
        ));
    }

    private String inferJobType(String description, String contractTime, String contractType) {
        if ("part_time".equalsIgnoreCase(contractTime)) {
            return "Part-time";
        }
        if ("contract".equalsIgnoreCase(contractType)) {
            return "Contract";
        }

        String normalized = description.toLowerCase();
        if (normalized.contains("remote")) {
            return "Remote";
        }
        if (normalized.contains("hybrid")) {
            return "Hybrid";
        }
        return "Full-time";
    }

    private String buildLocationLabel(String country, String state, String city) {
        return List.of(city, state, country).stream()
            .filter(StringUtils::hasText)
            .reduce((left, right) -> left + ", " + right)
            .orElse(country);
    }

    public record AdzunaResponse(List<AdzunaJobResult> results) {
    }

    public record AdzunaJobResult(
        String id,
        String title,
        String description,
        @JsonProperty("redirect_url") String redirectUrl,
        @JsonProperty("contract_time") String contractTime,
        @JsonProperty("contract_type") String contractType,
        AdzunaCompany company,
        AdzunaLocation location
    ) {
    }

    public record AdzunaCompany(@JsonProperty("display_name") String displayName) {
    }

    public record AdzunaLocation(@JsonProperty("display_name") String displayName, List<String> area) {
    }
}
