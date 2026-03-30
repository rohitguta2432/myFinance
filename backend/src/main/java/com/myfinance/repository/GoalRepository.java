package com.myfinance.repository;

import com.myfinance.model.Goal;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GoalRepository extends JpaRepository<Goal, Long> {
    List<Goal> findByUserId(Long userId);

    List<Goal> findByUserIdOrUserIdIsNull(Long userId);
}
