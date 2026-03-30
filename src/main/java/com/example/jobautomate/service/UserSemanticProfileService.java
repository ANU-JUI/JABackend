package com.example.jobautomate.service;

import com.example.jobautomate.dto.UserPreferencesRequest;
import com.example.jobautomate.model.UserSemanticProfile;
import com.example.jobautomate.repository.UserSemanticProfileRepository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Locale;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class UserSemanticProfileService {

    private final UserSemanticProfileRepository repository;
    private final EmbeddingClient embeddingClient;

    public UserSemanticProfileService(
            UserSemanticProfileRepository repository,
            EmbeddingClient embeddingClient
    ) {
        this.repository = repository;
        this.embeddingClient = embeddingClient;
    }

    public SemanticUserProfile getOrCreate(UserPreferencesRequest preferences) {

        String profileText = buildProfileText(preferences);
        String hash = TextHashService.sha256(profileText);

        UserSemanticProfile stored =
                repository.findById(preferences.userId()).orElse(null);

        boolean needsEmbedding =
                stored == null
                        || stored.getEmbeddingValues().isEmpty()
                        || !hash.equals(stored.getProfileTextHash());

        if (stored == null) {
            stored = new UserSemanticProfile();
            stored.setUserId(preferences.userId());
        }

        if (needsEmbedding) {

            List<Double> embedding =
                    embeddingClient.embed(profileText);

            stored.setEmbeddingValues(embedding);
            stored.setProfileTextHash(hash);
            stored.setProfileText(profileText);
            stored.setUpdatedAt(OffsetDateTime.now());

            repository.save(stored);
        }

        return new SemanticUserProfile(
                stored.getProfileText(),
                stored.getProfileTextHash(),
                stored.getEmbeddingValues()
        );
    }

    public String buildProfileHash(UserPreferencesRequest preferences) {
        return TextHashService.sha256(buildProfileText(preferences));
    }

    private String buildProfileText(UserPreferencesRequest preferences) {

        String resume = StringUtils.hasText(preferences.resumeSummary())
                ? preferences.resumeSummary().toLowerCase(Locale.ROOT)
                : "";

        return """
                Candidate role targets: %s.
                Skills: %s.
                Experience: %d years.
                Preferred locations: %s.
                Work preference: %s.
                Resume context: %s.
                """.formatted(
                join(preferences.preferredRoles()),
                join(preferences.skills()),
                preferences.experienceYears(),
                join(preferences.countries()),
                join(preferences.preferredJobTypes()),
                resume
        ).replaceAll("\\s+", " ").trim();
    }
    
    private String join(List<String> list) {

        if (list == null || list.isEmpty()) return "unknown";

        return list.stream()
                .map(v -> v.toLowerCase(Locale.ROOT).trim())
                .distinct()
                .reduce((a, b) -> a + ", " + b)
                .orElse("unknown");
    }

    public record SemanticUserProfile(
            String semanticText,
            String semanticTextHash,
            List<Double> embedding
    ) {
    }
}
