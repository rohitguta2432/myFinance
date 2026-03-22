package com.myfinance.repository;

import com.myfinance.model.Liability;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface LiabilityRepository extends JpaRepository<Liability, Long> {
    List<Liability> findByUserId(Long userId);
    List<Liability> findByUserIdOrUserIdIsNull(Long userId);
}
