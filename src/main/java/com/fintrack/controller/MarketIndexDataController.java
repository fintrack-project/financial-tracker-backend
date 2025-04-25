package com.fintrack.controller;

import com.fintrack.service.MarketIndexDataService;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/market-index-data")
public class MarketIndexDataController {

    private MarketIndexDataService marketIndexDataService;

    public MarketIndexDataController(MarketIndexDataService marketIndexDataService) {
        this.marketIndexDataService = marketIndexDataService;
    }

    @GetMapping("/fetch")
    public ResponseEntity<Map<String, Object>> fetchMarketData(@RequestParam List<String> symbols) {
        try {
            Map<String, Object> marketData = marketIndexDataService.getMostRecentMarketIndexData(symbols);
            return ResponseEntity.ok(marketData);
        } catch (Exception e) {
            e.printStackTrace(); // Log the error for debugging
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "Failed to fetch market index data"));
        }
    }
}