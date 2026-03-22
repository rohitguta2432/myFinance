package com.myfinance.repository;

import com.myfinance.model.Income;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface IncomeRepository extends JpaRepository<Income, Long> {
    List<Income> findByUserId(Long userId);
    List<Income> findByUserIdOrUserIdIsNull(Long userId);
}
