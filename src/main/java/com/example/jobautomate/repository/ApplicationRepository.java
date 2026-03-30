package com.example.jobautomate.repository;

import com.example.jobautomate.model.ApplicationRecord;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ApplicationRepository extends JpaRepository<ApplicationRecord, Long> {

    List<ApplicationRecord> findByUserId(String userId);

    Optional<ApplicationRecord> findByUserIdAndJobId(String userId, Long jobId);
}
