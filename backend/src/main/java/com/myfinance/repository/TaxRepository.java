package com.myfinance.repository;

import com.myfinance.model.Tax;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TaxRepository extends JpaRepository<Tax, Long> {
    Optional<Tax> findByUserId(Long userId);

    Optional<Tax> findFirstByUserIdOrUserIdIsNull(Long userId);
}
