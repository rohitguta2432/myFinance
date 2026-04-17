package com.myfinance.repository;

import com.myfinance.model.FeatureFlagAudit;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FeatureFlagAuditRepository extends JpaRepository<FeatureFlagAudit, Long> {
    List<FeatureFlagAudit> findTop100ByOrderByChangedAtDesc();
    List<FeatureFlagAudit> findByFlagKeyOrderByChangedAtDesc(String flagKey);
}
