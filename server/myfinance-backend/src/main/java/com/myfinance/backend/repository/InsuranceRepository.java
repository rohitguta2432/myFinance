package com.myfinance.backend.repository;

import com.myfinance.backend.model.Insurance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface InsuranceRepository extends JpaRepository<Insurance, UUID> {
    List<Insurance> findByUserId(UUID userId);
}
