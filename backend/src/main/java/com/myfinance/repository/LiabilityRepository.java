package com.myfinance.repository;

import com.myfinance.model.Liability;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LiabilityRepository extends JpaRepository<Liability, Long> {
}
