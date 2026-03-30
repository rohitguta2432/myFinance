package com.myfinance.repository;

import com.myfinance.model.Liability;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LiabilityRepository extends JpaRepository<Liability, Long> {
    List<Liability> findByUserId(Long userId);

    List<Liability> findByUserIdOrUserIdIsNull(Long userId);
}
