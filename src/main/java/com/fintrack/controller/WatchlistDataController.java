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
}