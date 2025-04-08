package com.fintrack.controller;

import com.fintrack.service.MarketAverageDataService;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api")
public class MarketAverageDataController {

    private MarketAverageDataService marketAverageDataService;

    public MarketAverageDataController(MarketAverageDataService marketAverageDataService) {
        this.marketAverageDataService = marketAverageDataService;
    }

    @GetMapping("/market-average-data")
    public ResponseEntity<Map<String, Object>> getMarketData(@RequestParam List<String> symbols) {
      try {
          Map<String, Object> marketData = marketAverageDataService.getMostRecentMarketData(symbols);
          return ResponseEntity.ok(marketData);
      } catch (Exception e) {
          e.printStackTrace(); // Log the error for debugging
          return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "Failed to fetch market data"));
      }
    }
}