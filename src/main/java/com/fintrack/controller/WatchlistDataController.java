package com.fintrack.controller;

import com.fintrack.model.WatchlistData;
import com.fintrack.service.WatchlistDataService;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/watchlist-data")
public class WatchlistDataController {

    private final WatchlistDataService watchlistDataService;

    public WatchlistDataController(WatchlistDataService watchlistDataService) {
        this.watchlistDataService = watchlistDataService;
    }

    @PostMapping("/fetch")
    public ResponseEntity<List<WatchlistData>> fetchWatchlistData(@RequestBody Map<String, Object> payload) {
        try {
            UUID accountId = UUID.fromString((String) payload.get("accountId"));
            List<String> assetTypes = (List<String>) payload.get("assetTypes");

            List<WatchlistData> watchlistData = watchlistDataService.fetchWatchlistData(accountId, assetTypes);
            return ResponseEntity.ok(watchlistData);
        } catch (Exception e) {
            e.printStackTrace(); // Log the error for debugging
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Collections.emptyList());
        }
    }

    @PostMapping("/add")
    public ResponseEntity<String> addWatchlistItem(@RequestBody Map<String, Object> payload) {
        try {
            UUID accountId = UUID.fromString((String) payload.get("accountId"));
            String symbol = (String) payload.get("symbol");
            String assetType = (String) payload.get("assetType");

            watchlistDataService.addWatchlistItem(accountId, symbol, assetType);
            return ResponseEntity.ok("Item added to watchlist successfully.");
        } catch (Exception e) {
            e.printStackTrace(); // Log the error for debugging
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to add item to watchlist.");
        }
    }

    @DeleteMapping("/remove")
    public ResponseEntity<String> removeWatchlistItem(@RequestBody Map<String, Object> payload) {
        try {
            UUID accountId = UUID.fromString((String) payload.get("accountId"));
            String symbol = (String) payload.get("symbol");
            String assetType = (String) payload.get("assetType");

            watchlistDataService.removeWatchlistItem(accountId, symbol, assetType);
            return ResponseEntity.ok("Item removed from watchlist successfully.");
        } catch (Exception e) {
            e.printStackTrace(); // Log the error for debugging
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to remove item from watchlist.");
        }
    }
}