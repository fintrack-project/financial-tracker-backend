package com.fintrack.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import com.fintrack.model.HoldingsCategory;

import java.util.*;

@Repository
public interface HoldingsCategoriesRepository extends JpaRepository<HoldingsCategory, Integer> {

    @Modifying
    @Query(value = """
        INSERT INTO holdings_categories (account_id, asset_name, category_id, category, subcategory, updated_at)
        VALUES (:accountId, :assetName, :categoryId, :category, :subcategory, CURRENT_TIMESTAMP)
        ON CONFLICT (account_id, asset_name, category)
        DO UPDATE SET 
            category_id = EXCLUDED.category_id,
            category = EXCLUDED.category,
            subcategory = EXCLUDED.subcategory,
            updated_at = CURRENT_TIMESTAMP
        """, nativeQuery = true)
    void upsertHoldingCategory(
        @Param("accountId") UUID accountId,
        @Param("assetName") String assetName,
        @Param("categoryId") Integer categoryId,
        @Param("category") String category,
        @Param("subcategory") String subcategory
    );

    @Modifying
    @Query(value = """
        INSERT INTO holdings_categories (account_id, asset_name, category_id, category, subcategory, updated_at)
        VALUES (:accountId, :assetName, :categoryId, :category, :subcategory, CURRENT_TIMESTAMP)
        """, nativeQuery = true)
    void insertHoldingCategory(
        @Param("accountId") UUID accountId,
        @Param("assetName") String assetName,
        @Param("categoryId") Integer categoryId,
        @Param("category") String category,
        @Param("subcategory") String subcategory
    );

    @Modifying
    @Query(value = """
        DELETE FROM holdings_categories
        WHERE account_id = :accountId AND category = :category
        """, nativeQuery = true)
    void deleteByAccountIdAndCategory(
        @Param("accountId") UUID accountId,
        @Param("category") String category
    );

    @Modifying
    @Query(value = """
        DELETE FROM holdings_categories
        WHERE account_id = :accountId AND category_id = :categoryId
        """, nativeQuery = true)
    void deleteByAccountIdAndCategoryId(
        @Param("accountId") UUID accountId,
        @Param("categoryId") Integer categoryId
    );

    @Modifying
    @Query(value = """
        UPDATE holdings_categories
        SET subcategory = NULL, category_id = :categoryId
        WHERE account_id = :accountId AND subcategory = :subcategory
        """, nativeQuery = true)
    void updateSubcategoryToNull(
        @Param("accountId") UUID accountId,
        @Param("subcategory") String subcategory,
        @Param("categoryId") Integer categoryId
    );

    @Query(value = """
        SELECT *
        FROM holdings_categories h
        WHERE h.account_id = :accountId
        """, nativeQuery = true)
    List<HoldingsCategory> findHoldingsCategoryByAccountId(@Param("accountId") UUID accountId);

    @Query(value = """
        SELECT h.asset_name, h.category AS category, h.subcategory AS subcategory, c.priority AS priority
        FROM holdings_categories h
        LEFT JOIN categories c ON h.category_id = c.category_id
        WHERE h.account_id = :accountId
        ORDER BY c.priority ASC
        """, nativeQuery = true)
    List<Map<String, Object>> findHoldingsByAccountId(@Param("accountId") UUID accountId);

    @Query(value = """
        SELECT h.asset_name, h.subcategory
        FROM holdings_categories h
        WHERE h.account_id = :accountId AND h.category_id = :categoryId
        """, nativeQuery = true)
    List<Map<String, Object>> findHoldingsByCategoryId(@Param("accountId") UUID accountId, @Param("categoryId") Integer categoryId);
}