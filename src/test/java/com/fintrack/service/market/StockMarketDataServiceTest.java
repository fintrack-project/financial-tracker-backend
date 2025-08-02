package com.fintrack.service.market;

import com.fintrack.constants.finance.AssetType;
import com.fintrack.repository.finance.HoldingsMonthlyRepository;
import com.fintrack.util.KafkaProducerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("StockMarketDataService Tests")
class StockMarketDataServiceTest {

    @Mock
    private CachedMarketDataService cachedMarketDataService;

    @Mock
    private HoldingsMonthlyRepository holdingsMonthlyRepository;

    @Mock
    private KafkaProducerService kafkaProducerService;

    private StockMarketDataService stockMarketDataService;

    private static final List<String> TEST_SYMBOLS = Arrays.asList("AAPL", "GOOGL", "MSFT");
    private static final String TEST_MESSAGE = "{\"assets\":[{\"symbol\":\"AAPL\",\"asset_type\":\"STOCK\"},{\"symbol\":\"GOOGL\",\"asset_type\":\"STOCK\"}]}";

    @BeforeEach
    void setUp() {
        stockMarketDataService = new StockMarketDataService(
            cachedMarketDataService,
            holdingsMonthlyRepository,
            kafkaProducerService
        );
    }

    @Test
    @DisplayName("Should return correct asset type")
    void shouldReturnCorrectAssetType() {
        // When: Getting asset type
        AssetType result = stockMarketDataService.getAssetType();

        // Then: Should return STOCK
        assertEquals(AssetType.STOCK, result);
    }

    @Test
    @DisplayName("Should process stock symbols successfully")
    void shouldProcessStockSymbolsSuccessfully() {
        // Given: Valid stock symbols
        List<String> symbols = Arrays.asList("AAPL", "GOOGL", "MSFT");

        // When: Processing symbols
        List<Map<String, String>> result = stockMarketDataService.processSymbols(symbols);

        // Then: Should return processed assets
        assertNotNull(result);
        assertEquals(3, result.size());
        
        // Verify first asset
        Map<String, String> firstAsset = result.get(0);
        assertEquals("AAPL", firstAsset.get("symbol"));
        assertEquals("STOCK", firstAsset.get("asset_type"));
        
        // Verify second asset
        Map<String, String> secondAsset = result.get(1);
        assertEquals("GOOGL", secondAsset.get("symbol"));
        assertEquals("STOCK", secondAsset.get("asset_type"));
        
        // Verify third asset
        Map<String, String> thirdAsset = result.get(2);
        assertEquals("MSFT", thirdAsset.get("symbol"));
        assertEquals("STOCK", thirdAsset.get("asset_type"));
    }

    @Test
    @DisplayName("Should handle empty symbols list")
    void shouldHandleEmptySymbolsList() {
        // Given: Empty symbols list
        List<String> symbols = Arrays.asList();

        // When: Processing symbols
        List<Map<String, String>> result = stockMarketDataService.processSymbols(symbols);

        // Then: Should return empty list
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("Should handle single symbol")
    void shouldHandleSingleSymbol() {
        // Given: Single symbol
        List<String> symbols = Arrays.asList("AAPL");

        // When: Processing symbols
        List<Map<String, String>> result = stockMarketDataService.processSymbols(symbols);

        // Then: Should return single asset
        assertNotNull(result);
        assertEquals(1, result.size());
        
        Map<String, String> asset = result.get(0);
        assertEquals("AAPL", asset.get("symbol"));
        assertEquals("STOCK", asset.get("asset_type"));
    }

    @Test
    @DisplayName("Should handle null symbols list")
    void shouldHandleNullSymbolsList() {
        // Given: Null symbols list
        List<String> symbols = null;

        // When & Then: Should throw exception
        assertThrows(
            NullPointerException.class,
            () -> stockMarketDataService.processSymbols(symbols)
        );
    }

    @Test
    @DisplayName("Should handle symbols with special characters")
    void shouldHandleSymbolsWithSpecialCharacters() {
        // Given: Symbols with special characters
        List<String> symbols = Arrays.asList("BRK.A", "BRK-B", "AT&T");

        // When: Processing symbols
        List<Map<String, String>> result = stockMarketDataService.processSymbols(symbols);

        // Then: Should process correctly
        assertNotNull(result);
        assertEquals(3, result.size());
        
        assertEquals("BRK.A", result.get(0).get("symbol"));
        assertEquals("BRK-B", result.get(1).get("symbol"));
        assertEquals("AT&T", result.get(2).get("symbol"));
        
        // All should have STOCK asset type
        result.forEach(asset -> assertEquals("STOCK", asset.get("asset_type")));
    }

    @Test
    @DisplayName("Should handle large number of symbols")
    void shouldHandleLargeNumberOfSymbols() {
        // Given: Large number of symbols
        List<String> symbols = Arrays.asList(
            "AAPL", "GOOGL", "MSFT", "AMZN", "TSLA", "META", "NVDA", "NFLX", "ADBE", "CRM"
        );

        // When: Processing symbols
        List<Map<String, String>> result = stockMarketDataService.processSymbols(symbols);

        // Then: Should process all symbols
        assertNotNull(result);
        assertEquals(10, result.size());
        
        // Verify all have correct asset type
        result.forEach(asset -> assertEquals("STOCK", asset.get("asset_type")));
        
        // Verify symbols are preserved
        for (int i = 0; i < symbols.size(); i++) {
            assertEquals(symbols.get(i), result.get(i).get("symbol"));
        }
    }

    @Test
    @DisplayName("Should handle market data update complete message successfully")
    void shouldHandleMarketDataUpdateCompleteMessageSuccessfully() {
        // Given: Valid market data update message
        String message = "{\"assets\":[{\"symbol\":\"AAPL\",\"asset_type\":\"STOCK\"},{\"symbol\":\"GOOGL\",\"asset_type\":\"STOCK\"}]}";

        // When: Processing market data update
        stockMarketDataService.onMarketDataUpdateComplete(message);

        // Then: Should process without exception
        // Note: This method logs but doesn't return anything, so we just verify it doesn't throw
        assertDoesNotThrow(() -> stockMarketDataService.onMarketDataUpdateComplete(message));
    }

    @Test
    @DisplayName("Should handle market data update with non-stock assets")
    void shouldHandleMarketDataUpdateWithNonStockAssets() {
        // Given: Message with mixed asset types
        String message = "{\"assets\":[{\"symbol\":\"AAPL\",\"asset_type\":\"STOCK\"},{\"symbol\":\"BTC\",\"asset_type\":\"CRYPTO\"}]}";

        // When: Processing market data update
        stockMarketDataService.onMarketDataUpdateComplete(message);

        // Then: Should process without exception
        assertDoesNotThrow(() -> stockMarketDataService.onMarketDataUpdateComplete(message));
    }

    @Test
    @DisplayName("Should handle invalid JSON message")
    void shouldHandleInvalidJsonMessage() {
        // Given: Invalid JSON message
        String message = "invalid json message";

        // When: Processing market data update
        stockMarketDataService.onMarketDataUpdateComplete(message);

        // Then: Should handle gracefully without throwing exception
        assertDoesNotThrow(() -> stockMarketDataService.onMarketDataUpdateComplete(message));
    }

    @Test
    @DisplayName("Should handle empty message")
    void shouldHandleEmptyMessage() {
        // Given: Empty message
        String message = "";

        // When: Processing market data update
        stockMarketDataService.onMarketDataUpdateComplete(message);

        // Then: Should handle gracefully
        assertDoesNotThrow(() -> stockMarketDataService.onMarketDataUpdateComplete(message));
    }

    @Test
    @DisplayName("Should handle message without assets")
    void shouldHandleMessageWithoutAssets() {
        // Given: Message without assets
        String message = "{\"other_field\":\"value\"}";

        // When: Processing market data update
        stockMarketDataService.onMarketDataUpdateComplete(message);

        // Then: Should handle gracefully
        assertDoesNotThrow(() -> stockMarketDataService.onMarketDataUpdateComplete(message));
    }

    @Test
    @DisplayName("Should handle null message")
    void shouldHandleNullMessage() {
        // Given: Null message
        String message = null;

        // When: Processing market data update
        stockMarketDataService.onMarketDataUpdateComplete(message);

        // Then: Should handle gracefully
        assertDoesNotThrow(() -> stockMarketDataService.onMarketDataUpdateComplete(message));
    }
} 