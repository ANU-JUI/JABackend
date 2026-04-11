package com.example.jobautomate.service;

import com.example.jobautomate.config.JobSourceProperties;
import com.example.jobautomate.dto.AggregatedJobSearchRequest;
import com.example.jobautomate.dto.AggregatedJobSearchResponse;
import com.example.jobautomate.dto.JobFeedResponse;
import com.example.jobautomate.dto.JobView;
import com.example.jobautomate.dto.UnifiedJobDto;
import com.example.jobautomate.dto.UserPreferencesRequest;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Mono;

@Service
public class JobAggregationService {

    private static final Logger log = LoggerFactory.getLogger(JobAggregationService.class);

    private final AdzunaService adzunaService;
    private final JSearchService jSearchService;
    private final JoobleService joobleService;
    private final LinkedinService linkedinService;
    private final JobSemanticDataService jobSemanticDataService;
    private final JobParsingService jobParsingService;
    private final HFEmbeddingClient hfEmbeddingClient;
    private final UserSemanticProfileService userSemanticProfileService;
    private final JobSourceProperties properties;

    public JobAggregationService(
        AdzunaService adzunaService,
        JSearchService jSearchService,
        JoobleService joobleService,
        LinkedinService linkedinService,
        JobSemanticDataService jobSemanticDataService,
        JobParsingService jobParsingService,
        HFEmbeddingClient hfEmbeddingClient,
        UserSemanticProfileService userSemanticProfileService,
        JobSourceProperties properties
    ) {
        this.adzunaService = adzunaService;
        this.jSearchService = jSearchService;
        this.joobleService = joobleService;
        this.linkedinService = linkedinService;
        this.jobSemanticDataService = jobSemanticDataService;
        this.jobParsingService = jobParsingService;
        this.hfEmbeddingClient = hfEmbeddingClient;
        this.userSemanticProfileService = userSemanticProfileService;
        this.properties = properties;
    }

    public AggregatedJobSearchResponse fetchJobs(String profileHash, AggregatedJobSearchRequest request) {
        List<String> queries = resolveQueries(request);
        if (queries.isEmpty()) {
            throw new IllegalArgumentException("At least one preferred role, skill, or search query is required");
        }

        Map<String, List<String>> queriesByCountry;
        if (request.countries() == null || request.countries().isEmpty()) {
            queriesByCountry = Map.of("global", queries);
        } else {
            queriesByCountry = request.countries().stream()
                .collect(Collectors.toMap(
                    country -> country,
                    country -> queries,
                    (existing, replacement) -> existing,
                    HashMap::new
                ));
        }

        Mono<List<UnifiedJobDto>> adzunaMono = adzunaService.searchJobs(queriesByCountry);
        Mono<List<UnifiedJobDto>> jsearchMono = jSearchService.searchJobs(queriesByCountry);
        Mono<List<UnifiedJobDto>> joobleMono = joobleService.searchJobs(queriesByCountry);
        Mono<List<UnifiedJobDto>> linkedinMono = linkedinService.searchJobs(queriesByCountry, request);

        List<UnifiedJobDto> merged = Mono.zip(adzunaMono, jsearchMono, joobleMono, linkedinMono)
            .map(tuple -> {
                log.info(
                    "Provider fetch summary - Adzuna: {}, JSearch: {}, Jooble: {}, LinkedIn: {}",
                    tuple.getT1().size(),
                    tuple.getT2().size(),
                    tuple.getT3().size(),
                    tuple.getT4().size()
                );
                List<UnifiedJobDto> allJobs = new ArrayList<>();
                allJobs.addAll(tuple.getT1());
                allJobs.addAll(tuple.getT2());
                allJobs.addAll(tuple.getT3());
                allJobs.addAll(tuple.getT4());
                return allJobs;
            })
            .blockOptional()
            .orElse(List.of());

        int totalFetched = merged.size();
        List<UnifiedJobDto> withExperience = enrichExperience(merged);
        List<UnifiedJobDto> deduplicated = deduplicate(withExperience);
        List<UnifiedJobDto> countryFiltered = deduplicated.stream()
    .filter(job -> isRelevantJob(job, request))
    .filter(job -> matchesCountry(job, request))
    .toList();

List<UnifiedJobDto> relevant;

// ✅ If country jobs exist → use them
if (!countryFiltered.isEmpty()) {
    log.info("Using country-specific jobs: {}", countryFiltered.size());
    relevant = countryFiltered;
} else {
    // 🔥 Fallback to global
    log.warn("No jobs found for selected countries → falling back to global jobs");

    relevant = deduplicated.stream()
        .filter(job -> isRelevantJob(job, request))
        .toList();
}
        List<UnifiedJobDto> scored = scoreJobs(relevant, request);
        double threshold = resolveThreshold(request.similarityThreshold());

        List<UnifiedJobDto> eligible = scored.stream()
            .filter(job -> job.similarityScore() >= threshold)
            .sorted(Comparator.comparingDouble(UnifiedJobDto::finalScore).reversed())
            .toList();
        List<UnifiedJobDto> ranked = eligible.stream()
            .limit(properties.getAggregation().getTopLimit())
            .toList();
        logAggregatedSourceSummary(ranked);

        log.info(
            "Aggregated {} jobs, deduplicated to {}, relevant {}, scored {}, eligible {}, returning {} above threshold {}",
            totalFetched,
            deduplicated.size(),
            relevant.size(),
            scored.size(),
            eligible.size(),
            ranked.size(),
            threshold
        );
        boolean useFallback = countryFiltered.isEmpty();

        AggregatedJobSearchResponse response = new AggregatedJobSearchResponse(
            ranked,
            totalFetched,
            deduplicated.size(),
            ranked.size(),
            threshold,
            useFallback
        );
        return response;
    }
    private Map<String, List<String>> buildQueriesByCountry(AggregatedJobSearchRequest request) {
    List<String> queries = resolveQueries(request);

    return request.countries().stream()
        .collect(Collectors.toMap(
            country -> country,
            country -> queries
        ));
}
    private boolean matchesCountry(UnifiedJobDto job, AggregatedJobSearchRequest request) {

    if (request.countries() == null || request.countries().isEmpty()) {
        return true;
    }

    String jobLocation =
        normalize(job.location());

    return request.countries().stream()
        .anyMatch(country ->
            jobLocation.contains(country.toLowerCase())
        );
}
    private List<UnifiedJobDto> deduplicate(List<UnifiedJobDto> jobs) {
        Map<String, UnifiedJobDto> deduped = new LinkedHashMap<>();
        for (UnifiedJobDto job : jobs) {
            String key = deduplicationKey(job);
            UnifiedJobDto existing = deduped.get(key);
            if (existing == null || shouldReplaceDuplicate(existing, job)) {
                deduped.put(key, job);
            }
        }
        return List.copyOf(deduped.values());
    }

    private List<UnifiedJobDto> scoreJobs(List<UnifiedJobDto> jobs, AggregatedJobSearchRequest request) {
        if (jobs.isEmpty()) {
            return List.of();
        }

        JobSemanticDataService.SemanticUserProfile userProfile =
            jobSemanticDataService.buildUserSemanticProfile(request.toUserPreferences());

        if (userProfile.embedding().isEmpty()) {
            log.warn("User profile embedding is empty; returning unscored live jobs");
            return jobs.stream()
                .map(job -> job.withFinalScore(
                    computeSourceWeight(job) + computeFreshnessBoost(job) - computePenalty(job)
                ))
                .toList();
        }

        List<String> semanticTexts = jobs.stream()
            .map(this::embeddingTextForJob)
            .toList();
        List<List<Double>> jobEmbeddings = hfEmbeddingClient.embedBatch(semanticTexts);

        List<UnifiedJobDto> scored = new ArrayList<>(jobs.size());
        for (int index = 0; index < jobs.size(); index++) {
            UnifiedJobDto job = jobs.get(index);
            List<Double> jobEmbedding = jobEmbeddings.size() > index ? jobEmbeddings.get(index) : List.of();
            double similarity = CosineSimilarity.between(userProfile.embedding(), jobEmbedding);
            double finalScore = similarity
                + computeSourceWeight(job)
                + computeFreshnessBoost(job)
                - computePenalty(job);

            scored.add(job.withSimilarityScore(similarity).withFinalScore(finalScore));
        }
        return scored;
    }

    // Filters out obviously mismatched jobs before semantic scoring.
    private boolean isRelevantJob(UnifiedJobDto job, AggregatedJobSearchRequest request) {
        String normalizedTitle = normalize(job.title());
        if (containsAny(normalizedTitle, "senior", "lead", "manager", "architect")) {
            return false;
        }

        Integer experienceRequired = job.experienceRequired() != null
            ? job.experienceRequired()
            : jobParsingService.extractExperienceYears(job.title(), job.description()).orElse(null);
        if (experienceRequired != null && experienceRequired > 3) {
            return false;
        }

        return !isTechUser(request) || !isNonTechJob(job);
    }

    private double computeFreshnessBoost(UnifiedJobDto job) {
        OffsetDateTime publishedAt = parsePublishedAt(job.publishedAt());
        if (publishedAt == null) {
            return 0.0;
        }

        long ageInDays = ChronoUnit.DAYS.between(publishedAt.toLocalDate(), OffsetDateTime.now().toLocalDate());
        if (ageInDays < 3) {
            return 0.05;
        }
        if (ageInDays <= 7) {
            return 0.02;
        }
        if (ageInDays > 10) {
            return -0.05;
        }
        return 0.0;
    }

    private double computeSourceWeight(UnifiedJobDto job) {
        return switch (normalize(job.source())) {
            case "jsearch" -> 0.05;
            case "jooble" -> 0.02;
            default -> 0.0;
        };
    }

    private double computePenalty(UnifiedJobDto job) {
        String text = normalize(job.title()) + " " + normalize(job.description());
        double penalty = 0.0;

        if (containsAny(text, "senior", "lead")) {
            penalty += 0.2;
        }
        if (text.matches(".*\\b5\\+?\\s*years?\\b.*") || text.matches(".*\\b5\\+?\\s*yrs?\\b.*")) {
            penalty += 0.3;
        }

        return penalty;
    }

    private String embeddingTextForJob(UnifiedJobDto job) {
        String description = StringUtils.hasText(job.description()) ? job.description().trim() : job.title();
        return ("Job title: " + job.title() + ". Company: " + job.company() + ". Location: " + job.location()
            + ". Description: " + description).replaceAll("\\s+", " ").trim();
    }

    private double resolveThreshold(Double threshold) {
        return threshold != null ? threshold : properties.getAggregation().getSimilarityThreshold();
    }

    private String deduplicationKey(UnifiedJobDto job) {
        return normalize(job.title()) + "|" + normalize(job.company()) + "|" + normalize(job.location());
    }

    private boolean shouldReplaceDuplicate(UnifiedJobDto existing, UnifiedJobDto candidate) {
        int existingQuality = duplicateQualityScore(existing);
        int candidateQuality = duplicateQualityScore(candidate);
        if (candidateQuality != existingQuality) {
            return candidateQuality > existingQuality;
        }

        OffsetDateTime existingPublishedAt = parsePublishedAt(existing.publishedAt());
        OffsetDateTime candidatePublishedAt = parsePublishedAt(candidate.publishedAt());
        if (existingPublishedAt == null && candidatePublishedAt != null) {
            return true;
        }
        if (existingPublishedAt != null && candidatePublishedAt == null) {
            return false;
        }
        if (existingPublishedAt != null && candidatePublishedAt != null) {
            return candidatePublishedAt.isAfter(existingPublishedAt);
        }

        return false;
    }

    private List<String> resolveQueries(AggregatedJobSearchRequest request) {
        List<String> explicitQueries = sanitizeQueries(request.searchQueries());
        if (!explicitQueries.isEmpty()) {
            return explicitQueries;
        }

        List<String> roles = sanitizeQueries(request.preferredRoles());
        List<String> skills = sanitizeQueries(request.skills());

        List<String> derivedQueries = Stream.concat(
                roles.stream(),
                roles.stream().flatMap(role -> skills.stream().limit(3).map(skill -> role + " " + skill))
            )
            .distinct()
            .limit(8)
            .toList();

        if (!derivedQueries.isEmpty()) {
            log.info("Derived {} live-search queries from user preferences", derivedQueries.size());
        }

        return derivedQueries;
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

    private String normalize(String value) {
        if (!StringUtils.hasText(value)) {
            return "";
        }
        return value.trim().toLowerCase(Locale.ROOT);
    }

    // Prefer the duplicate with richer fields instead of forcing a provider label.
    private int duplicateQualityScore(UnifiedJobDto job) {
        int score = 0;
        if (StringUtils.hasText(job.description())) {
            score += Math.min(job.description().trim().length(), 400);
        }
        if (StringUtils.hasText(job.applyUrl())) {
            score += 100;
        }
        if (StringUtils.hasText(job.location()) && !"remote".equals(normalize(job.location()))) {
            score += 25;
        }
        if (StringUtils.hasText(job.company()) && !"unknown company".equals(normalize(job.company()))) {
            score += 25;
        }
        return score;
    }

    private boolean isTechUser(AggregatedJobSearchRequest request) {
        String profileText = (
            String.join(" ", request.preferredRoles())
                + " " + String.join(" ", request.skills())
                + " " + String.join(" ", request.preferredJobTypes())
                + " " + (request.resumeSummary() == null ? "" : request.resumeSummary())
        ).toLowerCase(Locale.ROOT);

        return containsAny(
            profileText,
            "java", "spring", "developer", "software", "backend", "frontend", "full stack",
            "engineer", "react", "node", "python", "sql", "devops", "cloud", "api"
        );
    }

    private boolean isNonTechJob(UnifiedJobDto job) {
        String text = normalize(job.title()) + " " + normalize(job.description());
        boolean hasNonTechSignals = containsAny(
            text,
            "sales", "marketing", "accountant", "hr", "human resources", "recruiter", "teacher",
            "customer service", "business development", "operations executive", "telecaller", "finance"
        );
        boolean hasTechSignals = containsAny(
            text,
            "developer", "engineer", "software", "backend", "frontend", "full stack", "java", "spring",
            "react", "python", "sql", "devops", "cloud", "data engineer", "qa", "test automation"
        );
        return hasNonTechSignals && !hasTechSignals;
    }

    private boolean containsAny(String text, String... keywords) {
        for (String keyword : keywords) {
            if (text.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    private OffsetDateTime parsePublishedAt(String publishedAt) {
        if (!StringUtils.hasText(publishedAt)) {
            return null;
        }
        try {
            return OffsetDateTime.parse(publishedAt.trim());
        } catch (RuntimeException exception) {
            return null;
        }
    }

    private JobView toJobView(UnifiedJobDto job) {
        return toJobView(job, null);
    }

    private JobView toJobView(UnifiedJobDto job, UserPreferencesRequest preferences) {
        List<String> requiredSkills = jobParsingService.extractSkills(job.title(), job.description());
        List<String> userSkills = preferences == null || preferences.skills() == null
            ? List.of()
            : preferences.skills().stream()
                .filter(StringUtils::hasText)
                .map(String::trim)
                .toList();
        List<String> missingSkills = requiredSkills.stream()
            .filter(skill -> userSkills.stream().noneMatch(userSkill -> userSkill.equalsIgnoreCase(skill)))
            .toList();

        Integer experienceRequired = job.experienceRequired() != null
            ? job.experienceRequired()
            : jobParsingService.extractExperienceYears(job.title(), job.description()).orElse(null);

        return new JobView(
            job.id(),
            job.title(),
            job.company(),
            job.location(),
            experienceRequired,
            "Source",
            resolveDeadline(job),
            job.applyUrl(),
            requiredSkills,
            (int) Math.round(job.finalScore() * 100),
            missingSkills,
            buildGapMessage(requiredSkills, missingSkills),
            job.source(),
            false
        );
    }

    public AggregatedJobSearchRequest toSearchRequest(UserPreferencesRequest preferences) {
        return new AggregatedJobSearchRequest(
            List.of(),
            preferences.preferredRoles(),
            preferences.preferredJobTypes(),
            preferences.experienceYears(),
            preferences.countries(),
            preferences.skills(),
            preferences.resumeSummary(),
            properties.getAggregation().getSimilarityThreshold()
        );
    }

    public JobFeedResponse toFeedResponse(AggregatedJobSearchResponse response) {
        return toFeedResponse(response, null);
    }

    public JobFeedResponse toFeedResponse(AggregatedJobSearchResponse response, UserPreferencesRequest preferences) {
        List<JobView> availableJobs = response.jobs().stream()
            .map(job -> toJobView(job, preferences))
            .toList();

        return new JobFeedResponse(availableJobs, List.of(), availableJobs.size(), 0 , response.useFallback());
    }

    private LocalDate resolveDeadline(UnifiedJobDto job) {
        OffsetDateTime publishedAt = parsePublishedAt(job.publishedAt());
        LocalDate fallback = publishedAt != null ? publishedAt.toLocalDate().plusDays(14) : LocalDate.now().plusDays(14);
        return jobParsingService.extractDeadline(job.description(), fallback);
    }

    private String buildGapMessage(List<String> requiredSkills, List<String> missingSkills) {
        if (requiredSkills.isEmpty()) {
            return "Required skills inferred from the posting were limited.";
        }
        if (missingSkills.isEmpty()) {
            return "Your profile matches the listed skills well.";
        }
        return "Consider strengthening " + String.join(", ", missingSkills.stream().limit(3).toList()) + ".";
    }

    private void logAggregatedSourceSummary(List<UnifiedJobDto> jobs) {
        Map<String, Long> counts = jobs.stream()
            .collect(Collectors.groupingBy(UnifiedJobDto::source, LinkedHashMap::new, Collectors.counting()));
        String sampleSources = jobs.stream()
            .limit(5)
            .map(UnifiedJobDto::source)
            .collect(Collectors.joining(", "));

        log.info("Final source counts {}", counts);
        log.info("Final source sample {}", sampleSources);
    }

    private List<UnifiedJobDto> enrichExperience(List<UnifiedJobDto> jobs) {
        return jobs.stream()
            .map(job -> {
                if (job.experienceRequired() != null) {
                    return job;
                }
                Integer extracted = jobParsingService.extractExperienceYears(job.title(), job.description()).orElse(null);
                return job.withExperienceRequired(extracted);
            })
            .toList();
    }
}
