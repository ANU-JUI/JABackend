package com.example.jobautomate.service;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class SkillCatalog {

    private final Map<String, Set<String>> skillAliases = buildSkillAliases();
    private final Map<RoleCategory, Set<String>> roleSignals = buildRoleSignals();

    public Set<String> knownSkills() {
        return new LinkedHashSet<>(skillAliases.keySet());
    }

    public Set<String> seniorOnlySkills() {
        return Set.of("Kafka", "Kubernetes", "Microservices", "Airflow", "Spark", "Terraform", "AWS", "GCP", "Azure");
    }

    public Map<String, Set<String>> skillAliases() {
        return skillAliases;
    }

    public Map<RoleCategory, Set<String>> roleSignals() {
        return roleSignals;
    }

    private Map<String, Set<String>> buildSkillAliases() {
        Map<String, Set<String>> aliases = new LinkedHashMap<>();
        aliases.put("Java", Set.of("java", "core java", "java 8", "java 11", "java 17", "j2ee"));
        aliases.put("Spring Boot", Set.of("spring boot", "springboot", "spring framework", "spring mvc"));
        aliases.put("Hibernate", Set.of("hibernate", "jpa"));
        aliases.put("SQL", Set.of("sql", "queries"));
        aliases.put("MySQL", Set.of("mysql"));
        aliases.put("PostgreSQL", Set.of("postgresql", "postgres"));
        aliases.put("React", Set.of("react", "react.js", "reactjs"));
        aliases.put("JavaScript", Set.of("javascript", "ecmascript"));
        aliases.put("TypeScript", Set.of("typescript"));
        aliases.put("Node.js", Set.of("node.js", "nodejs"));
        aliases.put("Express", Set.of("express", "expressjs"));
        aliases.put("Redux", Set.of("redux"));
        aliases.put("Firebase", Set.of("firebase"));
        aliases.put("REST APIs", Set.of("rest api", "rest apis", "restful api", "rest services"));
        aliases.put("Docker", Set.of("docker", "containerization", "containers"));
        aliases.put("Kubernetes", Set.of("kubernetes", "k8s"));
        aliases.put("AWS", Set.of("aws", "amazon web services"));
        aliases.put("GCP", Set.of("gcp", "google cloud", "google cloud platform"));
        aliases.put("Azure", Set.of("azure", "microsoft azure"));
        aliases.put("Kafka", Set.of("kafka", "apache kafka"));
        aliases.put("Microservices", Set.of("microservices", "microservice architecture"));
        aliases.put("CI/CD", Set.of("ci cd", "ci/cd", "cicd", "continuous integration", "continuous delivery"));
        aliases.put("Git", Set.of("git", "github", "gitlab", "bitbucket"));
        aliases.put("HTML", Set.of("html", "html5"));
        aliases.put("CSS", Set.of("css", "css3"));
        aliases.put("Tailwind", Set.of("tailwind", "tailwindcss"));
        aliases.put("Python", Set.of("python"));
        aliases.put("Django", Set.of("django"));
        aliases.put("Flask", Set.of("flask"));
        aliases.put("FastAPI", Set.of("fastapi", "fast api"));
        aliases.put("Pandas", Set.of("pandas"));
        aliases.put("Machine Learning", Set.of("machine learning", "ml"));
        aliases.put("TensorFlow", Set.of("tensorflow"));
        aliases.put("PyTorch", Set.of("pytorch", "torch"));
        aliases.put("Airflow", Set.of("airflow", "apache airflow"));
        aliases.put("Spark", Set.of("spark", "apache spark", "pyspark"));
        aliases.put("MongoDB", Set.of("mongodb", "mongo db", "mongo"));
        aliases.put("Redis", Set.of("redis"));
        aliases.put("GraphQL", Set.of("graphql"));
        aliases.put("Figma", Set.of("figma"));
        aliases.put("Jest", Set.of("jest"));
        aliases.put("JUnit", Set.of("junit"));
        aliases.put("Selenium", Set.of("selenium"));
        aliases.put("Linux", Set.of("linux", "unix"));
        aliases.put("Terraform", Set.of("terraform"));
        aliases.put("C#", Set.of("c#", "c sharp"));
        aliases.put(".NET", Set.of(".net", "dotnet", "asp.net"));
        aliases.put("Angular", Set.of("angular", "angularjs"));
        aliases.put("Vue", Set.of("vue", "vue.js", "vuejs"));
        aliases.put("Next.js", Set.of("next.js", "nextjs"));
        aliases.put("React Native", Set.of("react native"));
        aliases.put("Go", Set.of("golang", "go language"));
        aliases.put("PHP", Set.of("php"));
        aliases.put("Laravel", Set.of("laravel"));
        aliases.put("Ruby", Set.of("ruby"));
        aliases.put("Rails", Set.of("rails", "ruby on rails"));
        return aliases;
    }

    private Map<RoleCategory, Set<String>> buildRoleSignals() {
        Map<RoleCategory, Set<String>> signals = new LinkedHashMap<>();
        signals.put(RoleCategory.BACKEND, Set.of(
            "backend", "backend developer", "backend engineer", "java", "spring boot", "api", "rest", "microservices", "hibernate"
        ));
        signals.put(RoleCategory.FRONTEND, Set.of(
            "frontend", "react", "javascript", "typescript", "angular", "vue", "css", "html", "redux"
        ));
        signals.put(RoleCategory.FULLSTACK, Set.of(
            "fullstack", "full stack", "frontend", "backend", "react", "node.js", "spring boot"
        ));
        signals.put(RoleCategory.DATA, Set.of(
            "data engineer", "data analyst", "machine learning", "python", "pandas", "spark", "airflow", "analytics", "etl"
        ));
        signals.put(RoleCategory.DEVOPS, Set.of(
            "devops", "docker", "kubernetes", "terraform", "ci/cd", "linux", "infrastructure", "sre", "platform engineer"
        ));
        signals.put(RoleCategory.CLOUD, Set.of(
            "cloud", "aws", "azure", "gcp", "terraform", "kubernetes", "solution architect"
        ));
        signals.put(RoleCategory.QA, Set.of(
            "qa", "quality assurance", "testing", "automation testing", "selenium", "junit", "jest", "test engineer"
        ));
        signals.put(RoleCategory.MOBILE, Set.of(
            "android", "ios", "mobile", "react native", "flutter", "swift", "kotlin"
        ));
        signals.put(RoleCategory.DESIGN, Set.of(
            "graphic designer", "designer", "figma", "photoshop", "illustrator", "ui designer", "ux designer"
        ));
        signals.put(RoleCategory.FINANCE, Set.of(
            "investment advisor", "financial analyst", "finance", "accounting", "portfolio", "wealth management"
        ));
        signals.put(RoleCategory.SUPPORT, Set.of(
            "support", "customer support", "service desk", "technical support", "operations"
        ));
        return signals;
    }
}
