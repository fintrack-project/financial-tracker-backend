package com.fintrack.controller.market;

import com.fintrack.common.ApiResponse;
import com.fintrack.common.ResponseWrapper;
import com.fintrack.model.market.WatchlistData;
import com.fintrack.service.market.WatchlistDataService;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.MediaType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

@RestController
@RequestMapping(value = "/api/watchlist-data", produces = MediaType.APPLICATION_JSON_VALUE)
public class WatchlistDataController {

    private static final Logger logger = LoggerFactory.getLogger(WatchlistDataController.class);
    private final WatchlistDataService watchlistDataService;

    public WatchlistDataController(WatchlistDataService watchlistDataService) {
        this.watchlistDataService = watchlistDataService;
    }

    @PostMapping(
        value = "/fetch",
        consumes = MediaType.APPLICATION_JSON_VALUE,
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<ApiResponse<List<WatchlistData>>> fetchWatchlistData(@RequestBody Map<String, Object> payload) {
        try {
            UUID accountId = UUID.fromString((String) payload.get("accountId"));
            List<String> assetTypes = (List<String>) payload.get("assetTypes");

            List<WatchlistData> watchlistData = watchlistDataService.fetchWatchlistData(accountId, assetTypes);
            return ResponseWrapper.ok(watchlistData);
        } catch (IllegalArgumentException e) {
            logger.error("Invalid request format: ", e);
            return ResponseWrapper.badRequest(e.getMessage());
        } catch (Exception e) {
            logger.error("Error fetching watchlist data: ", e);
            return ResponseWrapper.badRequest("Failed to fetch watchlist data");
        }
    }

    @PostMapping(
        value = "/add",
        consumes = MediaType.APPLICATION_JSON_VALUE,
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<ApiResponse<Void>> addWatchlistItem(@RequestBody Map<String, Object> payload) {
        try {
            UUID accountId = UUID.fromString((String) payload.get("accountId"));
            String symbol = (String) payload.get("symbol");
            String assetType = (String) payload.get("assetType");

            watchlistDataService.addWatchlistItem(accountId, symbol, assetType);
            return ResponseWrapper.ok(null, "Item added to watchlist successfully");
        } catch (IllegalArgumentException e) {
            logger.error("Invalid request format: ", e);
            return ResponseWrapper.badRequest(e.getMessage());
        } catch (Exception e) {
            logger.error("Error adding watchlist item: ", e);
            return ResponseWrapper.badRequest("Failed to add item to watchlist");
        }
    }

    @DeleteMapping(
        value = "/remove",
        consumes = MediaType.APPLICATION_JSON_VALUE,
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<ApiResponse<Void>> removeWatchlistItem(@RequestBody Map<String, Object> payload) {
        try {
            UUID accountId = UUID.fromString((String) payload.get("accountId"));
            String symbol = (String) payload.get("symbol");
            String assetType = (String) payload.get("assetType");

            watchlistDataService.removeWatchlistItem(accountId, symbol, assetType);
            return ResponseWrapper.ok(null, "Item removed from watchlist successfully");
        } catch (IllegalArgumentException e) {
            logger.error("Invalid request format: ", e);
            return ResponseWrapper.badRequest(e.getMessage());
        } catch (Exception e) {
            logger.error("Error removing watchlist item: ", e);
            return ResponseWrapper.badRequest("Failed to remove item from watchlist");
        }
    }
}