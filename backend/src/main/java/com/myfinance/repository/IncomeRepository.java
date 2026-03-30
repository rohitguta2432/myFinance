package com.myfinance.repository;

import com.myfinance.model.Income;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IncomeRepository extends JpaRepository<Income, Long> {
    List<Income> findByUserId(Long userId);

    List<Income> findByUserIdOrUserIdIsNull(Long userId);
}
