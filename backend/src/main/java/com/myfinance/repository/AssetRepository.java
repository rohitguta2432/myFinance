package com.myfinance.repository;

import com.myfinance.model.Asset;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface AssetRepository extends JpaRepository<Asset, Long> {
    List<Asset> findByUserId(Long userId);
}
