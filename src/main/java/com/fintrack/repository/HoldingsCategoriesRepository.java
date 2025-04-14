package com.fintrack.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import com.fintrack.model.HoldingsCategory;

import java.util.UUID;

@Repository
public interface HoldingsCategoriesRepository extends JpaRepository<HoldingsCategory, Integer> {

    @Modifying
    @Query(value = """
        INSERT INTO holdings_categories (account_id, asset_name, category_id, updated_at)
        VALUES (:accountId, :assetName, :categoryId, CURRENT_TIMESTAMP)
        ON CONFLICT (account_id, asset_name)
        DO UPDATE SET category_id = EXCLUDED.category_id, updated_at = CURRENT_TIMESTAMP
        """, nativeQuery = true)
    void upsertHoldingCategory(UUID accountId, String assetName, Integer categoryId);
}