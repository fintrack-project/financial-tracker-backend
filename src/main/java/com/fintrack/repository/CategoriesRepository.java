package com.fintrack.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import com.fintrack.model.Category;

import java.util.*;

@Repository
public interface CategoriesRepository extends JpaRepository<Category, Integer> {
    @Query(value = "SELECT category_id FROM categories WHERE account_id = :accountId AND category_name = :categoryName", nativeQuery = true)
    Integer findCategoryIdByAccountIdAndCategoryName(@Param("accountId") UUID accountId, @Param("categoryName") String categoryName);

    @Query(value = """
        SELECT category_id 
        FROM categories 
        WHERE account_id = :accountId 
          AND parent_id = (
              SELECT category_id 
              FROM categories 
              WHERE account_id = :accountId 
                AND LOWER(category_name) = LOWER(:categoryName) 
                AND parent_id IS NULL
          ) 
          AND LOWER(category_name) = LOWER(:subcategoryName)
        """, nativeQuery = true)
    Integer findSubcategoryIdByAccountIdAndCategoryNameAndSubcategoryName(
        @Param("accountId") UUID accountId,
        @Param("categoryName") String categoryName,
        @Param("subcategoryName") String subcategoryName
    );

    @Query(value = "SELECT * FROM categories WHERE account_id = :accountId AND parent_id IS NULL ORDER BY priority", nativeQuery = true)
    List<Category> findCategoriesByAccountId(@Param("accountId") UUID accountId);

    @Query(value = "SELECT * FROM categories WHERE account_id = :accountId AND parent_id = :parentId ORDER BY priority", nativeQuery = true)
    List<Category> findSubcategoriesByParentId(@Param("accountId") UUID accountId, @Param("parentId") Integer parentId);

    @Query(value = "SELECT MAX(priority) FROM categories WHERE account_id = :accountId AND parent_id IS NULL", nativeQuery = true)
    Integer findMaxPriorityByAccountId(@Param("accountId") UUID accountId);

    @Query(value = """
        SELECT MAX(priority) 
        FROM categories 
        WHERE account_id = :accountId 
            AND parent_id = (
                SELECT category_id 
                FROM categories 
                WHERE account_id = :accountId 
                    AND LOWER(category_name) = LOWER(:categoryName) 
                    AND parent_id IS NULL
        )
        """, nativeQuery = true)
    Integer findMaxSubcategoryPriorityByAccountIdAndCategoryName(
        @Param("accountId") UUID accountId,
        @Param("categoryName") String categoryName
    );

    @Modifying
    @Query(value = "UPDATE categories SET priority = :priority WHERE category_id = :categoryId", nativeQuery = true)
    void updateSubcategoryPriority(@Param("categoryId") Integer categoryId, @Param("priority") int priority);

    @Modifying
    @Query(value = "UPDATE categories SET category_name = :newCategoryName WHERE account_id = :accountId AND category_id = :categoryId", nativeQuery = true)
    void updateCategoryName(
            @Param("accountId") UUID accountId,
            @Param("categoryId") Integer categoryId,
            @Param("newCategoryName") String newCategoryName);

    @Modifying
    @Query(value = """
        UPDATE categories 
        SET category_name = :newSubcategoryName, updated_at = CURRENT_TIMESTAMP 
        WHERE account_id = :accountId 
            AND parent_id = (
                SELECT category_id 
                FROM categories 
                WHERE account_id = :accountId 
                    AND LOWER(category_name) = LOWER(:categoryName) 
                    AND parent_id IS NULL
        ) 
        AND LOWER(category_name) = LOWER(:oldSubcategoryName)
        """, nativeQuery = true)
    void updateSubcategoryName(
        @Param("accountId") UUID accountId,
        @Param("categoryName") String categoryName,
        @Param("oldSubcategoryName") String oldSubcategoryName,
        @Param("newSubcategoryName") String newSubcategoryName
    );

    @Query(value = """
        INSERT INTO categories (account_id, category_name, parent_id, level, priority, updated_at)
        VALUES (:accountId, :categoryName, :parentId, :level, :priority, CURRENT_TIMESTAMP)
        RETURNING category_id
        """, nativeQuery = true)
    Integer insertCategory(
    @Param("accountId") UUID accountId, 
    @Param("categoryName") String categoryName, 
    @Param("parentId") Integer parentId, 
    @Param("level") int level, 
    @Param("priority") int priority);

    @Modifying
    @Query(value = """
        INSERT INTO categories (account_id, category_name, parent_id, level, priority, updated_at) 
        VALUES (
            :accountId, 
            :subcategoryName, 
            (
                SELECT category_id 
                FROM categories 
                WHERE account_id = :accountId 
                    AND LOWER(category_name) = LOWER(:categoryName) 
                    AND parent_id IS NULL
            ), 
            2, 
            :priority, 
            CURRENT_TIMESTAMP
        )
        """, nativeQuery = true)
    void insertSubcategory(
        @Param("accountId") UUID accountId,
        @Param("categoryName") String categoryName,
        @Param("subcategoryName") String subcategoryName,
        @Param("priority") Integer priority
    );

    @Modifying
    @Query(value = "DELETE FROM categories WHERE account_id = :accountId AND parent_id = :parentId", nativeQuery = true)
    void deleteByParentId(@Param("accountId") UUID accountId, @Param("parentId") Integer parentId);

    @Modifying
    @Query(value = "DELETE FROM categories WHERE category_id = :categoryId", nativeQuery = true)
    void deleteByCategoryId(@Param("categoryId") Integer categoryId);

    @Modifying
    @Query(value = "DELETE FROM categories WHERE account_id = :accountId", nativeQuery = true)
    void deleteByAccountId(@Param("accountId") UUID accountId);

    @Modifying
    @Query(value = """
        DELETE FROM categories 
        WHERE account_id = :accountId 
            AND parent_id = (
                SELECT category_id 
                FROM categories 
                WHERE account_id = :accountId 
                    AND LOWER(category_name) = LOWER(:categoryName) 
                    AND parent_id IS NULL
        )
        """, nativeQuery = true)
    void deleteSubcategoriesByCategoryName(
        @Param("accountId") UUID accountId,
        @Param("categoryName") String categoryName
    );

    @Modifying
    @Query(value = """
        INSERT INTO holdings_categories (account_id, asset_name, category_id, updated_at)
        VALUES (:accountId, :assetName, :categoryId, CURRENT_TIMESTAMP)
        ON CONFLICT (account_id, asset_name)
        DO UPDATE SET category_id = EXCLUDED.category_id, updated_at = CURRENT_TIMESTAMP
        """, nativeQuery = true)
    void upsertHoldingCategory(
        @Param("accountId") UUID accountId,
        @Param("assetName") String assetName,
        @Param("categoryId") Integer categoryId
    );
}