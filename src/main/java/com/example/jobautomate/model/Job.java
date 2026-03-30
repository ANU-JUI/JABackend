package com.example.jobautomate.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;

@Entity
@Table(name = "jobs")
public class Job {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String externalId;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String company;

    @Column(nullable = false)
    private String jobType;

    @Column(nullable = false)
    private String country;

    private String stateName;

    private String city;

    @Column(nullable = false)
    private String locationLabel;

    private Integer requiredExperienceYears;

    @Lob
    @Column(nullable = false, length = 8000)
    private String description;

    @Column(nullable = false, length = 1000)
    private String applyLink;

    @Column(nullable = false)
    private LocalDate applicationDeadline;

    @Column(nullable = false, length = 2000)
    private String extractedSkills;

    private Integer maxExperienceYears;

    @Column(length = 40)
    private String roleCategory;

    @Column(length = 40)
    private String qualificationLevel;

    @Column(length = 40)
    private String detectedWorkMode;

    @Lob
    @Column(length = 4000)
    private String semanticSummary;

    @Lob
    @Column(length = 12000)
    private String semanticDocument;

    @Lob
    @Column(length = 100000)
    private String embeddingVector;

    @Column(length = 64)
    private String embeddingSourceHash;

    @Column(nullable = false)
    private boolean active;

    @Column(nullable = false)
    private OffsetDateTime lastFetchedAt;

    public List<String> getExtractedSkillList() {
        if (extractedSkills == null || extractedSkills.isBlank()) {
            return List.of();
        }
        return Arrays.stream(extractedSkills.split(","))
            .map(String::trim)
            .filter(value -> !value.isBlank())
            .toList();
    }

    public void setExtractedSkillList(List<String> skills) {
        this.extractedSkills = String.join(",", skills);
    }

    public List<Double> getEmbeddingValues() {
        if (embeddingVector == null || embeddingVector.isBlank()) {
            return List.of();
        }
        return Arrays.stream(embeddingVector.split(","))
            .map(String::trim)
            .filter(value -> !value.isBlank())
            .map(Double::parseDouble)
            .toList();
    }

    public void setEmbeddingValues(List<Double> values) {
        this.embeddingVector = values == null || values.isEmpty()
            ? ""
            : values.stream().map(value -> Double.toString(value)).reduce((left, right) -> left + "," + right).orElse("");
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getExternalId() {
        return externalId;
    }

    public void setExternalId(String externalId) {
        this.externalId = externalId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getCompany() {
        return company;
    }

    public void setCompany(String company) {
        this.company = company;
    }

    public String getJobType() {
        return jobType;
    }

    public void setJobType(String jobType) {
        this.jobType = jobType;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public String getStateName() {
        return stateName;
    }

    public void setStateName(String stateName) {
        this.stateName = stateName;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getLocationLabel() {
        return locationLabel;
    }

    public void setLocationLabel(String locationLabel) {
        this.locationLabel = locationLabel;
    }

    public Integer getRequiredExperienceYears() {
        return requiredExperienceYears;
    }

    public void setRequiredExperienceYears(Integer requiredExperienceYears) {
        this.requiredExperienceYears = requiredExperienceYears;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getApplyLink() {
        return applyLink;
    }

    public void setApplyLink(String applyLink) {
        this.applyLink = applyLink;
    }

    public LocalDate getApplicationDeadline() {
        return applicationDeadline;
    }

    public void setApplicationDeadline(LocalDate applicationDeadline) {
        this.applicationDeadline = applicationDeadline;
    }

    public String getExtractedSkills() {
        return extractedSkills;
    }

    public void setExtractedSkills(String extractedSkills) {
        this.extractedSkills = extractedSkills;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public Integer getMaxExperienceYears() {
        return maxExperienceYears;
    }

    public void setMaxExperienceYears(Integer maxExperienceYears) {
        this.maxExperienceYears = maxExperienceYears;
    }

    public String getRoleCategory() {
        return roleCategory;
    }

    public void setRoleCategory(String roleCategory) {
        this.roleCategory = roleCategory;
    }

    public String getQualificationLevel() {
        return qualificationLevel;
    }

    public void setQualificationLevel(String qualificationLevel) {
        this.qualificationLevel = qualificationLevel;
    }

    public String getDetectedWorkMode() {
        return detectedWorkMode;
    }

    public void setDetectedWorkMode(String detectedWorkMode) {
        this.detectedWorkMode = detectedWorkMode;
    }

    public String getSemanticSummary() {
        return semanticSummary;
    }

    public void setSemanticSummary(String semanticSummary) {
        this.semanticSummary = semanticSummary;
    }

    public String getSemanticDocument() {
        return semanticDocument;
    }

    public void setSemanticDocument(String semanticDocument) {
        this.semanticDocument = semanticDocument;
    }

    public String getEmbeddingVector() {
        return embeddingVector;
    }

    public void setEmbeddingVector(String embeddingVector) {
        this.embeddingVector = embeddingVector;
    }

    public OffsetDateTime getLastFetchedAt() {
        return lastFetchedAt;
    }

    public void setLastFetchedAt(OffsetDateTime lastFetchedAt) {
        this.lastFetchedAt = lastFetchedAt;
    }

    public String getEmbeddingSourceHash() {
        return embeddingSourceHash;
    }

    public void setEmbeddingSourceHash(String embeddingSourceHash) {
        this.embeddingSourceHash = embeddingSourceHash;
    }
}
