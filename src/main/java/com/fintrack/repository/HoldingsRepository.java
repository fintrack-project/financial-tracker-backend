package com.fintrack.repository;

import com.fintrack.model.Holdings;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.*;

@Repository
public interface HoldingsRepository extends JpaRepository<Holdings, Long> {

    @Query(value = """
        SELECT * 
        FROM holdings 
        WHERE account_id = :accountId
        AND total_balance > 0
        """, nativeQuery = true)
    List<Holdings> findHoldingsByAccount(@Param("accountId") UUID accountId);
}