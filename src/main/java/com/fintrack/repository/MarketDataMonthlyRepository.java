package com.fintrack.repository;

import com.fintrack.model.MarketDataMonthly;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface MarketDataMonthlyRepository extends JpaRepository<MarketDataMonthly, Long> {

    @Query(value = "SELECT * FROM market_data_monthly WHERE symbol IN :symbols AND date = :date", nativeQuery = true)
    List<MarketDataMonthly> findBySymbolsAndDate(@Param("symbols") List<String> symbols, @Param("date") LocalDate date);

    @Query(value = "SELECT * FROM market_data_monthly WHERE symbol = :symbol AND date BETWEEN :startDate AND :endDate", nativeQuery = true)
    List<MarketDataMonthly> findBySymbolAndDateRange(@Param("symbol") String symbol, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    @Query(value = "SELECT * FROM market_data_monthly WHERE symbol IN :symbols AND date BETWEEN :startDate AND :endDate", nativeQuery = true)
    List<MarketDataMonthly> findBySymbolsAndDateRange(@Param("symbol") String symbol, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);
}