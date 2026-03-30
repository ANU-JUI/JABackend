package com.example.jobautomate.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;

@Entity
@Table(name = "user_semantic_profiles")
public class UserSemanticProfile {

    @Id
    @Column(nullable = false, length = 120)
    private String userId;

    @Lob
    @Column(length = 12000)
    private String profileText;

    @Column(length = 64)
    private String profileTextHash;

    @Lob
    @Column(length = 100000)
    private String embeddingVector;

    @Column(nullable = false)
    private OffsetDateTime updatedAt;

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
            : values.stream().map(String::valueOf).collect(java.util.stream.Collectors.joining(","));
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getProfileText() {
        return profileText;
    }

    public void setProfileText(String profileText) {
        this.profileText = profileText;
    }

    public String getProfileTextHash() {
        return profileTextHash;
    }

    public void setProfileTextHash(String profileTextHash) {
        this.profileTextHash = profileTextHash;
    }

    public String getEmbeddingVector() {
        return embeddingVector;
    }

    public void setEmbeddingVector(String embeddingVector) {
        this.embeddingVector = embeddingVector;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(OffsetDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
