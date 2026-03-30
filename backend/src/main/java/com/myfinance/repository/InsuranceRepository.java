package com.myfinance.repository;

import com.myfinance.model.Insurance;
import com.myfinance.model.enums.InsuranceType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InsuranceRepository extends JpaRepository<Insurance, Long> {
    List<Insurance> findByUserId(Long userId);

    List<Insurance> findByUserIdOrUserIdIsNull(Long userId);

    Optional<Insurance> findByUserIdAndInsuranceType(Long userId, InsuranceType insuranceType);

    Optional<Insurance> findByInsuranceType(InsuranceType insuranceType);
}
