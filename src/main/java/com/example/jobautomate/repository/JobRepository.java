package com.example.jobautomate.repository;

import com.example.jobautomate.model.Job;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JobRepository extends JpaRepository<Job, Long> {

    List<Job> findByActiveTrueAndApplicationDeadlineGreaterThanEqual(LocalDate today);

    Optional<Job> findByExternalId(String externalId);
}
