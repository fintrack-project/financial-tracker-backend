package com.fintrack.controller;

import com.fintrack.model.MarketData;
import com.fintrack.service.MarketDataService;
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

    @PostMapping("/update")
    public ResponseEntity<Map<String, String>> updateMarketData(@RequestBody List<String> assetNames) {
        marketDataService.sendMarketDataUpdateRequest(assetNames);
        return ResponseEntity.ok(Map.of("message", "Market data update request sent."));
    }

    @GetMapping("/fetch")
    public ResponseEntity<List<MarketData>> fetchMarketData(@RequestParam List<String> assetNames) {
        List<MarketData> marketData = marketDataService.fetchMarketDataByAssetNames(assetNames);
        return ResponseEntity.ok(marketData);
    }
}