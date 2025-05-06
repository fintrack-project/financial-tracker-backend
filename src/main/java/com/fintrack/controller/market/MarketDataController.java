package com.fintrack.controller.market;

import com.fintrack.model.market.MarketData;
import com.fintrack.service.market.MarketDataService;

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

    // POST endpoint to fetch market data
    @PostMapping("/fetch")
    public ResponseEntity<List<MarketData>> fetchMarketData(@RequestBody Map<String, Object> payload) {
        try {
            UUID accountId = UUID.fromString((String) payload.get("accountId"));
            List<Map<String, String>> assets = (List<Map<String, String>>) payload.get("assets");

            List<MarketData> marketData = marketDataService.fetchMarketData(accountId, assets);
            return ResponseEntity.ok(marketData);
        } catch (Exception e) {
            e.printStackTrace(); // Log the error for debugging
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Collections.emptyList());
        }
    }
}