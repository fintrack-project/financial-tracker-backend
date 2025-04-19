package com.fintrack.repository;

import com.fintrack.model.Asset;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.*;

@Repository
public interface AssetRepository extends JpaRepository<Asset, Long> {

    // Find an Asset by accountId and assetName
    Optional<Asset> findByAccountIdAndAssetName(UUID accountId, String assetName);

    @Query(value = "SELECT * FROM asset WHERE account_id = :accountId", nativeQuery = true)
    List<Asset> findByAccountId(@Param("accountId") UUID accountId);
}