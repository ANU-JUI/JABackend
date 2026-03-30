package com.example.jobautomate.repository;

import com.example.jobautomate.model.UserSemanticProfile;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserSemanticProfileRepository extends JpaRepository<UserSemanticProfile, String> {
}
