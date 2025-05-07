package com.fintrack.controller.market;

import com.fintrack.common.ApiResponse;
import com.fintrack.service.market.MarketIndexDataService;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.MediaType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

@RestController
@RequestMapping(value = "/api/market-index-data", produces = MediaType.APPLICATION_JSON_VALUE)
public class MarketIndexDataController {

    private static final Logger logger = LoggerFactory.getLogger(MarketIndexDataController.class);
    private final MarketIndexDataService marketIndexDataService;

    public MarketIndexDataController(MarketIndexDataService marketIndexDataService) {
        this.marketIndexDataService = marketIndexDataService;
    }

    @GetMapping(
        value = "/fetch",
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<ApiResponse<Map<String, Object>>> fetchMarketData(@RequestParam List<String> symbols) {
        try {
            Map<String, Object> marketData = marketIndexDataService.getMostRecentMarketIndexData(symbols);
            return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(ApiResponse.success(marketData));
        } catch (IllegalArgumentException e) {
            logger.error("Invalid request format: ", e);
            return ResponseEntity.badRequest()
                .contentType(MediaType.APPLICATION_JSON)
                .body(ApiResponse.error("Invalid request format"));
        } catch (Exception e) {
            logger.error("Error fetching market index data: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .contentType(MediaType.APPLICATION_JSON)
                .body(ApiResponse.error("Failed to fetch market index data"));
        }
    }
}