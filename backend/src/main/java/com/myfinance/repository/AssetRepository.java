package com.myfinance.repository;

import com.myfinance.model.Asset;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AssetRepository extends JpaRepository<Asset, Long> {
    List<Asset> findByUserId(Long userId);

    List<Asset> findByUserIdOrUserIdIsNull(Long userId);
}
