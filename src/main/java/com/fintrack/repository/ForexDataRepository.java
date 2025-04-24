package com.fintrack.repository;

import com.fintrack.model.ForexData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ForexDataRepository extends JpaRepository<ForexData, Long> {

    @Query(value = "SELECT * FROM forex_data WHERE symbol IN :symbols", nativeQuery = true)
    List<ForexData> findBySymbols(@Param("symbols") List<String> symbols);
}