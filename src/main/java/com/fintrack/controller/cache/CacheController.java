package com.fintrack.controller.cache;

import com.fintrack.service.cache.MarketDataCacheService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * REST controller for cache management and monitoring.
 * Provides endpoints to view cache statistics and manage cache operations.
 */
@RestController
@RequestMapping("/api/cache")
public class CacheController {

    private static final Logger logger = LoggerFactory.getLogger(CacheController.class);
    
    private final MarketDataCacheService cacheService;

    public CacheController(MarketDataCacheService cacheService) {
        this.cacheService = cacheService;
    }

    /**
     * Get cache statistics including hit rate, size, and performance metrics.
     * 
     * @return Cache statistics as a map
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getCacheStats() {
        try {
            Map<String, Object> stats = cacheService.getCacheStats();
            logger.debug("Retrieved cache statistics: {}", stats);
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            logger.error("Error retrieving cache statistics", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Clear all market data cache entries.
     * 
     * @return Success response
     */
    @DeleteMapping("/clear")
    public ResponseEntity<Void> clearCache() {
        try {
            cacheService.clearCache();
            logger.info("Cache cleared successfully");
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            logger.error("Error clearing cache", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Get cache health status.
     * 
     * @return Health status response
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> getCacheHealth() {
        try {
            Map<String, Object> stats = cacheService.getCacheStats();
            Map<String, Object> health = Map.of(
                "status", "UP",
                "cacheSize", stats.getOrDefault("cacheSize", 0),
                "hitRate", stats.getOrDefault("hitRate", 0.0)
            );
            return ResponseEntity.ok(health);
        } catch (Exception e) {
            logger.error("Error checking cache health", e);
            return ResponseEntity.ok(Map.of("status", "DOWN", "error", e.getMessage()));
        }
    }
} 