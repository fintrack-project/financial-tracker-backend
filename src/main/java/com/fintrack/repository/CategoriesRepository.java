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

    @Modifying
    @Query(value = "DELETE FROM categories WHERE account_id = :accountId AND parent_id = :parentId", nativeQuery = true)
    void deleteByParentId(@Param("accountId") UUID accountId, @Param("parentId") Integer parentId);

    @Modifying
    @Query(value = "DELETE FROM categories WHERE account_id = :accountId", nativeQuery = true)
    void deleteByAccountId(@Param("accountId") UUID accountId);

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

    @Query(value = "SELECT * FROM categories WHERE account_id = :accountId AND parent_id IS NULL ORDER BY priority", nativeQuery = true)
    List<Category> findCategoriesByAccountId(@Param("accountId") UUID accountId);

    @Query(value = "SELECT * FROM categories WHERE account_id = :accountId AND parent_id = :parentId ORDER BY priority", nativeQuery = true)
    List<Category> findSubcategoriesByParentId(@Param("accountId") UUID accountId, @Param("parentId") Integer parentId);
    
    @Modifying
    @Query(value = "UPDATE categories SET priority = :priority WHERE category_id = :categoryId", nativeQuery = true)
    void updateSubcategoryPriority(@Param("categoryId") Integer categoryId, @Param("priority") int priority);
}