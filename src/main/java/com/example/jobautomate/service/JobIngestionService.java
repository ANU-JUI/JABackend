package com.example.jobautomate.service;

import com.example.jobautomate.config.JobSourceProperties;
import com.example.jobautomate.model.Job;
import com.example.jobautomate.repository.JobRepository;

import jakarta.annotation.PostConstruct;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class JobIngestionService {

    private static final Logger log = LoggerFactory.getLogger(JobIngestionService.class);

    private final JobRepository jobRepository;
    private final JobParsingService jobParsingService;
    private final JobSemanticDataService jobSemanticDataService;
    private final LiveJobProvider liveJobProvider;
    private final JobSourceProperties jobSourceProperties;

    public JobIngestionService(
            JobRepository jobRepository,
            JobParsingService jobParsingService,
            JobSemanticDataService jobSemanticDataService,
            LiveJobProvider liveJobProvider,
            JobSourceProperties jobSourceProperties
    ) {
        this.jobRepository = jobRepository;
        this.jobParsingService = jobParsingService;
        this.jobSemanticDataService = jobSemanticDataService;
        this.liveJobProvider = liveJobProvider;
        this.jobSourceProperties = jobSourceProperties;
    }

    @PostConstruct
    public void initializeJobs() {
        refreshJobs();
    }

    @Scheduled(cron = "${app.scheduler.cron}")
    public void refreshJobs() {

        OffsetDateTime now = OffsetDateTime.now();
        deactivateExpiredJobs();

        List<LiveJobPost> liveJobs = liveJobProvider.fetchJobs();

        if (!liveJobs.isEmpty()) {

            List<Job> entities = liveJobs.stream()
                    .map(source -> mapLiveToEntity(source, now))
                    .toList();

            // ⭐ HF BATCH EMBEDDING
            jobSemanticDataService.enrichJobsWithEmbeddings(entities);

            jobRepository.saveAll(entities);

            log.info("Saved {} live jobs with embeddings", entities.size());
            return;
        }

        if (!jobSourceProperties.isSeedFallbackEnabled()) {
            log.warn("Live job provider empty & fallback disabled");
            return;
        }

        log.warn("Using seed fallback jobs");

        List<SeedJobData.SeedJobPost> seeds = SeedJobData.jobs();

        List<Job> entities = seeds.stream()
                .map(seed -> mapSeedToEntity(seed, now))
                .toList();

        jobSemanticDataService.enrichJobsWithEmbeddings(entities);

        jobRepository.saveAll(entities);

        log.info("Saved {} seed jobs with embeddings", entities.size());
    }

    private Job mapLiveToEntity(LiveJobPost source, OffsetDateTime fetchedAt) {

        Job job = jobRepository.findByExternalId(source.externalId())
                .orElseGet(Job::new);

        JobProfile profile = jobParsingService.extractJobProfile(
                source.title(),
                source.description(),
                source.country(),
                source.locationLabel(),
                source.jobType()
        );

        job.setExternalId(source.externalId());
        job.setTitle(source.title());
        job.setCompany(source.company());
        job.setJobType(source.jobType());
        job.setCountry(profile.country());
        job.setStateName(source.state());
        job.setCity(source.city());
        job.setLocationLabel(profile.normalizedLocation());
        job.setRequiredExperienceYears(profile.minExperience());
        job.setMaxExperienceYears(profile.maxExperience());
        job.setDescription(source.description());
        job.setApplyLink(source.applyLink());
        job.setApplicationDeadline(
                jobParsingService.extractDeadline(
                        source.description(),
                        source.fallbackDeadline()
                )
        );
        job.setExtractedSkillList(profile.skills().stream().toList());
        job.setRoleCategory(profile.roleCategory().name());
        job.setQualificationLevel(profile.qualificationLevel().name());
        job.setDetectedWorkMode(profile.workMode().name());
        job.setSemanticDocument(
                jobSemanticDataService.semanticTextForJob(job)
        );
        job.setActive(true);
        job.setLastFetchedAt(fetchedAt);

        return job;
    }

    private Job mapSeedToEntity(SeedJobData.SeedJobPost seed, OffsetDateTime now) {

        Job job = jobRepository.findByExternalId(seed.externalId())
                .orElseGet(Job::new);

        JobProfile profile = jobParsingService.extractJobProfile(
                seed.title(),
                seed.description(),
                seed.country(),
                buildLocation(seed.country(), seed.state(), seed.city()),
                seed.jobType()
        );

        job.setExternalId(seed.externalId());
        job.setTitle(seed.title());
        job.setCompany(seed.company());
        job.setJobType(seed.jobType());
        job.setCountry(profile.country());
        job.setStateName(seed.state());
        job.setCity(seed.city());
        job.setLocationLabel(profile.normalizedLocation());
        job.setRequiredExperienceYears(
                profile.minExperience() != null
                        ? profile.minExperience()
                        : seed.experienceYears()
        );
        job.setMaxExperienceYears(profile.maxExperience());
        job.setDescription(seed.description());
        job.setApplyLink(seed.applyLink());
        job.setApplicationDeadline(
                jobParsingService.extractDeadline(
                        seed.description(),
                        seed.fallbackDeadline()
                )
        );
        job.setExtractedSkillList(profile.skills().stream().toList());
        job.setRoleCategory(profile.roleCategory().name());
        job.setQualificationLevel(profile.qualificationLevel().name());
        job.setDetectedWorkMode(profile.workMode().name());
        job.setSemanticDocument(
                jobSemanticDataService.semanticTextForJob(job)
        );
        job.setActive(true);
        job.setLastFetchedAt(now);

        return job;
    }

    private void deactivateExpiredJobs() {

        LocalDate today = LocalDate.now();

        jobRepository.findAll().stream()
                .filter(job -> job.getApplicationDeadline() != null
                        && job.getApplicationDeadline().isBefore(today))
                .filter(Job::isActive)
                .forEach(job -> {
                    job.setActive(false);
                    jobRepository.save(job);
                });
    }

    private String buildLocation(String country, String state, String city) {

        if ("Remote".equalsIgnoreCase(country)) {
            return "Remote";
        }

        return List.of(city, state, country).stream()
                .filter(v -> v != null && !v.isBlank())
                .reduce((a, b) -> a + ", " + b)
                .orElse(country);
    }
}