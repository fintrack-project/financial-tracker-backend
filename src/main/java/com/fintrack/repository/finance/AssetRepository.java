package com.fintrack.repository.finance;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Repository;

import com.fintrack.model.finance.Asset;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.*;

@Repository
public interface AssetRepository extends JpaRepository<Asset, Long> {

    // Find an Asset by accountId and assetName
    Optional<Asset> findByAccountIdAndAssetName(UUID accountId, String assetName);

    @Query(value = "SELECT * FROM asset WHERE account_id = :accountId", nativeQuery = true)
    List<Asset> findByAccountId(@Param("accountId") UUID accountId);

    @Modifying
    @Query(value = """
        DELETE FROM asset
        WHERE account_id = :accountId
        AND asset_name NOT IN (
            SELECT DISTINCT asset_name FROM transactions 
            WHERE account_id = :accountId AND deleted_at IS NULL
        )
        """, nativeQuery = true)
    int deleteOrphanedAssets(@Param("accountId") UUID accountId);
}