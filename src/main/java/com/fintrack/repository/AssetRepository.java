package com.fintrack.repository;

import com.fintrack.model.Asset;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface AssetRepository extends JpaRepository<Asset, Long> {

    // Find an Asset by accountId and assetName
    Optional<Asset> findByAccountIdAndAssetName(UUID accountId, String assetName);
}