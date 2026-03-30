package com.example.jobautomate.service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class JobParsingService {

    private static final Pattern DEADLINE_PATTERN =
        Pattern.compile("(?:apply by|deadline|last date to apply)[:\\s]+([A-Za-z]{3,9}\\s+\\d{1,2},\\s+\\d{4}|\\d{4}-\\d{2}-\\d{2})",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern BETWEEN_EXPERIENCE_PATTERN =
        Pattern.compile("\\bbetween\\s*(\\d{1,2})\\s*(?:and|to)\\s*(\\d{1,2})\\s*(?:years|yrs)\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern RANGE_EXPERIENCE_PATTERN =
        Pattern.compile("\\b(\\d{1,2})\\s*(?:-|to|–|—)\\s*(\\d{1,2})\\s*(?:years|yrs)\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern PLUS_EXPERIENCE_PATTERN =
        Pattern.compile("\\b(\\d{1,2})\\s*\\+\\s*(?:years|yrs)\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern MINIMUM_EXPERIENCE_PATTERN =
        Pattern.compile("\\b(?:minimum of|minimum|min|at least|over|more than)\\s*(\\d{1,2})\\s*(?:years|yrs)\\b",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern SIMPLE_EXPERIENCE_PATTERN =
        Pattern.compile("\\b(\\d{1,2})\\s*(?:years|yrs)\\s*(?:of\\s+)?(?:[a-z]+\\s+){0,4}?(?:experience|exp|required|preferred)\\b",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern BACHELOR_PATTERN =
        Pattern.compile("\\b(?:bachelor(?:'s)?|graduate|b\\.?tech|b\\.?e|bsc|bca)\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern MASTER_PATTERN =
        Pattern.compile("\\b(?:master(?:'s)?|m\\.?tech|m\\.?e|msc|mca|mba)\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern PHD_PATTERN =
        Pattern.compile("\\b(?:phd|doctorate)\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern DIPLOMA_PATTERN =
        Pattern.compile("\\b(?:diploma|polytechnic)\\b", Pattern.CASE_INSENSITIVE);

    private final SkillCatalog skillCatalog;
    private final Map<String, Integer> textNumbers;
    private final Map<String, Set<String>> normalizedSkillAliases;

    public JobParsingService(SkillCatalog skillCatalog) {
        this.skillCatalog = skillCatalog;
        this.textNumbers = buildTextNumbers();
        this.normalizedSkillAliases = buildNormalizedSkillAliases(skillCatalog.skillAliases());
    }

    public List<String> extractSkills(String title, String description) {
        String normalized = normalizeForSkillMatching(title + " " + description);
        List<String> tokens = normalized.isBlank() ? List.of() : List.of(normalized.split("\\s+"));
        Set<String> matches = new LinkedHashSet<>();

        for (int size = 4; size >= 1; size--) {
            for (int i = 0; i + size <= tokens.size(); i++) {
                String candidate = String.join(" ", tokens.subList(i, i + size));
                Set<String> canonicalSkills = normalizedSkillAliases.get(candidate);
                if (canonicalSkills != null) {
                    matches.addAll(canonicalSkills);
                }
            }
        }

        return matches.stream().sorted(Comparator.naturalOrder()).toList();
    }

    public LocalDate extractDeadline(String description, LocalDate fallback) {
        Matcher matcher = DEADLINE_PATTERN.matcher(description);
        if (!matcher.find()) {
            return fallback;
        }

        String raw = matcher.group(1).trim();
        for (DateTimeFormatter formatter : List.of(
            DateTimeFormatter.ofPattern("MMMM d, uuuu", Locale.ENGLISH),
            DateTimeFormatter.ofPattern("MMM d, uuuu", Locale.ENGLISH),
            DateTimeFormatter.ISO_LOCAL_DATE
        )) {
            try {
                return LocalDate.parse(raw, formatter);
            } catch (DateTimeParseException ignored) {
            }
        }
        return fallback;
    }

    public Optional<Integer> extractExperienceYears(String title, String description) {
        String combined = normalizeForExperienceExtraction(title + " " + description);
        List<Integer> candidates = new ArrayList<>();

        collectExperienceMatches(candidates, BETWEEN_EXPERIENCE_PATTERN, combined, 1);
        collectExperienceMatches(candidates, RANGE_EXPERIENCE_PATTERN, combined, 1);
        collectExperienceMatches(candidates, PLUS_EXPERIENCE_PATTERN, combined, 1);
        collectExperienceMatches(candidates, MINIMUM_EXPERIENCE_PATTERN, combined, 1);
        collectExperienceMatches(candidates, SIMPLE_EXPERIENCE_PATTERN, combined, 1);

        return candidates.stream()
            .filter(value -> value >= 0 && value <= 25)
            .min(Integer::compareTo)
            .or(() -> inferExperienceFromTitle(title));
    }

    public JobProfile extractJobProfile(String title, String description, String country, String locationLabel, String jobType) {
        String safeDescription = description == null ? "" : description;
        String normalizedLocation = normalizeLocation(locationLabel, country);
        return new JobProfile(
            new LinkedHashSet<>(extractSkills(title, safeDescription)),
            extractExperienceYears(title, safeDescription).orElse(null),
            extractExperienceUpperBound(safeDescription).orElse(null),
            inferRoleCategory(title, safeDescription),
            extractQualificationLevel(safeDescription),
            inferWorkMode(jobType, safeDescription, locationLabel),
            normalizeCountry(country, locationLabel, jobType, safeDescription),
            normalizedLocation,
            inferSenioritySignals(title, safeDescription)
        );
    }

    public RoleCategory inferRoleCategory(String title, String description) {
        String normalizedTitle = normalizeForSkillMatching(title);
        String normalizedDescription = normalizeForSkillMatching(description);

        RoleCategory bestCategory = RoleCategory.UNKNOWN;
        int bestScore = 0;
        for (Map.Entry<RoleCategory, Set<String>> entry : skillCatalog.roleSignals().entrySet()) {
            int score = 0;
            for (String signal : entry.getValue()) {
                String normalizedSignal = normalizeForSkillMatching(signal);
                if (containsWholeTerm(normalizedTitle, normalizedSignal)) {
                    score += normalizedSignal.contains(" ") ? 28 : 20;
                } else if (containsWholeTerm(normalizedDescription, normalizedSignal)) {
                    score += normalizedSignal.contains(" ") ? 14 : 9;
                }
            }

            if (score > bestScore) {
                bestScore = score;
                bestCategory = entry.getKey();
            }
        }

        return bestScore >= 18 ? bestCategory : RoleCategory.UNKNOWN;
    }

    public QualificationLevel extractQualificationLevel(String description) {
        if (!StringUtils.hasText(description)) {
            return QualificationLevel.UNKNOWN;
        }
        if (PHD_PATTERN.matcher(description).find()) {
            return QualificationLevel.PHD;
        }
        if (MASTER_PATTERN.matcher(description).find()) {
            return QualificationLevel.MASTER;
        }
        if (BACHELOR_PATTERN.matcher(description).find()) {
            return QualificationLevel.BACHELOR;
        }
        if (DIPLOMA_PATTERN.matcher(description).find()) {
            return QualificationLevel.DIPLOMA;
        }
        if (description.toLowerCase(Locale.ROOT).contains("any graduate")) {
            return QualificationLevel.ANY;
        }
        return QualificationLevel.UNKNOWN;
    }

    public WorkMode inferWorkMode(String jobType, String description, String locationLabel) {
        String normalized = normalizeForSkillMatching(String.join(" ",
            jobType == null ? "" : jobType,
            description == null ? "" : description,
            locationLabel == null ? "" : locationLabel
        ));
        if (containsWholeTerm(normalized, "hybrid")) {
            return WorkMode.HYBRID;
        }
        if (containsWholeTerm(normalized, "remote") || containsWholeTerm(normalized, "work from home")) {
            return WorkMode.REMOTE;
        }
        if (StringUtils.hasText(normalized)) {
            return WorkMode.ONSITE;
        }
        return WorkMode.UNKNOWN;
    }

    private void collectExperienceMatches(List<Integer> candidates, Pattern pattern, String source, int groupIndex) {
        Matcher matcher = pattern.matcher(source);
        while (matcher.find()) {
            candidates.add(Integer.parseInt(matcher.group(groupIndex)));
        }
    }

    private Optional<Integer> extractExperienceUpperBound(String description) {
        String normalized = normalizeForExperienceExtraction(description);
        Matcher betweenMatcher = BETWEEN_EXPERIENCE_PATTERN.matcher(normalized);
        if (betweenMatcher.find()) {
            return Optional.of(Integer.parseInt(betweenMatcher.group(2)));
        }

        Matcher rangeMatcher = RANGE_EXPERIENCE_PATTERN.matcher(normalized);
        if (rangeMatcher.find()) {
            return Optional.of(Integer.parseInt(rangeMatcher.group(2)));
        }
        return Optional.empty();
    }

    private boolean containsWholeTerm(String text, String term) {
        if (!StringUtils.hasText(term)) {
            return false;
        }

        Pattern pattern = Pattern.compile("(?<![a-z0-9])" + Pattern.quote(term) + "(?![a-z0-9])");
        return pattern.matcher(text).find();
    }

    private String normalizeForSkillMatching(String source) {
        return source.toLowerCase(Locale.ROOT)
            .replace("ci/cd", "ci cd")
            .replace('/', ' ')
            .replace('-', ' ')
            .replace('_', ' ')
            .replaceAll("[^a-z0-9+#.\\s]", " ")
            .replaceAll("\\s+", " ")
            .trim();
    }

    private String normalizeForExperienceExtraction(String source) {
        String normalized = source.toLowerCase(Locale.ROOT)
            .replace('–', '-')
            .replace('—', '-')
            .replaceAll("[,()]", " ")
            .replaceAll("\\s+", " ")
            .trim();

        for (Map.Entry<String, Integer> entry : textNumbers.entrySet()) {
            normalized = normalized.replaceAll("\\b" + Pattern.quote(entry.getKey()) + "\\b", String.valueOf(entry.getValue()));
        }
        return normalized;
    }

    private Map<String, Integer> buildTextNumbers() {
        Map<String, Integer> values = new LinkedHashMap<>();
        values.put("zero", 0);
        values.put("one", 1);
        values.put("two", 2);
        values.put("three", 3);
        values.put("four", 4);
        values.put("five", 5);
        values.put("six", 6);
        values.put("seven", 7);
        values.put("eight", 8);
        values.put("nine", 9);
        values.put("ten", 10);
        values.put("eleven", 11);
        values.put("twelve", 12);
        values.put("thirteen", 13);
        values.put("fourteen", 14);
        values.put("fifteen", 15);
        values.put("sixteen", 16);
        values.put("seventeen", 17);
        values.put("eighteen", 18);
        values.put("nineteen", 19);
        values.put("twenty", 20);
        return values;
    }

    private Map<String, Set<String>> buildNormalizedSkillAliases(Map<String, Set<String>> aliases) {
        Map<String, Set<String>> normalized = new LinkedHashMap<>();
        for (Map.Entry<String, Set<String>> entry : aliases.entrySet()) {
            for (String alias : entry.getValue()) {
                normalized.computeIfAbsent(normalizeForSkillMatching(alias), ignored -> new LinkedHashSet<>())
                    .add(entry.getKey());
            }
        }
        return normalized;
    }

    private Set<String> inferSenioritySignals(String title, String description) {
        String normalized = normalizeForSkillMatching(title + " " + description);
        Set<String> signals = new LinkedHashSet<>();
        for (String signal : List.of("intern", "fresher", "junior", "senior", "lead", "staff", "principal", "architect")) {
            if (containsWholeTerm(normalized, signal)) {
                signals.add(signal);
            }
        }
        return signals;
    }

    private String normalizeCountry(String country, String locationLabel, String jobType, String description) {
        if (inferWorkMode(jobType, description, locationLabel) == WorkMode.REMOTE) {
            return "Remote";
        }
        return StringUtils.hasText(country) ? country.trim() : "Unknown";
    }

    private String normalizeLocation(String locationLabel, String country) {
        if (StringUtils.hasText(locationLabel)) {
            return locationLabel.trim();
        }
        return StringUtils.hasText(country) ? country.trim() : "Unknown";
    }

    private Optional<Integer> inferExperienceFromTitle(String title) {
        String normalized = title.toLowerCase(Locale.ROOT);
        if (normalized.contains("intern") || normalized.contains("fresher") || normalized.contains("entry level")) {
            return Optional.of(0);
        }
        if (normalized.contains("junior") || normalized.contains("associate")) {
            return Optional.of(1);
        }
        if (normalized.contains("mid") || normalized.contains("intermediate")) {
            return Optional.of(3);
        }
        if (normalized.contains("senior")) {
            return Optional.of(5);
        }
        if (normalized.contains("lead") || normalized.contains("staff")) {
            return Optional.of(7);
        }
        if (normalized.contains("principal") || normalized.contains("architect") || normalized.contains("head")) {
            return Optional.of(9);
        }
        return Optional.empty();
    }

}
