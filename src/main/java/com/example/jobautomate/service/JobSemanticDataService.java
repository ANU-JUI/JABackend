package com.example.jobautomate.service;

import com.example.jobautomate.dto.UserPreferencesRequest;
import com.example.jobautomate.model.Job;
import java.util.LinkedHashSet;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class JobSemanticDataService {

    private final JobParsingService jobParsingService;
    private final HFEmbeddingClient embeddingClient;

    public JobSemanticDataService(
            JobParsingService jobParsingService,
            HFEmbeddingClient embeddingClient
    ) {
        this.jobParsingService = jobParsingService;
        this.embeddingClient = embeddingClient;
    }

    // ⭐ SINGLE JOB ENRICH (only use when really needed)
    public JobSemanticData enrichJob(
            String title,
            String description,
            String country,
            String locationLabel,
            String jobType
    ) {

        JobProfile profile = jobParsingService.extractJobProfile(
                title, description, country, locationLabel, jobType
        );

        String semanticDocument = buildSemanticDocumentText(
                title,
                jobType,
                profile,
                description
        );

        List<List<Double>> vec = embeddingClient.embedBatch(List.of(semanticDocument));
        List<Double> embedding = vec.isEmpty() ? List.of() : vec.get(0);

        return new JobSemanticData(profile, embedding, semanticDocument);
    }

    // ⭐ IMPORTANT → THIS WILL BE USED IN INGESTION (BATCH)
    public void enrichJobsWithEmbeddings(List<Job> jobs) {

        List<String> semanticDocs = jobs.stream()
                .map(this::semanticTextForJob)
                .toList();

        List<List<Double>> vectors = embeddingClient.embedBatch(semanticDocs);

        for (int i = 0; i < jobs.size(); i++) {
            if (vectors.size() > i) {
                jobs.get(i).setEmbeddingValues(vectors.get(i));
            }
        }
    }

    public JobProfile resolveJobProfile(Job job) {

        JobProfile fallback = jobParsingService.extractJobProfile(
                job.getTitle(),
                job.getDescription(),
                job.getCountry(),
                job.getLocationLabel(),
                job.getJobType()
        );

        return new JobProfile(
                job.getExtractedSkillList().isEmpty()
                        ? fallback.skills()
                        : new LinkedHashSet<>(job.getExtractedSkillList()),
                job.getRequiredExperienceYears() != null
                        ? job.getRequiredExperienceYears()
                        : fallback.minExperience(),
                job.getMaxExperienceYears() != null
                        ? job.getMaxExperienceYears()
                        : fallback.maxExperience(),
                fallback.roleCategory(),
                fallback.qualificationLevel(),
                fallback.workMode(),
                StringUtils.hasText(job.getCountry())
                        ? job.getCountry()
                        : fallback.country(),
                StringUtils.hasText(job.getLocationLabel())
                        ? job.getLocationLabel()
                        : fallback.normalizedLocation(),
                fallback.senioritySignals()
        );
    }

    // ⭐ USER PROFILE EMBEDDING
    public SemanticUserProfile buildUserSemanticProfile(
            UserPreferencesRequest preferences
    ) {

        String semanticText = """
                Candidate roles: %s.
                Experience: %d years.
                Preferred countries: %s.
                Skills: %s.
                """.formatted(
                String.join(", ", preferences.preferredRoles()),
                preferences.experienceYears(),
                String.join(", ", preferences.countries()),
                String.join(", ", preferences.skills())
        ).replaceAll("\\s+", " ").trim();

        List<List<Double>> vec = embeddingClient.embedBatch(List.of(semanticText));
        List<Double> embedding = vec.isEmpty() ? List.of() : vec.get(0);

        return new SemanticUserProfile(semanticText, embedding);
    }

    public String semanticTextForJob(Job job) {

        if (StringUtils.hasText(job.getSemanticDocument())) {
            return job.getSemanticDocument();
        }

        JobProfile profile = resolveJobProfile(job);

        return buildSemanticDocumentText(
                job.getTitle(),
                job.getJobType(),
                profile,
                job.getDescription()
        );
    }

    private String buildSemanticDocumentText(
            String title,
            String jobType,
            JobProfile profile,
            String description
    ) {

        String cleanDesc = description == null
                ? ""
                : description.replaceAll("\\s+", " ").trim();

        if (cleanDesc.length() > 800) {
            cleanDesc = cleanDesc.substring(0, 800);
        }

        return """
                Job title: %s.
                Job type: %s.
                Role category: %s.
                Work mode: %s.
                Min experience: %s years.
                Location: %s.
                Country: %s.
                Required skills: %s.
                Description: %s
                """.formatted(
                safe(title),
                safe(jobType),
                profile.roleCategory().name(),
                profile.workMode().name(),
                profile.minExperience() == null
                        ? "unknown"
                        : profile.minExperience(),
                safe(profile.normalizedLocation()),
                safe(profile.country()),
                profile.skills().isEmpty()
                        ? "unknown"
                        : String.join(", ", profile.skills()),
                cleanDesc
        ).replaceAll("\\s+", " ").trim();
    }

    private String safe(String v) {
        return StringUtils.hasText(v) ? v.trim() : "unknown";
    }

    public record JobSemanticData(
            JobProfile profile,
            List<Double> embedding,
            String semanticDocument
    ) {}

    public record SemanticUserProfile(
            String semanticText,
            List<Double> embedding
    ) {}
}