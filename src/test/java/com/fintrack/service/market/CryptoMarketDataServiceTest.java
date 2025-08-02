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
class CryptoMarketDataServiceTest {

    @Mock
    private CachedMarketDataService cachedMarketDataService;

    @Mock
    private HoldingsMonthlyRepository holdingsMonthlyRepository;

    @Mock
    private KafkaProducerService kafkaProducerService;

    private CryptoMarketDataService cryptoMarketDataService;

    @BeforeEach
    void setUp() {
        cryptoMarketDataService = new CryptoMarketDataService(
            cachedMarketDataService, 
            holdingsMonthlyRepository, 
            kafkaProducerService
        );
    }

    @Test
    @DisplayName("Should return correct asset type")
    void shouldReturnCorrectAssetType() {
        // When
        AssetType assetType = cryptoMarketDataService.getAssetType();

        // Then
        assertEquals(AssetType.CRYPTO, assetType);
    }

    @Test
    @DisplayName("Should process crypto symbols successfully")
    void shouldProcessCryptoSymbolsSuccessfully() {
        // Given
        List<String> symbols = Arrays.asList("BTC/USD", "ETH/USD", "ADA/USD");

        // When
        List<Map<String, String>> result = cryptoMarketDataService.processSymbols(symbols);

        // Then
        assertEquals(3, result.size());
        
        // Check first symbol
        Map<String, String> firstAsset = result.get(0);
        assertEquals("BTC/USD", firstAsset.get("symbol"));
        assertEquals("CRYPTO", firstAsset.get("asset_type"));
        
        // Check second symbol
        Map<String, String> secondAsset = result.get(1);
        assertEquals("ETH/USD", secondAsset.get("symbol"));
        assertEquals("CRYPTO", secondAsset.get("asset_type"));
        
        // Check third symbol
        Map<String, String> thirdAsset = result.get(2);
        assertEquals("ADA/USD", thirdAsset.get("symbol"));
        assertEquals("CRYPTO", thirdAsset.get("asset_type"));
    }

    @Test
    @DisplayName("Should handle empty symbols list")
    void shouldHandleEmptySymbolsList() {
        // Given
        List<String> symbols = new ArrayList<>();

        // When
        List<Map<String, String>> result = cryptoMarketDataService.processSymbols(symbols);

        // Then
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("Should handle single crypto symbol")
    void shouldHandleSingleCryptoSymbol() {
        // Given
        List<String> symbols = Arrays.asList("BTC/USD");

        // When
        List<Map<String, String>> result = cryptoMarketDataService.processSymbols(symbols);

        // Then
        assertEquals(1, result.size());
        Map<String, String> asset = result.get(0);
        assertEquals("BTC/USD", asset.get("symbol"));
        assertEquals("CRYPTO", asset.get("asset_type"));
    }

    @Test
    @DisplayName("Should handle market data update complete message gracefully")
    void shouldHandleMarketDataUpdateCompleteMessageGracefully() {
        // Given
        String message = "{\"assets\":[{\"symbol\":\"BTC/USD\",\"asset_type\":\"CRYPTO\"}]}";

        // When & Then
        assertDoesNotThrow(() -> {
            cryptoMarketDataService.onMarketDataUpdateComplete(message);
        });
    }

    @Test
    @DisplayName("Should handle null message gracefully")
    void shouldHandleNullMessageGracefully() {
        // When & Then
        assertDoesNotThrow(() -> cryptoMarketDataService.onMarketDataUpdateComplete(null));
    }

    @Test
    @DisplayName("Should handle empty message gracefully")
    void shouldHandleEmptyMessageGracefully() {
        // When & Then
        assertDoesNotThrow(() -> cryptoMarketDataService.onMarketDataUpdateComplete(""));
    }

    @Test
    @DisplayName("Should handle invalid JSON message gracefully")
    void shouldHandleInvalidJsonMessageGracefully() {
        // Given
        String message = "invalid json message";

        // When & Then
        assertDoesNotThrow(() -> cryptoMarketDataService.onMarketDataUpdateComplete(message));
    }

    @Test
    @DisplayName("Should process multiple crypto symbols with different formats")
    void shouldProcessMultipleCryptoSymbolsWithDifferentFormats() {
        // Given
        List<String> symbols = Arrays.asList("BTC/USD", "ETH/USD", "ADA/USD", "DOT/USD", "LINK/USD");

        // When
        List<Map<String, String>> result = cryptoMarketDataService.processSymbols(symbols);

        // Then
        assertEquals(5, result.size());
        
        // Verify all symbols are processed correctly
        for (int i = 0; i < symbols.size(); i++) {
            Map<String, String> asset = result.get(i);
            assertEquals(symbols.get(i), asset.get("symbol"));
            assertEquals("CRYPTO", asset.get("asset_type"));
        }
    }

    @Test
    @DisplayName("Should handle crypto symbols with different quote currencies")
    void shouldHandleCryptoSymbolsWithDifferentQuoteCurrencies() {
        // Given
        List<String> symbols = Arrays.asList("BTC/USD", "ETH/EUR", "ADA/GBP", "DOT/JPY");

        // When
        List<Map<String, String>> result = cryptoMarketDataService.processSymbols(symbols);

        // Then
        assertEquals(4, result.size());
        
        // Verify symbols with different quote currencies are processed correctly
        Map<String, String> firstAsset = result.get(0);
        assertEquals("BTC/USD", firstAsset.get("symbol"));
        assertEquals("CRYPTO", firstAsset.get("asset_type"));
        
        Map<String, String> secondAsset = result.get(1);
        assertEquals("ETH/EUR", secondAsset.get("symbol"));
        assertEquals("CRYPTO", secondAsset.get("asset_type"));
        
        Map<String, String> thirdAsset = result.get(2);
        assertEquals("ADA/GBP", thirdAsset.get("symbol"));
        assertEquals("CRYPTO", thirdAsset.get("asset_type"));
        
        Map<String, String> fourthAsset = result.get(3);
        assertEquals("DOT/JPY", fourthAsset.get("symbol"));
        assertEquals("CRYPTO", fourthAsset.get("asset_type"));
    }

    @Test
    @DisplayName("Should handle crypto symbols with special characters")
    void shouldHandleCryptoSymbolsWithSpecialCharacters() {
        // Given
        List<String> symbols = Arrays.asList("BTC-USD", "ETH-EUR", "ADA-GBP");

        // When
        List<Map<String, String>> result = cryptoMarketDataService.processSymbols(symbols);

        // Then
        assertEquals(3, result.size());
        
        // Verify symbols with special characters are processed correctly
        Map<String, String> firstAsset = result.get(0);
        assertEquals("BTC-USD", firstAsset.get("symbol"));
        assertEquals("CRYPTO", firstAsset.get("asset_type"));
        
        Map<String, String> secondAsset = result.get(1);
        assertEquals("ETH-EUR", secondAsset.get("symbol"));
        assertEquals("CRYPTO", secondAsset.get("asset_type"));
        
        Map<String, String> thirdAsset = result.get(2);
        assertEquals("ADA-GBP", thirdAsset.get("symbol"));
        assertEquals("CRYPTO", thirdAsset.get("asset_type"));
    }

    @Test
    @DisplayName("Should handle large number of crypto symbols")
    void shouldHandleLargeNumberOfCryptoSymbols() {
        // Given
        List<String> symbols = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            symbols.add("CRYPTO" + i + "/USD");
        }

        // When
        List<Map<String, String>> result = cryptoMarketDataService.processSymbols(symbols);

        // Then
        assertEquals(100, result.size());
        
        // Verify all symbols are processed correctly
        for (int i = 0; i < symbols.size(); i++) {
            Map<String, String> asset = result.get(i);
            assertEquals("CRYPTO" + i + "/USD", asset.get("symbol"));
            assertEquals("CRYPTO", asset.get("asset_type"));
        }
    }

    @Test
    @DisplayName("Should handle crypto symbols with numbers")
    void shouldHandleCryptoSymbolsWithNumbers() {
        // Given
        List<String> symbols = Arrays.asList("BTC2024/USD", "ETH99/USD", "ADA2025/USD");

        // When
        List<Map<String, String>> result = cryptoMarketDataService.processSymbols(symbols);

        // Then
        assertEquals(3, result.size());
        
        // Verify symbols with numbers are processed correctly
        Map<String, String> firstAsset = result.get(0);
        assertEquals("BTC2024/USD", firstAsset.get("symbol"));
        assertEquals("CRYPTO", firstAsset.get("asset_type"));
        
        Map<String, String> secondAsset = result.get(1);
        assertEquals("ETH99/USD", secondAsset.get("symbol"));
        assertEquals("CRYPTO", secondAsset.get("asset_type"));
        
        Map<String, String> thirdAsset = result.get(2);
        assertEquals("ADA2025/USD", thirdAsset.get("symbol"));
        assertEquals("CRYPTO", thirdAsset.get("asset_type"));
    }

    @Test
    @DisplayName("Should handle crypto symbols with mixed case")
    void shouldHandleCryptoSymbolsWithMixedCase() {
        // Given
        List<String> symbols = Arrays.asList("btc/USD", "ETH/usd", "ada/Usd", "DOT/usd");

        // When
        List<Map<String, String>> result = cryptoMarketDataService.processSymbols(symbols);

        // Then
        assertEquals(4, result.size());
        
        // Verify symbols with mixed case are processed correctly
        Map<String, String> firstAsset = result.get(0);
        assertEquals("btc/USD", firstAsset.get("symbol"));
        assertEquals("CRYPTO", firstAsset.get("asset_type"));
        
        Map<String, String> secondAsset = result.get(1);
        assertEquals("ETH/usd", secondAsset.get("symbol"));
        assertEquals("CRYPTO", secondAsset.get("asset_type"));
        
        Map<String, String> thirdAsset = result.get(2);
        assertEquals("ada/Usd", thirdAsset.get("symbol"));
        assertEquals("CRYPTO", thirdAsset.get("asset_type"));
        
        Map<String, String> fourthAsset = result.get(3);
        assertEquals("DOT/usd", fourthAsset.get("symbol"));
        assertEquals("CRYPTO", fourthAsset.get("asset_type"));
    }

    @Test
    @DisplayName("Should handle crypto symbols with different separators")
    void shouldHandleCryptoSymbolsWithDifferentSeparators() {
        // Given
        List<String> symbols = Arrays.asList("BTC/USD", "ETH-USD", "ADA.USD", "DOT_USD");

        // When
        List<Map<String, String>> result = cryptoMarketDataService.processSymbols(symbols);

        // Then
        assertEquals(4, result.size());
        
        // Verify symbols with different separators are processed correctly
        Map<String, String> firstAsset = result.get(0);
        assertEquals("BTC/USD", firstAsset.get("symbol"));
        assertEquals("CRYPTO", firstAsset.get("asset_type"));
        
        Map<String, String> secondAsset = result.get(1);
        assertEquals("ETH-USD", secondAsset.get("symbol"));
        assertEquals("CRYPTO", secondAsset.get("asset_type"));
        
        Map<String, String> thirdAsset = result.get(2);
        assertEquals("ADA.USD", thirdAsset.get("symbol"));
        assertEquals("CRYPTO", thirdAsset.get("asset_type"));
        
        Map<String, String> fourthAsset = result.get(3);
        assertEquals("DOT_USD", fourthAsset.get("symbol"));
        assertEquals("CRYPTO", fourthAsset.get("asset_type"));
    }
} 