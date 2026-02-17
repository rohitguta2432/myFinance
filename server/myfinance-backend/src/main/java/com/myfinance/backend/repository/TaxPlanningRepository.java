package com.myfinance.backend.repository;

import com.myfinance.backend.model.TaxPlanning;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface TaxPlanningRepository extends JpaRepository<TaxPlanning, UUID> {
    Optional<TaxPlanning> findByUserId(UUID userId);
}
