package com.fintrack.controller;

import com.fintrack.service.MarketDataUpdateService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/market-data")
public class MarketDataController {

    private final MarketDataUpdateService marketDataUpdateService;

    public MarketDataController(MarketDataUpdateService marketDataUpdateService) {
        this.marketDataUpdateService = marketDataUpdateService;
    }

    @PostMapping("/update")
    public ResponseEntity<String> updateMarketData(@RequestBody List<String> assetNames) {
        marketDataUpdateService.sendMarketDataUpdateRequest(assetNames);
        return ResponseEntity.ok("Market data update request sent.");
    }
}