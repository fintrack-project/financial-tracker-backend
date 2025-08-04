package com.fintrack.service.market;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fintrack.constants.finance.AssetType;
import com.fintrack.model.market.MarketData;
import com.fintrack.repository.market.MarketDataRepository;
import com.fintrack.service.cache.MarketDataCacheService;
import com.fintrack.util.KafkaProducerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CachedMarketDataServiceTest {

    @Mock
    private MarketDataCacheService cacheService;
    
    @Mock
    private KafkaProducerService kafkaProducerService;
    
    @Mock
    private MarketDataRepository marketDataRepository;
    
    private ObjectMapper objectMapper;
    private CachedMarketDataService cachedMarketDataService;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule()); // Fix for LocalDateTime serialization
        cachedMarketDataService = new CachedMarketDataService(
            cacheService, kafkaProducerService, marketDataRepository, objectMapper
        );
    }

    @Test
    void testHandleETLBatchCompletionMessage() throws Exception {
        // Given: ETL batch completion message with multiple market data entries
        Map<String, Object> etlMessage = new HashMap<>();
        etlMessage.put("totalBatches", 30);
        etlMessage.put("totalTransactions", 3000);
        etlMessage.put("processingTimeMs", 15000L);
        
        List<Map<String, Object>> assets = Arrays.asList(
            createAssetData("AAPL", "STOCK", new BigDecimal("150.00")),
            createAssetData("GOOGL", "STOCK", new BigDecimal("2800.00")),
            createAssetData("BTC/USD", "CRYPTO", new BigDecimal("45000.00"))
        );
        etlMessage.put("assets", assets);
        
        String messageJson = objectMapper.writeValueAsString(etlMessage);
        
        // When: Processing the ETL batch completion message
        cachedMarketDataService.onMarketDataUpdateComplete(messageJson);
        
        // Then: Cache should be updated with all market data entries
        verify(cacheService, times(1)).setMarketDataBatch(anyList());
        verify(cacheService, never()).setMarketData(any(MarketData.class));
    }

    @Test
    void testHandleETLBatchCompletionWithError() throws Exception {
        // Given: ETL batch completion message with error
        Map<String, Object> errorMessage = new HashMap<>();
        errorMessage.put("error", "Processing failed due to invalid data");
        errorMessage.put("errorType", "VALIDATION_ERROR");
        errorMessage.put("batchId", "batch-123");
        errorMessage.put("failedBatch", 15);
        
        String messageJson = objectMapper.writeValueAsString(errorMessage);
        
        // When: Processing the ETL error message
        cachedMarketDataService.onMarketDataUpdateComplete(messageJson);
        
        // Then: Cache should not be updated
        verify(cacheService, never()).setMarketDataBatch(anyList());
        verify(cacheService, never()).setMarketData(any(MarketData.class));
    }

    @Test
    void testHandleEmptyAssetsList() throws Exception {
        // Given: ETL batch completion message with empty assets
        Map<String, Object> emptyMessage = new HashMap<>();
        emptyMessage.put("totalBatches", 30);
        emptyMessage.put("totalTransactions", 3000);
        emptyMessage.put("processingTimeMs", 15000L);
        emptyMessage.put("assets", new ArrayList<>());
        
        String messageJson = objectMapper.writeValueAsString(emptyMessage);
        
        // When: Processing the ETL batch completion message
        cachedMarketDataService.onMarketDataUpdateComplete(messageJson);
        
        // Then: Cache should not be updated
        verify(cacheService, never()).setMarketDataBatch(anyList());
    }

    private Map<String, Object> createAssetData(String symbol, String assetType, BigDecimal price) {
        Map<String, Object> asset = new HashMap<>();
        asset.put("symbol", symbol);
        asset.put("assetType", assetType);
        asset.put("price", price);
        asset.put("updatedAt", LocalDateTime.now().toString());
        return asset;
    }
} 