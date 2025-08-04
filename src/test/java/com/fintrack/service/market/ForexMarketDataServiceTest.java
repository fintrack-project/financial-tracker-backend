package com.fintrack.service.market;

import com.fintrack.constants.finance.AssetType;
import com.fintrack.model.finance.AccountCurrency;
import com.fintrack.repository.finance.AccountCurrenciesRepository;
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
class ForexMarketDataServiceTest {

    @Mock
    private CachedMarketDataService cachedMarketDataService;

    @Mock
    private HoldingsMonthlyRepository holdingsMonthlyRepository;

    @Mock
    private KafkaProducerService kafkaProducerService;

    @Mock
    private AccountCurrenciesRepository accountCurrenciesRepository;

    private ForexMarketDataService forexMarketDataService;

    private static final UUID TEST_ACCOUNT_ID = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        forexMarketDataService = new ForexMarketDataService(
            cachedMarketDataService, 
            holdingsMonthlyRepository, 
            kafkaProducerService,
            accountCurrenciesRepository
        );
    }

    @Test
    @DisplayName("Should return correct asset type")
    void shouldReturnCorrectAssetType() {
        // When
        AssetType assetType = forexMarketDataService.getAssetType();

        // Then
        assertEquals(AssetType.FOREX, assetType);
    }

    @Test
    @DisplayName("Should process forex symbols successfully")
    void shouldProcessForexSymbolsSuccessfully() {
        // Given
        List<String> symbols = Arrays.asList("EUR/USD", "GBP/USD", "JPY/USD");

        // When
        List<Map<String, String>> result = forexMarketDataService.processSymbols(symbols);

        // Then
        assertEquals(3, result.size());
        
        // Check first symbol
        Map<String, String> firstAsset = result.get(0);
        assertEquals("EUR/USD", firstAsset.get("symbol"));
        assertEquals("FOREX", firstAsset.get("asset_type"));
        
        // Check second symbol
        Map<String, String> secondAsset = result.get(1);
        assertEquals("GBP/USD", secondAsset.get("symbol"));
        assertEquals("FOREX", secondAsset.get("asset_type"));
        
        // Check third symbol
        Map<String, String> thirdAsset = result.get(2);
        assertEquals("JPY/USD", thirdAsset.get("symbol"));
        assertEquals("FOREX", thirdAsset.get("asset_type"));
    }

    @Test
    @DisplayName("Should handle empty symbols list")
    void shouldHandleEmptySymbolsList() {
        // Given
        List<String> symbols = new ArrayList<>();

        // When
        List<Map<String, String>> result = forexMarketDataService.processSymbols(symbols);

        // Then
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("Should handle single forex symbol")
    void shouldHandleSingleForexSymbol() {
        // Given
        List<String> symbols = Arrays.asList("EUR/USD");

        // When
        List<Map<String, String>> result = forexMarketDataService.processSymbols(symbols);

        // Then
        assertEquals(1, result.size());
        Map<String, String> asset = result.get(0);
        assertEquals("EUR/USD", asset.get("symbol"));
        assertEquals("FOREX", asset.get("asset_type"));
    }

    @Test
    @DisplayName("Should handle single currency symbols")
    void shouldHandleSingleCurrencySymbols() {
        // Given
        List<String> symbols = Arrays.asList("EUR", "GBP", "JPY");

        // When
        List<Map<String, String>> result = forexMarketDataService.processSymbols(symbols);

        // Then
        assertEquals(3, result.size());
        
        // Check first symbol
        Map<String, String> firstAsset = result.get(0);
        assertEquals("EUR/USD", firstAsset.get("symbol"));
        assertEquals("FOREX", firstAsset.get("asset_type"));
        
        // Check second symbol
        Map<String, String> secondAsset = result.get(1);
        assertEquals("GBP/USD", secondAsset.get("symbol"));
        assertEquals("FOREX", secondAsset.get("asset_type"));
        
        // Check third symbol
        Map<String, String> thirdAsset = result.get(2);
        assertEquals("JPY/USD", thirdAsset.get("symbol"));
        assertEquals("FOREX", thirdAsset.get("asset_type"));
    }

    @Test
    @DisplayName("Should skip USD when processing single currency")
    void shouldSkipUSDWhenProcessingSingleCurrency() {
        // Given
        List<String> symbols = Arrays.asList("EUR", "USD", "GBP");

        // When
        List<Map<String, String>> result = forexMarketDataService.processSymbols(symbols);

        // Then
        assertEquals(2, result.size());
        
        // Check first symbol
        Map<String, String> firstAsset = result.get(0);
        assertEquals("EUR/USD", firstAsset.get("symbol"));
        assertEquals("FOREX", firstAsset.get("asset_type"));
        
        // Check second symbol
        Map<String, String> secondAsset = result.get(1);
        assertEquals("GBP/USD", secondAsset.get("symbol"));
        assertEquals("FOREX", secondAsset.get("asset_type"));
    }

    @Test
    @DisplayName("Should handle market data update complete message gracefully")
    void shouldHandleMarketDataUpdateCompleteMessageGracefully() {
        // Given
        String message = "{\"assets\":[{\"symbol\":\"EUR/USD\",\"asset_type\":\"FOREX\"}]}";

        // When & Then
        assertDoesNotThrow(() -> {
            forexMarketDataService.onMarketDataUpdateComplete(message);
        });
    }

    @Test
    @DisplayName("Should handle null message gracefully")
    void shouldHandleNullMessageGracefully() {
        // When & Then
        assertDoesNotThrow(() -> forexMarketDataService.onMarketDataUpdateComplete(null));
    }

    @Test
    @DisplayName("Should handle empty message gracefully")
    void shouldHandleEmptyMessageGracefully() {
        // When & Then
        assertDoesNotThrow(() -> forexMarketDataService.onMarketDataUpdateComplete(""));
    }

    @Test
    @DisplayName("Should handle invalid JSON message gracefully")
    void shouldHandleInvalidJsonMessageGracefully() {
        // Given
        String message = "invalid json message";

        // When & Then
        assertDoesNotThrow(() -> forexMarketDataService.onMarketDataUpdateComplete(message));
    }

    @Test
    @DisplayName("Should process symbols for account with default currency")
    void shouldProcessSymbolsForAccountWithDefaultCurrency() {
        // Given
        List<String> symbols = Arrays.asList("EUR", "GBP", "JPY");
        AccountCurrency defaultCurrency = new AccountCurrency();
        defaultCurrency.setCurrency("EUR");
        defaultCurrency.setDefault(true);
        
        when(accountCurrenciesRepository.findByAccountId(TEST_ACCOUNT_ID))
            .thenReturn(Arrays.asList(defaultCurrency));

        // When
        List<Map<String, String>> result = forexMarketDataService.processSymbolsForAccount(TEST_ACCOUNT_ID, symbols);

        // Then
        assertEquals(2, result.size());
        
        // Check first symbol (GBP/EUR)
        Map<String, String> firstAsset = result.get(0);
        assertEquals("EUR/GBP", firstAsset.get("symbol"));
        assertEquals("FOREX", firstAsset.get("asset_type"));
        
        // Check second symbol (JPY/EUR)
        Map<String, String> secondAsset = result.get(1);
        assertEquals("EUR/JPY", secondAsset.get("symbol"));
        assertEquals("FOREX", secondAsset.get("asset_type"));
        
        verify(accountCurrenciesRepository).findByAccountId(TEST_ACCOUNT_ID);
    }

    @Test
    @DisplayName("Should process symbols for account with USD default when no default currency found")
    void shouldProcessSymbolsForAccountWithUSDDefaultWhenNoDefaultCurrencyFound() {
        // Given
        List<String> symbols = Arrays.asList("EUR", "GBP", "JPY");
        
        when(accountCurrenciesRepository.findByAccountId(TEST_ACCOUNT_ID))
            .thenReturn(new ArrayList<>());

        // When
        List<Map<String, String>> result = forexMarketDataService.processSymbolsForAccount(TEST_ACCOUNT_ID, symbols);

        // Then
        assertEquals(3, result.size());
        
        // Check first symbol (USD/EUR)
        Map<String, String> firstAsset = result.get(0);
        assertEquals("USD/EUR", firstAsset.get("symbol"));
        assertEquals("FOREX", firstAsset.get("asset_type"));
        
        // Check second symbol (USD/GBP)
        Map<String, String> secondAsset = result.get(1);
        assertEquals("USD/GBP", secondAsset.get("symbol"));
        assertEquals("FOREX", secondAsset.get("asset_type"));
        
        // Check third symbol (USD/JPY)
        Map<String, String> thirdAsset = result.get(2);
        assertEquals("USD/JPY", thirdAsset.get("symbol"));
        assertEquals("FOREX", thirdAsset.get("asset_type"));
        
        verify(accountCurrenciesRepository).findByAccountId(TEST_ACCOUNT_ID);
    }

    @Test
    @DisplayName("Should process symbols for account with exception handling")
    void shouldProcessSymbolsForAccountWithExceptionHandling() {
        // Given
        List<String> symbols = Arrays.asList("EUR", "GBP", "JPY");
        
        when(accountCurrenciesRepository.findByAccountId(TEST_ACCOUNT_ID))
            .thenThrow(new RuntimeException("Database error"));

        // When
        List<Map<String, String>> result = forexMarketDataService.processSymbolsForAccount(TEST_ACCOUNT_ID, symbols);

        // Then
        assertEquals(3, result.size());
        
        // Should default to USD when exception occurs
        Map<String, String> firstAsset = result.get(0);
        assertEquals("USD/EUR", firstAsset.get("symbol"));
        assertEquals("FOREX", firstAsset.get("asset_type"));
        
        verify(accountCurrenciesRepository).findByAccountId(TEST_ACCOUNT_ID);
    }

    @Test
    @DisplayName("Should handle mixed forex pairs and single currencies")
    void shouldHandleMixedForexPairsAndSingleCurrencies() {
        // Given
        List<String> symbols = Arrays.asList("EUR/USD", "GBP", "JPY/EUR", "CAD");

        // When
        List<Map<String, String>> result = forexMarketDataService.processSymbols(symbols);

        // Then
        assertEquals(4, result.size());
        
        // Check first symbol (already formatted)
        Map<String, String> firstAsset = result.get(0);
        assertEquals("EUR/USD", firstAsset.get("symbol"));
        assertEquals("FOREX", firstAsset.get("asset_type"));
        
        // Check second symbol (formatted as GBP/USD)
        Map<String, String> secondAsset = result.get(1);
        assertEquals("GBP/USD", secondAsset.get("symbol"));
        assertEquals("FOREX", secondAsset.get("asset_type"));
        
        // Check third symbol (already formatted)
        Map<String, String> thirdAsset = result.get(2);
        assertEquals("JPY/EUR", thirdAsset.get("symbol"));
        assertEquals("FOREX", thirdAsset.get("asset_type"));
        
        // Check fourth symbol (formatted as CAD/USD)
        Map<String, String> fourthAsset = result.get(3);
        assertEquals("CAD/USD", fourthAsset.get("symbol"));
        assertEquals("FOREX", fourthAsset.get("asset_type"));
    }

    @Test
    @DisplayName("Should handle large number of forex symbols")
    void shouldHandleLargeNumberOfForexSymbols() {
        // Given
        List<String> symbols = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            symbols.add("CURRENCY" + i + "/USD");
        }

        // When
        List<Map<String, String>> result = forexMarketDataService.processSymbols(symbols);

        // Then
        assertEquals(100, result.size());
        
        // Verify all symbols are processed correctly
        for (int i = 0; i < symbols.size(); i++) {
            Map<String, String> asset = result.get(i);
            assertEquals("CURRENCY" + i + "/USD", asset.get("symbol"));
            assertEquals("FOREX", asset.get("asset_type"));
        }
    }

    @Test
    @DisplayName("Should handle forex symbols with special characters")
    void shouldHandleForexSymbolsWithSpecialCharacters() {
        // Given
        List<String> symbols = Arrays.asList("EUR-USD", "GBP_EUR", "JPY.USD");

        // When
        List<Map<String, String>> result = forexMarketDataService.processSymbols(symbols);

        // Then
        assertEquals(3, result.size());
        
        // Verify symbols with special characters are processed correctly
        Map<String, String> firstAsset = result.get(0);
        assertEquals("EUR-USD/USD", firstAsset.get("symbol"));
        assertEquals("FOREX", firstAsset.get("asset_type"));
        
        Map<String, String> secondAsset = result.get(1);
        assertEquals("GBP_EUR/USD", secondAsset.get("symbol"));
        assertEquals("FOREX", secondAsset.get("asset_type"));
        
        Map<String, String> thirdAsset = result.get(2);
        assertEquals("JPY.USD/USD", thirdAsset.get("symbol"));
        assertEquals("FOREX", thirdAsset.get("asset_type"));
    }

    @Test
    @DisplayName("Should handle forex symbols with numbers")
    void shouldHandleForexSymbolsWithNumbers() {
        // Given
        List<String> symbols = Arrays.asList("EUR2024/USD", "GBP99/USD", "JPY2025/USD");

        // When
        List<Map<String, String>> result = forexMarketDataService.processSymbols(symbols);

        // Then
        assertEquals(3, result.size());
        
        // Verify symbols with numbers are processed correctly
        Map<String, String> firstAsset = result.get(0);
        assertEquals("EUR2024/USD", firstAsset.get("symbol"));
        assertEquals("FOREX", firstAsset.get("asset_type"));
        
        Map<String, String> secondAsset = result.get(1);
        assertEquals("GBP99/USD", secondAsset.get("symbol"));
        assertEquals("FOREX", secondAsset.get("asset_type"));
        
        Map<String, String> thirdAsset = result.get(2);
        assertEquals("JPY2025/USD", thirdAsset.get("symbol"));
        assertEquals("FOREX", thirdAsset.get("asset_type"));
    }

    @Test
    @DisplayName("Should handle forex symbols with mixed case")
    void shouldHandleForexSymbolsWithMixedCase() {
        // Given
        List<String> symbols = Arrays.asList("eur/USD", "GBP/usd", "jpy/Usd", "CAD/usd");

        // When
        List<Map<String, String>> result = forexMarketDataService.processSymbols(symbols);

        // Then
        assertEquals(4, result.size());
        
        // Verify symbols with mixed case are processed correctly
        Map<String, String> firstAsset = result.get(0);
        assertEquals("eur/USD", firstAsset.get("symbol"));
        assertEquals("FOREX", firstAsset.get("asset_type"));
        
        Map<String, String> secondAsset = result.get(1);
        assertEquals("GBP/usd", secondAsset.get("symbol"));
        assertEquals("FOREX", secondAsset.get("asset_type"));
        
        Map<String, String> thirdAsset = result.get(2);
        assertEquals("jpy/Usd", thirdAsset.get("symbol"));
        assertEquals("FOREX", thirdAsset.get("asset_type"));
        
        Map<String, String> fourthAsset = result.get(3);
        assertEquals("CAD/usd", fourthAsset.get("symbol"));
        assertEquals("FOREX", fourthAsset.get("asset_type"));
    }
} 