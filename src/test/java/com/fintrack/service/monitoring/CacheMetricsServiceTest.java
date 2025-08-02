package com.fintrack.service.monitoring;

import com.fintrack.service.cache.MarketDataCacheService;
import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CacheMetricsService Tests")
class CacheMetricsServiceTest {

    @Mock
    private MarketDataCacheService cacheService;

    @Mock
    private MeterRegistry meterRegistry;

    private CacheMetricsService cacheMetricsService;

    @BeforeEach
    void setUp() {
        cacheMetricsService = new CacheMetricsService(cacheService, meterRegistry);
    }

    @Test
    @DisplayName("Should record cache metrics successfully")
    void shouldRecordCacheMetricsSuccessfully() {
        // Given: Valid cache stats
        Map<String, Object> stats = new HashMap<>();
        stats.put("hitRate", 0.85);
        stats.put("cacheSize", 1000);
        stats.put("totalHits", 8500L);
        stats.put("totalMisses", 1500L);

        when(cacheService.getCacheStats()).thenReturn(stats);

        // When: Record cache metrics
        assertDoesNotThrow(() -> {
            cacheMetricsService.recordCacheMetrics();
        });

        // Then: Should record all metrics
        verify(meterRegistry).gauge("cache.hit.rate", 0.85);
        verify(meterRegistry).gauge("cache.size", 1000);
        verify(meterRegistry).gauge("cache.hits.total", 8500L);
        verify(meterRegistry).gauge("cache.misses.total", 1500L);
    }

    @Test
    @DisplayName("Should handle null hit rate gracefully")
    void shouldHandleNullHitRateGracefully() {
        // Given: Cache stats with null hit rate
        Map<String, Object> stats = new HashMap<>();
        stats.put("hitRate", null);
        stats.put("cacheSize", 500);
        stats.put("totalHits", 4000L);
        stats.put("totalMisses", 1000L);

        when(cacheService.getCacheStats()).thenReturn(stats);

        // When: Record cache metrics
        assertDoesNotThrow(() -> {
            cacheMetricsService.recordCacheMetrics();
        });

        // Then: Should record other metrics but skip hit rate
        verify(meterRegistry, never()).gauge(eq("cache.hit.rate"), any());
        verify(meterRegistry).gauge(eq("cache.size"), eq(500));
        verify(meterRegistry).gauge(eq("cache.hits.total"), eq(4000L));
        verify(meterRegistry).gauge(eq("cache.misses.total"), eq(1000L));
    }

    @Test
    @DisplayName("Should handle null cache size gracefully")
    void shouldHandleNullCacheSizeGracefully() {
        // Given: Cache stats with null cache size
        Map<String, Object> stats = new HashMap<>();
        stats.put("hitRate", 0.75);
        stats.put("cacheSize", null);
        stats.put("totalHits", 3000L);
        stats.put("totalMisses", 1000L);

        when(cacheService.getCacheStats()).thenReturn(stats);

        // When: Record cache metrics
        assertDoesNotThrow(() -> {
            cacheMetricsService.recordCacheMetrics();
        });

        // Then: Should record other metrics but skip cache size
        verify(meterRegistry).gauge(eq("cache.hit.rate"), eq(0.75));
        verify(meterRegistry, never()).gauge(eq("cache.size"), any());
        verify(meterRegistry).gauge(eq("cache.hits.total"), eq(3000L));
        verify(meterRegistry).gauge(eq("cache.misses.total"), eq(1000L));
    }

    @Test
    @DisplayName("Should handle null total hits gracefully")
    void shouldHandleNullTotalHitsGracefully() {
        // Given: Cache stats with null total hits
        Map<String, Object> stats = new HashMap<>();
        stats.put("hitRate", 0.80);
        stats.put("cacheSize", 800);
        stats.put("totalHits", null);
        stats.put("totalMisses", 500L);

        when(cacheService.getCacheStats()).thenReturn(stats);

        // When: Record cache metrics
        assertDoesNotThrow(() -> {
            cacheMetricsService.recordCacheMetrics();
        });

        // Then: Should record other metrics but skip total hits
        verify(meterRegistry).gauge(eq("cache.hit.rate"), eq(0.80));
        verify(meterRegistry).gauge(eq("cache.size"), eq(800));
        verify(meterRegistry, never()).gauge(eq("cache.hits.total"), any());
        verify(meterRegistry).gauge(eq("cache.misses.total"), eq(500L));
    }

    @Test
    @DisplayName("Should handle null total misses gracefully")
    void shouldHandleNullTotalMissesGracefully() {
        // Given: Cache stats with null total misses
        Map<String, Object> stats = new HashMap<>();
        stats.put("hitRate", 0.90);
        stats.put("cacheSize", 1200);
        stats.put("totalHits", 9000L);
        stats.put("totalMisses", null);

        when(cacheService.getCacheStats()).thenReturn(stats);

        // When: Record cache metrics
        assertDoesNotThrow(() -> {
            cacheMetricsService.recordCacheMetrics();
        });

        // Then: Should record other metrics but skip total misses
        verify(meterRegistry).gauge(eq("cache.hit.rate"), eq(0.90));
        verify(meterRegistry).gauge(eq("cache.size"), eq(1200));
        verify(meterRegistry).gauge(eq("cache.hits.total"), eq(9000L));
        verify(meterRegistry, never()).gauge(eq("cache.misses.total"), any());
    }

    @Test
    @DisplayName("Should handle empty cache stats gracefully")
    void shouldHandleEmptyCacheStatsGracefully() {
        // Given: Empty cache stats
        Map<String, Object> stats = new HashMap<>();

        when(cacheService.getCacheStats()).thenReturn(stats);

        // When: Record cache metrics
        assertDoesNotThrow(() -> {
            cacheMetricsService.recordCacheMetrics();
        });

        // Then: Should not record any metrics
        verify(meterRegistry, never()).gauge(anyString(), any());
    }

    @Test
    @DisplayName("Should handle cache service exception gracefully")
    void shouldHandleCacheServiceExceptionGracefully() {
        // Given: Cache service throws exception
        when(cacheService.getCacheStats()).thenThrow(new RuntimeException("Cache service error"));

        // When: Record cache metrics
        assertDoesNotThrow(() -> {
            cacheMetricsService.recordCacheMetrics();
        });

        // Then: Should not record any metrics
        verify(meterRegistry, never()).gauge(anyString(), any());
    }

    @Test
    @DisplayName("Should validate cache hit rate range")
    void shouldValidateCacheHitRateRange() {
        // Given: Different hit rates
        Double[] hitRates = {0.0, 0.25, 0.50, 0.75, 1.0};

        for (Double hitRate : hitRates) {
            Map<String, Object> stats = new HashMap<>();
            stats.put("hitRate", hitRate);
            stats.put("cacheSize", 100);
            stats.put("totalHits", 1000L);
            stats.put("totalMisses", 100L);

            when(cacheService.getCacheStats()).thenReturn(stats);

            // When: Record cache metrics
            assertDoesNotThrow(() -> {
                cacheMetricsService.recordCacheMetrics();
            });

            // Then: Should record hit rate correctly
            verify(meterRegistry).gauge(eq("cache.hit.rate"), eq(hitRate));
        }
    }

    @Test
    @DisplayName("Should validate cache size range")
    void shouldValidateCacheSizeRange() {
        // Given: Different cache sizes
        Integer[] cacheSizes = {0, 100, 1000, 10000, 100000};

        for (Integer cacheSize : cacheSizes) {
            Map<String, Object> stats = new HashMap<>();
            stats.put("hitRate", 0.80);
            stats.put("cacheSize", cacheSize);
            stats.put("totalHits", 8000L);
            stats.put("totalMisses", 2000L);

            when(cacheService.getCacheStats()).thenReturn(stats);

            // When: Record cache metrics
            assertDoesNotThrow(() -> {
                cacheMetricsService.recordCacheMetrics();
            });

            // Then: Should record cache size correctly
            verify(meterRegistry).gauge(eq("cache.size"), eq(cacheSize));
        }
    }

    @Test
    @DisplayName("Should validate total hits range")
    void shouldValidateTotalHitsRange() {
        // Given: Different total hits
        Long[] totalHits = {0L, 100L, 1000L, 10000L, 100000L};

        for (Long hits : totalHits) {
            Map<String, Object> stats = new HashMap<>();
            stats.put("hitRate", 0.75);
            stats.put("cacheSize", 500);
            stats.put("totalHits", hits);
            stats.put("totalMisses", 1000L);

            when(cacheService.getCacheStats()).thenReturn(stats);

            // When: Record cache metrics
            assertDoesNotThrow(() -> {
                cacheMetricsService.recordCacheMetrics();
            });

            // Then: Should record total hits correctly
            verify(meterRegistry).gauge(eq("cache.hits.total"), eq(hits));
        }
    }

    @Test
    @DisplayName("Should validate total misses range")
    void shouldValidateTotalMissesRange() {
        // Given: Different total misses
        Long[] totalMisses = {0L, 50L, 500L, 5000L, 50000L};

        for (Long misses : totalMisses) {
            Map<String, Object> stats = new HashMap<>();
            stats.put("hitRate", 0.70);
            stats.put("cacheSize", 300);
            stats.put("totalHits", 7000L);
            stats.put("totalMisses", misses);

            when(cacheService.getCacheStats()).thenReturn(stats);

            // When: Record cache metrics
            assertDoesNotThrow(() -> {
                cacheMetricsService.recordCacheMetrics();
            });

            // Then: Should record total misses correctly
            verify(meterRegistry).gauge(eq("cache.misses.total"), eq(misses));
        }
    }

    @Test
    @DisplayName("Should handle partial cache stats")
    void shouldHandlePartialCacheStats() {
        // Given: Partial cache stats (only some metrics available)
        Map<String, Object> stats = new HashMap<>();
        stats.put("hitRate", 0.85);
        stats.put("cacheSize", 750);
        // totalHits and totalMisses are missing

        when(cacheService.getCacheStats()).thenReturn(stats);

        // When: Record cache metrics
        assertDoesNotThrow(() -> {
            cacheMetricsService.recordCacheMetrics();
        });

        // Then: Should record available metrics only
        verify(meterRegistry).gauge(eq("cache.hit.rate"), eq(0.85));
        verify(meterRegistry).gauge(eq("cache.size"), eq(750));
        verify(meterRegistry, never()).gauge(eq("cache.hits.total"), any());
        verify(meterRegistry, never()).gauge(eq("cache.misses.total"), any());
    }

    @Test
    @DisplayName("Should validate metric names")
    void shouldValidateMetricNames() {
        // Given: Valid cache stats
        Map<String, Object> stats = new HashMap<>();
        stats.put("hitRate", 0.80);
        stats.put("cacheSize", 1000);
        stats.put("totalHits", 8000L);
        stats.put("totalMisses", 2000L);

        when(cacheService.getCacheStats()).thenReturn(stats);

        // When: Record cache metrics
        assertDoesNotThrow(() -> {
            cacheMetricsService.recordCacheMetrics();
        });

        // Then: Should use correct metric names
        verify(meterRegistry).gauge(eq("cache.hit.rate"), eq(0.80));
        verify(meterRegistry).gauge(eq("cache.size"), eq(1000));
        verify(meterRegistry).gauge(eq("cache.hits.total"), eq(8000L));
        verify(meterRegistry).gauge(eq("cache.misses.total"), eq(2000L));
    }

    @Test
    @DisplayName("Should handle zero values correctly")
    void shouldHandleZeroValuesCorrectly() {
        // Given: Cache stats with zero values
        Map<String, Object> stats = new HashMap<>();
        stats.put("hitRate", 0.0);
        stats.put("cacheSize", 0);
        stats.put("totalHits", 0L);
        stats.put("totalMisses", 0L);

        when(cacheService.getCacheStats()).thenReturn(stats);

        // When: Record cache metrics
        assertDoesNotThrow(() -> {
            cacheMetricsService.recordCacheMetrics();
        });

        // Then: Should record zero values correctly
        verify(meterRegistry).gauge(eq("cache.hit.rate"), eq(0.0));
        verify(meterRegistry).gauge(eq("cache.size"), eq(0));
        verify(meterRegistry).gauge(eq("cache.hits.total"), eq(0L));
        verify(meterRegistry).gauge(eq("cache.misses.total"), eq(0L));
    }

    @Test
    @DisplayName("Should handle maximum values correctly")
    void shouldHandleMaximumValuesCorrectly() {
        // Given: Cache stats with maximum values
        Map<String, Object> stats = new HashMap<>();
        stats.put("hitRate", 1.0);
        stats.put("cacheSize", Integer.MAX_VALUE);
        stats.put("totalHits", Long.MAX_VALUE);
        stats.put("totalMisses", Long.MAX_VALUE);

        when(cacheService.getCacheStats()).thenReturn(stats);

        // When: Record cache metrics
        assertDoesNotThrow(() -> {
            cacheMetricsService.recordCacheMetrics();
        });

        // Then: Should record maximum values correctly
        verify(meterRegistry).gauge(eq("cache.hit.rate"), eq(1.0));
        verify(meterRegistry).gauge(eq("cache.size"), eq(Integer.MAX_VALUE));
        verify(meterRegistry).gauge(eq("cache.hits.total"), eq(Long.MAX_VALUE));
        verify(meterRegistry).gauge(eq("cache.misses.total"), eq(Long.MAX_VALUE));
    }

    @Test
    @DisplayName("Should validate cache metrics service constructor")
    void shouldValidateCacheMetricsServiceConstructor() {
        // Given: Valid dependencies
        MarketDataCacheService mockCacheService = mock(MarketDataCacheService.class);
        MeterRegistry mockMeterRegistry = mock(MeterRegistry.class);

        // When: Create cache metrics service
        CacheMetricsService service = new CacheMetricsService(mockCacheService, mockMeterRegistry);

        // Then: Should be created successfully
        assertNotNull(service);
    }

    @Test
    @DisplayName("Should validate cache stats structure")
    void shouldValidateCacheStatsStructure() {
        // Given: Cache stats with all expected fields
        Map<String, Object> stats = new HashMap<>();
        stats.put("hitRate", 0.75);
        stats.put("cacheSize", 500);
        stats.put("totalHits", 7500L);
        stats.put("totalMisses", 2500L);

        // When & Then: Should have correct data types
        assertTrue(stats.get("hitRate") instanceof Double);
        assertTrue(stats.get("cacheSize") instanceof Integer);
        assertTrue(stats.get("totalHits") instanceof Long);
        assertTrue(stats.get("totalMisses") instanceof Long);
    }
} 