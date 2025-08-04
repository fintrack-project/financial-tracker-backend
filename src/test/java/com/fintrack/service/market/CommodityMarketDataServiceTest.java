package com.fintrack.service.market;

import com.fintrack.constants.finance.AssetType;
import com.fintrack.repository.finance.HoldingsMonthlyRepository;
import com.fintrack.util.KafkaProducerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CommodityMarketDataServiceTest {

    @Mock
    private CachedMarketDataService cachedMarketDataService;

    @Mock
    private HoldingsMonthlyRepository holdingsMonthlyRepository;

    @Mock
    private KafkaProducerService kafkaProducerService;

    private CommodityMarketDataService commodityMarketDataService;

    @BeforeEach
    void setUp() {
        commodityMarketDataService = new CommodityMarketDataService(
            cachedMarketDataService, 
            holdingsMonthlyRepository, 
            kafkaProducerService
        );
    }

    @Test
    @DisplayName("Should return correct asset type")
    void shouldReturnCorrectAssetType() {
        // When
        AssetType assetType = commodityMarketDataService.getAssetType();

        // Then
        assertEquals(AssetType.COMMODITY, assetType);
    }

    @Test
    @DisplayName("Should process commodity symbols successfully")
    void shouldProcessCommoditySymbolsSuccessfully() {
        // Given
        List<String> symbols = Arrays.asList("GOLD", "SILVER", "OIL");

        // When
        List<Map<String, String>> result = commodityMarketDataService.processSymbols(symbols);

        // Then
        assertEquals(3, result.size());
        
        // Check first symbol
        Map<String, String> firstAsset = result.get(0);
        assertEquals("GOLD", firstAsset.get("symbol"));
        assertEquals("COMMODITY", firstAsset.get("asset_type"));
        
        // Check second symbol
        Map<String, String> secondAsset = result.get(1);
        assertEquals("SILVER", secondAsset.get("symbol"));
        assertEquals("COMMODITY", secondAsset.get("asset_type"));
        
        // Check third symbol
        Map<String, String> thirdAsset = result.get(2);
        assertEquals("OIL", thirdAsset.get("symbol"));
        assertEquals("COMMODITY", thirdAsset.get("asset_type"));
    }

    @Test
    @DisplayName("Should handle empty symbols list")
    void shouldHandleEmptySymbolsList() {
        // Given
        List<String> symbols = new ArrayList<>();

        // When
        List<Map<String, String>> result = commodityMarketDataService.processSymbols(symbols);

        // Then
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("Should handle single commodity symbol")
    void shouldHandleSingleCommoditySymbol() {
        // Given
        List<String> symbols = Arrays.asList("GOLD");

        // When
        List<Map<String, String>> result = commodityMarketDataService.processSymbols(symbols);

        // Then
        assertEquals(1, result.size());
        Map<String, String> asset = result.get(0);
        assertEquals("GOLD", asset.get("symbol"));
        assertEquals("COMMODITY", asset.get("asset_type"));
    }

    @Test
    @DisplayName("Should handle market data update complete message gracefully")
    void shouldHandleMarketDataUpdateCompleteMessageGracefully() {
        // Given
        String message = "{\"assets\":[{\"symbol\":\"GOLD\",\"asset_type\":\"COMMODITY\"}]}";

        // When & Then
        assertDoesNotThrow(() -> {
            commodityMarketDataService.onMarketDataUpdateComplete(message);
        });
    }

    @Test
    @DisplayName("Should handle null message gracefully")
    void shouldHandleNullMessageGracefully() {
        // When & Then
        assertDoesNotThrow(() -> commodityMarketDataService.onMarketDataUpdateComplete(null));
    }

    @Test
    @DisplayName("Should handle empty message gracefully")
    void shouldHandleEmptyMessageGracefully() {
        // When & Then
        assertDoesNotThrow(() -> commodityMarketDataService.onMarketDataUpdateComplete(""));
    }

    @Test
    @DisplayName("Should handle invalid JSON message gracefully")
    void shouldHandleInvalidJsonMessageGracefully() {
        // Given
        String message = "invalid json message";

        // When & Then
        assertDoesNotThrow(() -> commodityMarketDataService.onMarketDataUpdateComplete(message));
    }

    @Test
    @DisplayName("Should process multiple commodity symbols with different formats")
    void shouldProcessMultipleCommoditySymbolsWithDifferentFormats() {
        // Given
        List<String> symbols = Arrays.asList("GOLD", "SILVER", "OIL", "COPPER", "PLATINUM");

        // When
        List<Map<String, String>> result = commodityMarketDataService.processSymbols(symbols);

        // Then
        assertEquals(5, result.size());
        
        // Verify all symbols are processed correctly
        for (int i = 0; i < symbols.size(); i++) {
            Map<String, String> asset = result.get(i);
            assertEquals(symbols.get(i), asset.get("symbol"));
            assertEquals("COMMODITY", asset.get("asset_type"));
        }
    }

    @Test
    @DisplayName("Should handle commodity symbols with special characters")
    void shouldHandleCommoditySymbolsWithSpecialCharacters() {
        // Given
        List<String> symbols = Arrays.asList("GOLD-USD", "SILVER-EUR", "OIL-BRENT");

        // When
        List<Map<String, String>> result = commodityMarketDataService.processSymbols(symbols);

        // Then
        assertEquals(3, result.size());
        
        // Verify symbols with special characters are processed correctly
        Map<String, String> firstAsset = result.get(0);
        assertEquals("GOLD-USD", firstAsset.get("symbol"));
        assertEquals("COMMODITY", firstAsset.get("asset_type"));
        
        Map<String, String> secondAsset = result.get(1);
        assertEquals("SILVER-EUR", secondAsset.get("symbol"));
        assertEquals("COMMODITY", secondAsset.get("asset_type"));
        
        Map<String, String> thirdAsset = result.get(2);
        assertEquals("OIL-BRENT", thirdAsset.get("symbol"));
        assertEquals("COMMODITY", thirdAsset.get("asset_type"));
    }

    @Test
    @DisplayName("Should handle large number of commodity symbols")
    void shouldHandleLargeNumberOfCommoditySymbols() {
        // Given
        List<String> symbols = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            symbols.add("COMMODITY" + i);
        }

        // When
        List<Map<String, String>> result = commodityMarketDataService.processSymbols(symbols);

        // Then
        assertEquals(100, result.size());
        
        // Verify all symbols are processed correctly
        for (int i = 0; i < symbols.size(); i++) {
            Map<String, String> asset = result.get(i);
            assertEquals("COMMODITY" + i, asset.get("symbol"));
            assertEquals("COMMODITY", asset.get("asset_type"));
        }
    }

    @Test
    @DisplayName("Should handle commodity symbols with numbers")
    void shouldHandleCommoditySymbolsWithNumbers() {
        // Given
        List<String> symbols = Arrays.asList("GOLD2024", "SILVER99", "OIL2025");

        // When
        List<Map<String, String>> result = commodityMarketDataService.processSymbols(symbols);

        // Then
        assertEquals(3, result.size());
        
        // Verify symbols with numbers are processed correctly
        Map<String, String> firstAsset = result.get(0);
        assertEquals("GOLD2024", firstAsset.get("symbol"));
        assertEquals("COMMODITY", firstAsset.get("asset_type"));
        
        Map<String, String> secondAsset = result.get(1);
        assertEquals("SILVER99", secondAsset.get("symbol"));
        assertEquals("COMMODITY", secondAsset.get("asset_type"));
        
        Map<String, String> thirdAsset = result.get(2);
        assertEquals("OIL2025", thirdAsset.get("symbol"));
        assertEquals("COMMODITY", thirdAsset.get("asset_type"));
    }

    @Test
    @DisplayName("Should handle commodity symbols with mixed case")
    void shouldHandleCommoditySymbolsWithMixedCase() {
        // Given
        List<String> symbols = Arrays.asList("Gold", "SILVER", "Oil", "Copper");

        // When
        List<Map<String, String>> result = commodityMarketDataService.processSymbols(symbols);

        // Then
        assertEquals(4, result.size());
        
        // Verify symbols with mixed case are processed correctly
        Map<String, String> firstAsset = result.get(0);
        assertEquals("Gold", firstAsset.get("symbol"));
        assertEquals("COMMODITY", firstAsset.get("asset_type"));
        
        Map<String, String> secondAsset = result.get(1);
        assertEquals("SILVER", secondAsset.get("symbol"));
        assertEquals("COMMODITY", secondAsset.get("asset_type"));
        
        Map<String, String> thirdAsset = result.get(2);
        assertEquals("Oil", thirdAsset.get("symbol"));
        assertEquals("COMMODITY", thirdAsset.get("asset_type"));
        
        Map<String, String> fourthAsset = result.get(3);
        assertEquals("Copper", fourthAsset.get("symbol"));
        assertEquals("COMMODITY", fourthAsset.get("asset_type"));
    }
} 