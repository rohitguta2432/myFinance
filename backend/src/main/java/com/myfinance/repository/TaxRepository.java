package com.myfinance.repository;

import com.myfinance.model.Tax;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface TaxRepository extends JpaRepository<Tax, Long> {
    Optional<Tax> findByUserId(Long userId);
}
