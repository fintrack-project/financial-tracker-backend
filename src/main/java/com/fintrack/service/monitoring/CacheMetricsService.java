package com.fintrack.service.monitoring;

import com.fintrack.service.cache.MarketDataCacheService;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class CacheMetricsService {

    private static final Logger logger = LoggerFactory.getLogger(CacheMetricsService.class);
    
    private final MarketDataCacheService cacheService;
    private final MeterRegistry meterRegistry;

    public CacheMetricsService(MarketDataCacheService cacheService, MeterRegistry meterRegistry) {
        this.cacheService = cacheService;
        this.meterRegistry = meterRegistry;
    }

    @Scheduled(fixedRate = 60000) // Every minute
    public void recordCacheMetrics() {
        try {
            Map<String, Object> stats = cacheService.getCacheStats();
            
            // Record cache hit rate
            Double hitRate = (Double) stats.get("hitRate");
            if (hitRate != null) {
                meterRegistry.gauge("cache.hit.rate", hitRate);
            }
            
            // Record cache size
            Integer cacheSize = (Integer) stats.get("cacheSize");
            if (cacheSize != null) {
                meterRegistry.gauge("cache.size", cacheSize);
            }
            
            // Record total hits
            Long totalHits = (Long) stats.get("totalHits");
            if (totalHits != null) {
                meterRegistry.gauge("cache.hits.total", totalHits);
            }
            
            // Record total misses
            Long totalMisses = (Long) stats.get("totalMisses");
            if (totalMisses != null) {
                meterRegistry.gauge("cache.misses.total", totalMisses);
            }
            
            logger.debug("Recorded cache metrics - Hit Rate: {}, Size: {}, Hits: {}, Misses: {}", 
                hitRate, cacheSize, totalHits, totalMisses);
                
        } catch (Exception e) {
            logger.error("Error recording cache metrics", e);
        }
    }
} 