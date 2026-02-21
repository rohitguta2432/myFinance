package com.myfinance.repository;

import com.myfinance.model.Insurance;
import com.myfinance.model.enums.InsuranceType;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface InsuranceRepository extends JpaRepository<Insurance, Long> {
    Optional<Insurance> findByInsuranceType(InsuranceType insuranceType);
}
