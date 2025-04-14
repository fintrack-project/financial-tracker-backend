package com.fintrack.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import com.fintrack.model.Category;

import java.util.UUID;

@Repository
public interface CategoriesRepository extends JpaRepository<Category, Integer> {

    @Modifying
    @Query(value = "DELETE FROM categories WHERE account_id = :accountId", nativeQuery = true)
    void deleteByAccountId(@Param("accountId") UUID accountId);

    @Modifying
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
}