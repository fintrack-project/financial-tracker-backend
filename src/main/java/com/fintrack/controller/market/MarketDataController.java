package com.fintrack.controller.market;

import com.fintrack.common.ApiResponse;
import com.fintrack.common.ResponseWrapper;
import com.fintrack.model.market.MarketData;
import com.fintrack.service.market.MarketDataService;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.MediaType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

@RestController
@RequestMapping(value = "/api/market-data", produces = MediaType.APPLICATION_JSON_VALUE)
public class MarketDataController {

    private static final Logger logger = LoggerFactory.getLogger(MarketDataController.class);
    private final MarketDataService marketDataService;

    public MarketDataController(MarketDataService marketDataService) {
        this.marketDataService = marketDataService;
    }

    @PostMapping(
        value = "/fetch",
        consumes = MediaType.APPLICATION_JSON_VALUE,
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<ApiResponse<List<MarketData>>> fetchMarketData(@RequestBody Map<String, Object> payload) {
        try {
            UUID accountId = UUID.fromString((String) payload.get("accountId"));
            List<Map<String, String>> assets = (List<Map<String, String>>) payload.get("assets");

            List<MarketData> marketData = marketDataService.fetchMarketData(accountId, assets);
            return ResponseWrapper.ok(marketData);
        } catch (IllegalArgumentException e) {
            logger.error("Invalid request format: ", e);
            return ResponseWrapper.badRequest(e.getMessage());
        } catch (Exception e) {
            logger.error("Error fetching market data: ", e);
            return ResponseWrapper.badRequest("Failed to fetch market data");
        }
    }
}