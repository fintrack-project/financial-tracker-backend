package com.fintrack.repository;

import com.fintrack.model.WatchlistData;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

@Repository
public interface WatchlistDataRepository extends JpaRepository<WatchlistData, Long> {

    @Query(value = "SELECT * FROM watchlist_data WHERE account_id = :accountId AND asset_type IN :assetTypes AND deleted_at IS NULL", nativeQuery = true)
    List<WatchlistData> findWatchlistDataByAccountIdAndAssetTypes(@Param("accountId") UUID accountId, @Param("assetTypes") List<String> assetTypes);

    @Query(value = "SELECT * FROM watchlist_data WHERE account_id = :accountId AND symbol = :symbol AND asset_type = :assetType AND deleted_at IS NULL", nativeQuery = true)
    WatchlistData findByAccountIdAndSymbolAndAssetType(@Param("accountId") UUID accountId, @Param("symbol") String symbol, @Param("assetType") String assetType);
}