package com.fintrack.controller;

import com.fintrack.model.MarketData;
import com.fintrack.service.MarketDataService;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/market-data")
public class MarketDataController {

    private final MarketDataService marketDataService;

    public MarketDataController(MarketDataService marketDataService) {
        this.marketDataService = marketDataService;
    }

    @GetMapping("/fetch")
    public ResponseEntity<List<MarketData>> fetchMarketData(@RequestParam List<String> symbols) {
        try {
            List<MarketData> marketData = marketDataService.fetchMarketData(symbols);
            return ResponseEntity.ok(marketData);
        } catch (Exception e) {
            e.printStackTrace(); // Log the error for debugging
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Collections.emptyList());
        }
        
    }
}