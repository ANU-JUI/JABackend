package com.example.jobautomate.service;

import java.time.LocalDate;
import java.util.List;

public final class SeedJobData {

    private SeedJobData() {
    }

    public static List<SeedJobPost> jobs() {
        return List.of(
            new SeedJobPost("job-001", "Frontend Engineer", "NovaStack", "Full-time", "India", "Karnataka", "Bengaluru", 2,
                """
                Build React interfaces for a high-volume SaaS product. Required: React, JavaScript, CSS, REST APIs, Firebase, Git.
                Good to have: TypeScript and Jest. Deadline: April 15, 2026.
                """,
                "https://example.com/jobs/job-001", LocalDate.of(2026, 4, 15)),
            new SeedJobPost("job-002", "Backend Java Developer", "AtlasWare", "Full-time", "India", "Maharashtra", "Pune", 3,
                """
                Work on Spring Boot microservices and SQL-backed APIs. Required: Java, Spring Boot, Hibernate, SQL, Docker, Git.
                Preferred: AWS and CI/CD familiarity. Apply by 2026-04-20.
                """,
                "https://example.com/jobs/job-002", LocalDate.of(2026, 4, 20)),
            new SeedJobPost("job-003", "Full Stack Developer", "BrightLoop", "Hybrid", "United States", "California", "San Francisco", 4,
                """
                Build end-to-end features with React, Node.js, Express, MongoDB, REST APIs, Docker and AWS.
                Looking for engineers comfortable with CI/CD pipelines. Deadline: May 2, 2026.
                """,
                "https://example.com/jobs/job-003", LocalDate.of(2026, 5, 2)),
            new SeedJobPost("job-004", "Junior Software Engineer", "CivicCloud", "Remote", "Remote", "", "", 0,
                """
                Entry-level role for freshers with JavaScript, React, HTML, CSS and Git basics.
                Will train on Firebase and REST APIs. Last date to apply April 8, 2026.
                """,
                "https://example.com/jobs/job-004", LocalDate.of(2026, 4, 8)),
            new SeedJobPost("job-005", "Data Engineer", "ScaleForge", "Full-time", "India", "Telangana", "Hyderabad", 5,
                """
                Design pipelines with Python, Spark, Airflow, Kafka, AWS, SQL and Docker.
                Prior experience with data platforms is required. Deadline: April 28, 2026.
                """,
                "https://example.com/jobs/job-005", LocalDate.of(2026, 4, 28)),
            new SeedJobPost("job-006", "React Developer", "PixelMint", "Contract", "India", "Delhi", "New Delhi", 1,
                """
                Build marketing and dashboard experiences using React, JavaScript, CSS, HTML, Firebase and Figma collaboration.
                TypeScript is a plus. Apply by April 18, 2026.
                """,
                "https://example.com/jobs/job-006", LocalDate.of(2026, 4, 18)),
            new SeedJobPost("job-007", "Cloud Support Engineer", "Northwind Systems", "Full-time", "Canada", "Ontario", "Toronto", 2,
                """
                Support production applications deployed on AWS with Docker, Linux, SQL and monitoring workflows.
                Strong communication and troubleshooting skills required. Deadline: April 22, 2026.
                """,
                "https://example.com/jobs/job-007", LocalDate.of(2026, 4, 22)),
            new SeedJobPost("job-008", "Senior Platform Engineer", "BlueRail", "Remote", "Remote", "", "", 7,
                """
                Lead platform evolution using Kubernetes, Kafka, Microservices, AWS, CI/CD, Java and Spring Boot.
                This is a senior position requiring systems design depth. Deadline: May 10, 2026.
                """,
                "https://example.com/jobs/job-008", LocalDate.of(2026, 5, 10)),
            new SeedJobPost("job-009", "Python Developer", "DataMosaic", "Hybrid", "India", "Tamil Nadu", "Chennai", 2,
                """
                Develop APIs and automation services with Python, FastAPI, SQL, Docker, Git and PostgreSQL.
                Exposure to Pandas is useful. Deadline: April 26, 2026.
                """,
                "https://example.com/jobs/job-009", LocalDate.of(2026, 4, 26)),
            new SeedJobPost("job-010", "DevOps Engineer", "OrbitMesh", "Full-time", "Germany", "Berlin", "Berlin", 4,
                """
                Manage CI/CD pipelines and container platforms using Docker, Kubernetes, AWS, Git and Terraform-style workflows.
                Strong scripting and release engineering experience expected. Deadline: April 30, 2026.
                """,
                "https://example.com/jobs/job-010", LocalDate.of(2026, 4, 30)),
            new SeedJobPost("job-011", "Product Analyst", "Insight Harbor", "Full-time", "India", "Karnataka", "Bengaluru", 2,
                """
                Analyze product funnels using SQL, Python, Pandas, dashboards and stakeholder reporting.
                Experience with experimentation is a plus. Apply by April 17, 2026.
                """,
                "https://example.com/jobs/job-011", LocalDate.of(2026, 4, 17)),
            new SeedJobPost("job-012", "QA Automation Engineer", "Verity Labs", "Full-time", "United Kingdom", "England", "London", 3,
                """
                Build test automation with Java, Selenium, REST APIs, JUnit, CI/CD and Git.
                Strong debugging and quality discipline required. Deadline: April 24, 2026.
                """,
                "https://example.com/jobs/job-012", LocalDate.of(2026, 4, 24))
        );
    }

    public record SeedJobPost(
        String externalId,
        String title,
        String company,
        String jobType,
        String country,
        String state,
        String city,
        int experienceYears,
        String description,
        String applyLink,
        LocalDate fallbackDeadline
    ) {
    }
}
