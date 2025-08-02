package com.fintrack.service.cache;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fintrack.constants.finance.AssetType;
import com.fintrack.model.market.MarketData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MarketDataCacheServiceTest {

    @Mock
    private RedisTemplate<String, Object> valkeyTemplate;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    private ValkeyMarketDataCacheService marketDataCacheService;

    private static final String TEST_SYMBOL = "AAPL";
    private static final AssetType TEST_ASSET_TYPE = AssetType.STOCK;
    private static final UUID TEST_ACCOUNT_ID = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        marketDataCacheService = new ValkeyMarketDataCacheService(valkeyTemplate, objectMapper);
    }

    @Test
    @DisplayName("Should get market data successfully when cached")
    void shouldGetMarketDataSuccessfullyWhenCached() {
        // Given
        MarketData expectedMarketData = createTestMarketData();
        String expectedKey = "market_data:STOCK:AAPL";
        
        when(valkeyTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(expectedKey)).thenReturn(expectedMarketData);

        // When
        Optional<MarketData> result = marketDataCacheService.getMarketData(TEST_SYMBOL, TEST_ASSET_TYPE);

        // Then
        assertTrue(result.isPresent());
        assertEquals(expectedMarketData, result.get());
        verify(valueOperations).get(expectedKey);
    }

    @Test
    @DisplayName("Should return empty when market data not cached")
    void shouldReturnEmptyWhenMarketDataNotCached() {
        // Given
        String expectedKey = "market_data:STOCK:AAPL";
        when(valkeyTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(expectedKey)).thenReturn(null);

        // When
        Optional<MarketData> result = marketDataCacheService.getMarketData(TEST_SYMBOL, TEST_ASSET_TYPE);

        // Then
        assertFalse(result.isPresent());
        verify(valueOperations).get(expectedKey);
    }

    @Test
    @DisplayName("Should set market data successfully")
    void shouldSetMarketDataSuccessfully() {
        // Given
        MarketData marketData = createTestMarketData();
        String expectedKey = "market_data:STOCK:AAPL";
        when(valkeyTemplate.opsForValue()).thenReturn(valueOperations);

        // When
        marketDataCacheService.setMarketData(marketData);

        // Then
        verify(valueOperations).set(eq(expectedKey), eq(marketData), eq(Duration.ofMinutes(5)));
    }

    @Test
    @DisplayName("Should get market data batch successfully")
    void shouldGetMarketDataBatchSuccessfully() {
        // Given
        List<String> symbols = Arrays.asList("AAPL", "GOOGL", "MSFT");
        List<MarketData> expectedData = Arrays.asList(
            createTestMarketData("AAPL"),
            createTestMarketData("GOOGL"),
            createTestMarketData("MSFT")
        );
        
        List<String> expectedKeys = Arrays.asList(
            "market_data:STOCK:AAPL",
            "market_data:STOCK:GOOGL",
            "market_data:STOCK:MSFT"
        );
        
        when(valkeyTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.multiGet(expectedKeys)).thenReturn(Arrays.asList(
            expectedData.get(0), expectedData.get(1), expectedData.get(2)
        ));

        // When
        List<MarketData> result = marketDataCacheService.getMarketDataBatch(symbols, TEST_ASSET_TYPE);

        // Then
        assertEquals(3, result.size());
        assertEquals(expectedData, result);
        verify(valueOperations).multiGet(expectedKeys);
    }

    @Test
    @DisplayName("Should handle partial cache hits in batch retrieval")
    void shouldHandlePartialCacheHitsInBatchRetrieval() {
        // Given
        List<String> symbols = Arrays.asList("AAPL", "GOOGL", "MSFT");
        List<String> expectedKeys = Arrays.asList(
            "market_data:STOCK:AAPL",
            "market_data:STOCK:GOOGL",
            "market_data:STOCK:MSFT"
        );
        
        MarketData cachedData = createTestMarketData("AAPL");
        when(valkeyTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.multiGet(expectedKeys)).thenReturn(Arrays.asList(
            cachedData, null, null
        ));

        // When
        List<MarketData> result = marketDataCacheService.getMarketDataBatch(symbols, TEST_ASSET_TYPE);

        // Then
        assertEquals(1, result.size());
        assertEquals(cachedData, result.get(0));
        verify(valueOperations).multiGet(expectedKeys);
    }

    @Test
    @DisplayName("Should set market data batch successfully")
    void shouldSetMarketDataBatchSuccessfully() {
        // Given
        List<MarketData> marketDataList = Arrays.asList(
            createTestMarketData("AAPL"),
            createTestMarketData("GOOGL")
        );
        when(valkeyTemplate.opsForValue()).thenReturn(valueOperations);

        // When
        marketDataCacheService.setMarketDataBatch(marketDataList);

        // Then
        verify(valueOperations).multiSet(any(Map.class));
        verify(valkeyTemplate, times(2)).expire(anyString(), eq(Duration.ofMinutes(5)));
    }

    @Test
    @DisplayName("Should get account market data successfully")
    void shouldGetAccountMarketDataSuccessfully() {
        // Given
        String expectedKey = "account_market_data:" + TEST_ACCOUNT_ID;
        List<MarketData> expectedData = Arrays.asList(
            createTestMarketData("AAPL"),
            createTestMarketData("GOOGL")
        );
        
        when(valkeyTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(expectedKey)).thenReturn(expectedData);

        // When
        List<MarketData> result = marketDataCacheService.getAccountMarketData(TEST_ACCOUNT_ID);

        // Then
        assertEquals(expectedData, result);
        verify(valueOperations).get(expectedKey);
    }

    @Test
    @DisplayName("Should return empty list when account market data not cached")
    void shouldReturnEmptyListWhenAccountMarketDataNotCached() {
        // Given
        String expectedKey = "account_market_data:" + TEST_ACCOUNT_ID;
        when(valkeyTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(expectedKey)).thenReturn(null);

        // When
        List<MarketData> result = marketDataCacheService.getAccountMarketData(TEST_ACCOUNT_ID);

        // Then
        assertTrue(result.isEmpty());
        verify(valueOperations).get(expectedKey);
    }

    @Test
    @DisplayName("Should set account market data successfully")
    void shouldSetAccountMarketDataSuccessfully() {
        // Given
        String expectedKey = "account_market_data:" + TEST_ACCOUNT_ID;
        List<MarketData> marketDataList = Arrays.asList(
            createTestMarketData("AAPL"),
            createTestMarketData("GOOGL")
        );
        when(valkeyTemplate.opsForValue()).thenReturn(valueOperations);

        // When
        marketDataCacheService.setAccountMarketData(TEST_ACCOUNT_ID, marketDataList);

        // Then
        verify(valueOperations).set(eq(expectedKey), eq(marketDataList), eq(Duration.ofMinutes(10)));
    }

    @Test
    @DisplayName("Should invalidate market data successfully")
    void shouldInvalidateMarketDataSuccessfully() {
        // Given
        String expectedKey = "market_data:STOCK:AAPL";

        // When
        marketDataCacheService.invalidateMarketData(TEST_SYMBOL, TEST_ASSET_TYPE);

        // Then
        verify(valkeyTemplate).delete(expectedKey);
    }

    @Test
    @DisplayName("Should clear cache successfully")
    void shouldClearCacheSuccessfully() {
        // Given
        Set<String> cacheKeys = new HashSet<>(Arrays.asList(
            "market_data:STOCK:AAPL",
            "market_data:STOCK:GOOGL"
        ));
        
        when(valkeyTemplate.keys("market_data:*")).thenReturn(cacheKeys);

        // When
        marketDataCacheService.clearCache();

        // Then
        verify(valkeyTemplate).keys("market_data:*");
        verify(valkeyTemplate).delete(cacheKeys);
    }

    @Test
    @DisplayName("Should get cache stats successfully")
    void shouldGetCacheStatsSuccessfully() {
        // Given
        Set<String> hitKeys = new HashSet<>(Arrays.asList(
            "cache_stats:STOCK:AAPL:hits",
            "cache_stats:STOCK:GOOGL:hits"
        ));
        Set<String> missKeys = new HashSet<>(Arrays.asList(
            "cache_stats:STOCK:MSFT:misses"
        ));
        Set<String> marketDataKeys = new HashSet<>(Arrays.asList(
            "market_data:STOCK:AAPL",
            "market_data:STOCK:GOOGL"
        ));
        
        when(valkeyTemplate.keys("cache_stats:*:hits")).thenReturn(hitKeys);
        when(valkeyTemplate.keys("cache_stats:*:misses")).thenReturn(missKeys);
        when(valkeyTemplate.keys("market_data:*")).thenReturn(marketDataKeys);
        when(valkeyTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("cache_stats:STOCK:AAPL:hits")).thenReturn("100");
        when(valueOperations.get("cache_stats:STOCK:GOOGL:hits")).thenReturn("50");
        when(valueOperations.get("cache_stats:STOCK:MSFT:misses")).thenReturn("25");

        // When
        Map<String, Object> stats = marketDataCacheService.getCacheStats();

        // Then
        assertNotNull(stats);
        assertEquals(150L, stats.get("totalHits"));
        assertEquals(25L, stats.get("totalMisses"));
        assertEquals(0.857, (Double) stats.get("hitRate"), 0.001);
        assertEquals(2, stats.get("cacheSize"));
    }

    @Test
    @DisplayName("Should check data freshness successfully")
    void shouldCheckDataFreshnessSuccessfully() {
        // Given
        String expectedKey = "market_data:STOCK:AAPL";
        Duration maxAge = Duration.ofMinutes(2);
        
        when(valkeyTemplate.getExpire(expectedKey, java.util.concurrent.TimeUnit.SECONDS))
            .thenReturn(180L); // 3 minutes remaining

        // When
        boolean isFresh = marketDataCacheService.isDataFresh(TEST_SYMBOL, TEST_ASSET_TYPE, maxAge);

        // Then
        assertTrue(isFresh);
        verify(valkeyTemplate).getExpire(expectedKey, java.util.concurrent.TimeUnit.SECONDS);
    }

    @Test
    @DisplayName("Should return false for stale data")
    void shouldReturnFalseForStaleData() {
        // Given
        String expectedKey = "market_data:STOCK:AAPL";
        Duration maxAge = Duration.ofMinutes(2);
        
        when(valkeyTemplate.getExpire(expectedKey, java.util.concurrent.TimeUnit.SECONDS))
            .thenReturn(60L); // 1 minute remaining (stale)

        // When
        boolean isFresh = marketDataCacheService.isDataFresh(TEST_SYMBOL, TEST_ASSET_TYPE, maxAge);

        // Then
        assertFalse(isFresh);
        verify(valkeyTemplate).getExpire(expectedKey, java.util.concurrent.TimeUnit.SECONDS);
    }

    @Test
    @DisplayName("Should return false when data not found")
    void shouldReturnFalseWhenDataNotFound() {
        // Given
        String expectedKey = "market_data:STOCK:AAPL";
        Duration maxAge = Duration.ofMinutes(2);
        
        when(valkeyTemplate.getExpire(expectedKey, java.util.concurrent.TimeUnit.SECONDS))
            .thenReturn(null);

        // When
        boolean isFresh = marketDataCacheService.isDataFresh(TEST_SYMBOL, TEST_ASSET_TYPE, maxAge);

        // Then
        assertFalse(isFresh);
        verify(valkeyTemplate).getExpire(expectedKey, java.util.concurrent.TimeUnit.SECONDS);
    }

    @Test
    @DisplayName("Should handle cache operations with different asset types")
    void shouldHandleCacheOperationsWithDifferentAssetTypes() {
        // Given
        AssetType cryptoType = AssetType.CRYPTO;
        String cryptoSymbol = "BTC";
        MarketData cryptoData = createTestMarketData(cryptoSymbol, cryptoType);
        String expectedKey = "market_data:CRYPTO:BTC";
        
        when(valkeyTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(expectedKey)).thenReturn(cryptoData);

        // When
        Optional<MarketData> result = marketDataCacheService.getMarketData(cryptoSymbol, cryptoType);

        // Then
        assertTrue(result.isPresent());
        assertEquals(cryptoData, result.get());
        verify(valueOperations).get(expectedKey);
    }

    @Test
    @DisplayName("Should handle empty batch operations")
    void shouldHandleEmptyBatchOperations() {
        // Given
        List<String> emptySymbols = new ArrayList<>();
        List<MarketData> emptyData = new ArrayList<>();
        when(valkeyTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.multiGet(any())).thenReturn(new ArrayList<>());

        // When
        List<MarketData> getResult = marketDataCacheService.getMarketDataBatch(emptySymbols, TEST_ASSET_TYPE);
        marketDataCacheService.setMarketDataBatch(emptyData);

        // Then
        assertTrue(getResult.isEmpty());
        verify(valueOperations).multiGet(any());
        verify(valueOperations).multiSet(any());
    }

    @Test
    @DisplayName("Should handle cache operations with null values gracefully")
    void shouldHandleCacheOperationsWithNullValuesGracefully() {
        // Given
        String expectedKey = "market_data:STOCK:AAPL";
        when(valkeyTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(expectedKey)).thenThrow(new RuntimeException("Cache error"));

        // When & Then
        assertDoesNotThrow(() -> {
            Optional<MarketData> result = marketDataCacheService.getMarketData(TEST_SYMBOL, TEST_ASSET_TYPE);
            assertFalse(result.isPresent());
        });
    }

    @Test
    @DisplayName("Should validate cache key generation")
    void shouldValidateCacheKeyGeneration() {
        // Given
        String symbol = "TEST";
        AssetType assetType = AssetType.FOREX;
        String expectedKey = "market_data:FOREX:TEST";
        
        when(valkeyTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(expectedKey)).thenReturn(null);

        // When
        marketDataCacheService.getMarketData(symbol, assetType);

        // Then
        verify(valueOperations).get(expectedKey);
    }

    @Test
    @DisplayName("Should validate account cache key generation")
    void shouldValidateAccountCacheKeyGeneration() {
        // Given
        UUID accountId = UUID.randomUUID();
        String expectedKey = "account_market_data:" + accountId;
        
        when(valkeyTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(expectedKey)).thenReturn(new ArrayList<>());

        // When
        marketDataCacheService.getAccountMarketData(accountId);

        // Then
        verify(valueOperations).get(expectedKey);
    }

    private MarketData createTestMarketData() {
        return createTestMarketData(TEST_SYMBOL, TEST_ASSET_TYPE);
    }

    private MarketData createTestMarketData(String symbol) {
        return createTestMarketData(symbol, TEST_ASSET_TYPE);
    }

    private MarketData createTestMarketData(String symbol, AssetType assetType) {
        MarketData marketData = new MarketData();
        marketData.setSymbol(symbol);
        marketData.setAssetType(assetType);
        marketData.setPrice(new BigDecimal("150.00"));
        marketData.setChange(new BigDecimal("2.50"));
        marketData.setPercentChange(new BigDecimal("1.67"));
        marketData.setHigh(new BigDecimal("155.00"));
        marketData.setLow(new BigDecimal("145.00"));
        marketData.setUpdatedAt(LocalDateTime.now());
        return marketData;
    }
} 