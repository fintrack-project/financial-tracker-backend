package com.fintrack.service.market;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("MarketDataCacheListener Tests")
class MarketDataCacheListenerTest {

    @Mock
    private CachedMarketDataService cachedMarketDataService;

    @Mock
    private ObjectMapper objectMapper;

    private MarketDataCacheListener marketDataCacheListener;

    private static final String TEST_SYMBOL = "AAPL";
    private static final String TEST_ASSET_TYPE = "STOCK";

    @BeforeEach
    void setUp() {
        marketDataCacheListener = new MarketDataCacheListener(cachedMarketDataService, objectMapper);
    }

    @Test
    @DisplayName("Should handle full refresh message successfully")
    void shouldHandleFullRefreshMessageSuccessfully() throws Exception {
        // Given: Full refresh message
        String message = "{\"full_refresh\": true}";
        Map<String, Object> completionData = new HashMap<>();
        completionData.put("full_refresh", true);
        
        when(objectMapper.readValue(message, Map.class)).thenReturn(completionData);
        doNothing().when(cachedMarketDataService).evictAllMarketDataCache();

        // When: Handling market data update complete message
        marketDataCacheListener.handleMarketDataUpdateComplete(message);

        // Then: Should invalidate all caches
        verify(objectMapper).readValue(message, Map.class);
        verify(cachedMarketDataService).evictAllMarketDataCache();
        verifyNoMoreInteractions(cachedMarketDataService);
    }

    @Test
    @DisplayName("Should handle selective cache invalidation for specific assets")
    void shouldHandleSelectiveCacheInvalidationForSpecificAssets() throws Exception {
        // Given: Message with specific assets
        String message = "{\"assets\":[{\"symbol\":\"AAPL\",\"asset_type\":\"STOCK\"},{\"symbol\":\"GOOGL\",\"asset_type\":\"STOCK\"}]}";
        Map<String, Object> completionData = new HashMap<>();
        List<Map<String, String>> assets = Arrays.asList(
            createAssetMap("AAPL", "STOCK"),
            createAssetMap("GOOGL", "STOCK")
        );
        completionData.put("full_refresh", false);
        completionData.put("assets", assets);
        
        when(objectMapper.readValue(message, Map.class)).thenReturn(completionData);
        doNothing().when(cachedMarketDataService).evictMarketDataCache(anyString(), anyString());

        // When: Handling market data update complete message
        marketDataCacheListener.handleMarketDataUpdateComplete(message);

        // Then: Should invalidate cache for each asset
        verify(objectMapper).readValue(message, Map.class);
        verify(cachedMarketDataService).evictMarketDataCache("AAPL", "STOCK");
        verify(cachedMarketDataService).evictMarketDataCache("GOOGL", "STOCK");
        verifyNoMoreInteractions(cachedMarketDataService);
    }

    @Test
    @DisplayName("Should handle message with null assets")
    void shouldHandleMessageWithNullAssets() throws Exception {
        // Given: Message with null assets
        String message = "{\"full_refresh\": false, \"assets\": null}";
        Map<String, Object> completionData = new HashMap<>();
        completionData.put("full_refresh", false);
        completionData.put("assets", null);
        
        when(objectMapper.readValue(message, Map.class)).thenReturn(completionData);

        // When: Handling market data update complete message
        marketDataCacheListener.handleMarketDataUpdateComplete(message);

        // Then: Should not invalidate any caches
        verify(objectMapper).readValue(message, Map.class);
        verifyNoInteractions(cachedMarketDataService);
    }

    @Test
    @DisplayName("Should handle message with empty assets list")
    void shouldHandleMessageWithEmptyAssetsList() throws Exception {
        // Given: Message with empty assets list
        String message = "{\"full_refresh\": false, \"assets\": []}";
        Map<String, Object> completionData = new HashMap<>();
        completionData.put("full_refresh", false);
        completionData.put("assets", Arrays.asList());
        
        when(objectMapper.readValue(message, Map.class)).thenReturn(completionData);

        // When: Handling market data update complete message
        marketDataCacheListener.handleMarketDataUpdateComplete(message);

        // Then: Should not invalidate any caches
        verify(objectMapper).readValue(message, Map.class);
        verifyNoInteractions(cachedMarketDataService);
    }

    @Test
    @DisplayName("Should handle asset with null symbol")
    void shouldHandleAssetWithNullSymbol() throws Exception {
        // Given: Message with asset having null symbol
        String message = "{\"assets\":[{\"symbol\":null,\"asset_type\":\"STOCK\"}]}";
        Map<String, Object> completionData = new HashMap<>();
        List<Map<String, String>> assets = Arrays.asList(
            createAssetMap(null, "STOCK")
        );
        completionData.put("full_refresh", false);
        completionData.put("assets", assets);
        
        when(objectMapper.readValue(message, Map.class)).thenReturn(completionData);

        // When: Handling market data update complete message
        marketDataCacheListener.handleMarketDataUpdateComplete(message);

        // Then: Should not invalidate cache for null symbol
        verify(objectMapper).readValue(message, Map.class);
        verifyNoInteractions(cachedMarketDataService);
    }

    @Test
    @DisplayName("Should handle asset with null asset type")
    void shouldHandleAssetWithNullAssetType() throws Exception {
        // Given: Message with asset having null asset type
        String message = "{\"assets\":[{\"symbol\":\"AAPL\",\"asset_type\":null}]}";
        Map<String, Object> completionData = new HashMap<>();
        List<Map<String, String>> assets = Arrays.asList(
            createAssetMap("AAPL", null)
        );
        completionData.put("full_refresh", false);
        completionData.put("assets", assets);
        
        when(objectMapper.readValue(message, Map.class)).thenReturn(completionData);

        // When: Handling market data update complete message
        marketDataCacheListener.handleMarketDataUpdateComplete(message);

        // Then: Should not invalidate cache for null asset type
        verify(objectMapper).readValue(message, Map.class);
        verifyNoInteractions(cachedMarketDataService);
    }

    @Test
    @DisplayName("Should handle asset with both null symbol and asset type")
    void shouldHandleAssetWithBothNullSymbolAndAssetType() throws Exception {
        // Given: Message with asset having both null symbol and asset type
        String message = "{\"assets\":[{\"symbol\":null,\"asset_type\":null}]}";
        Map<String, Object> completionData = new HashMap<>();
        List<Map<String, String>> assets = Arrays.asList(
            createAssetMap(null, null)
        );
        completionData.put("full_refresh", false);
        completionData.put("assets", assets);
        
        when(objectMapper.readValue(message, Map.class)).thenReturn(completionData);

        // When: Handling market data update complete message
        marketDataCacheListener.handleMarketDataUpdateComplete(message);

        // Then: Should not invalidate cache
        verify(objectMapper).readValue(message, Map.class);
        verifyNoInteractions(cachedMarketDataService);
    }

    @Test
    @DisplayName("Should handle mixed valid and invalid assets")
    void shouldHandleMixedValidAndInvalidAssets() throws Exception {
        // Given: Message with mixed valid and invalid assets
        String message = "{\"assets\":[{\"symbol\":\"AAPL\",\"asset_type\":\"STOCK\"},{\"symbol\":null,\"asset_type\":\"STOCK\"},{\"symbol\":\"GOOGL\",\"asset_type\":null}]}";
        Map<String, Object> completionData = new HashMap<>();
        List<Map<String, String>> assets = Arrays.asList(
            createAssetMap("AAPL", "STOCK"),
            createAssetMap(null, "STOCK"),
            createAssetMap("GOOGL", null)
        );
        completionData.put("full_refresh", false);
        completionData.put("assets", assets);
        
        when(objectMapper.readValue(message, Map.class)).thenReturn(completionData);
        doNothing().when(cachedMarketDataService).evictMarketDataCache(anyString(), anyString());

        // When: Handling market data update complete message
        marketDataCacheListener.handleMarketDataUpdateComplete(message);

        // Then: Should only invalidate cache for valid asset
        verify(objectMapper).readValue(message, Map.class);
        verify(cachedMarketDataService).evictMarketDataCache("AAPL", "STOCK");
        verifyNoMoreInteractions(cachedMarketDataService);
    }

    @Test
    @DisplayName("Should handle JSON parsing exception")
    void shouldHandleJsonParsingException() throws Exception {
        // Given: Invalid JSON message
        String message = "invalid json message";
        when(objectMapper.readValue(message, Map.class)).thenThrow(new RuntimeException("JSON parsing error"));

        // When: Handling market data update complete message
        marketDataCacheListener.handleMarketDataUpdateComplete(message);

        // Then: Should handle exception gracefully
        verify(objectMapper).readValue(message, Map.class);
        verifyNoInteractions(cachedMarketDataService);
    }

    @Test
    @DisplayName("Should handle null message")
    void shouldHandleNullMessage() throws Exception {
        // Given: Null message
        String message = null;
        when(objectMapper.readValue(message, Map.class)).thenReturn(null);

        // When: Handling market data update complete message
        marketDataCacheListener.handleMarketDataUpdateComplete(message);

        // Then: Should handle gracefully
        verify(objectMapper).readValue(message, Map.class);
        verifyNoInteractions(cachedMarketDataService);
    }

    @Test
    @DisplayName("Should handle empty message")
    void shouldHandleEmptyMessage() throws Exception {
        // Given: Empty message
        String message = "";
        when(objectMapper.readValue(message, Map.class)).thenReturn(null);

        // When: Handling market data update complete message
        marketDataCacheListener.handleMarketDataUpdateComplete(message);

        // Then: Should handle gracefully
        verify(objectMapper).readValue(message, Map.class);
        verifyNoInteractions(cachedMarketDataService);
    }

    @Test
    @DisplayName("Should handle message without full_refresh field")
    void shouldHandleMessageWithoutFullRefreshField() throws Exception {
        // Given: Message without full_refresh field
        String message = "{\"assets\":[{\"symbol\":\"AAPL\",\"asset_type\":\"STOCK\"}]}";
        Map<String, Object> completionData = new HashMap<>();
        List<Map<String, String>> assets = Arrays.asList(
            createAssetMap("AAPL", "STOCK")
        );
        completionData.put("assets", assets);
        
        when(objectMapper.readValue(message, Map.class)).thenReturn(completionData);
        doNothing().when(cachedMarketDataService).evictMarketDataCache(anyString(), anyString());

        // When: Handling market data update complete message
        marketDataCacheListener.handleMarketDataUpdateComplete(message);

        // Then: Should invalidate cache for the asset
        verify(objectMapper).readValue(message, Map.class);
        verify(cachedMarketDataService).evictMarketDataCache("AAPL", "STOCK");
        verifyNoMoreInteractions(cachedMarketDataService);
    }

    @Test
    @DisplayName("Should handle message without assets field")
    void shouldHandleMessageWithoutAssetsField() throws Exception {
        // Given: Message without assets field
        String message = "{\"full_refresh\": false}";
        Map<String, Object> completionData = new HashMap<>();
        completionData.put("full_refresh", false);
        
        when(objectMapper.readValue(message, Map.class)).thenReturn(completionData);

        // When: Handling market data update complete message
        marketDataCacheListener.handleMarketDataUpdateComplete(message);

        // Then: Should not invalidate any caches
        verify(objectMapper).readValue(message, Map.class);
        verifyNoInteractions(cachedMarketDataService);
    }

    private Map<String, String> createAssetMap(String symbol, String assetType) {
        Map<String, String> asset = new HashMap<>();
        asset.put("symbol", symbol);
        asset.put("asset_type", assetType);
        return asset;
    }
} 