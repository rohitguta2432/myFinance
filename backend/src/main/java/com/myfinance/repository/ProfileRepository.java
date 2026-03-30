package com.myfinance.repository;

import com.myfinance.model.Profile;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProfileRepository extends JpaRepository<Profile, Long> {
    Optional<Profile> findByUserId(Long userId);

    Optional<Profile> findFirstByUserIdOrUserIdIsNull(Long userId);
}
