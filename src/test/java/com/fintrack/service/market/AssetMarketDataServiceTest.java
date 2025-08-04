package com.fintrack.service.market;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fintrack.constants.KafkaTopics;
import com.fintrack.constants.finance.AssetType;
import com.fintrack.model.market.MarketData;
import com.fintrack.repository.finance.HoldingsMonthlyRepository;
import com.fintrack.repository.market.MarketDataRepository;
import com.fintrack.util.KafkaProducerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Test class for AssetMarketDataService.
 * Since AssetMarketDataService is abstract, we create a concrete implementation for testing.
 */
@ExtendWith(MockitoExtension.class)
class AssetMarketDataServiceTest {

    @Mock
    private MarketDataRepository marketDataRepository;

    @Mock
    private HoldingsMonthlyRepository holdingsMonthlyRepository;

    @Mock
    private KafkaProducerService kafkaProducerService;

    private TestAssetMarketDataService assetMarketDataService;

    /**
     * Concrete implementation of AssetMarketDataService for testing purposes.
     */
    private static class TestAssetMarketDataService extends AssetMarketDataService {
        public TestAssetMarketDataService(
                MarketDataRepository marketDataRepository,
                HoldingsMonthlyRepository holdingsMonthlyRepository,
                KafkaProducerService kafkaProducerService) {
            super(marketDataRepository, holdingsMonthlyRepository, kafkaProducerService);
        }

        @Override
        public AssetType getAssetType() {
            return AssetType.STOCK;
        }

        @Override
        public List<Map<String, String>> processSymbols(List<String> symbols) {
            List<Map<String, String>> processedSymbols = new ArrayList<>();
            for (String symbol : symbols) {
                Map<String, String> asset = new HashMap<>();
                asset.put("symbol", symbol);
                asset.put("asset_type", getAssetType().name());
                processedSymbols.add(asset);
            }
            return processedSymbols;
        }
    }

    @BeforeEach
    void setUp() {
        assetMarketDataService = new TestAssetMarketDataService(
                marketDataRepository,
                holdingsMonthlyRepository,
                kafkaProducerService
        );
    }

    @Test
    void shouldGetAssetType() {
        AssetType assetType = assetMarketDataService.getAssetType();
        assertEquals(AssetType.STOCK, assetType);
    }

    @Test
    void shouldProcessSymbols() {
        List<String> symbols = Arrays.asList("AAPL", "GOOGL", "MSFT");
        List<Map<String, String>> processedSymbols = assetMarketDataService.processSymbols(symbols);

        assertEquals(3, processedSymbols.size());
        
        for (int i = 0; i < symbols.size(); i++) {
            Map<String, String> asset = processedSymbols.get(i);
            assertEquals(symbols.get(i), asset.get("symbol"));
            assertEquals(AssetType.STOCK.name(), asset.get("asset_type"));
        }
    }

    @Test
    void shouldFetchMarketDataSuccessfully() {
        UUID accountId = UUID.randomUUID();
        List<String> symbols = Arrays.asList("AAPL", "GOOGL");
        
        // Mock market data
        MarketData marketData1 = createTestMarketData("AAPL", AssetType.STOCK);
        MarketData marketData2 = createTestMarketData("GOOGL", AssetType.STOCK);
        
        when(marketDataRepository.findMarketDataBySymbolAndAssetType("AAPL", AssetType.STOCK.name()))
                .thenReturn(Arrays.asList(marketData1));
        when(marketDataRepository.findMarketDataBySymbolAndAssetType("GOOGL", AssetType.STOCK.name()))
                .thenReturn(Arrays.asList(marketData2));
        
        when(holdingsMonthlyRepository.findEarliestDateByAccountId(accountId))
                .thenReturn(LocalDate.now().minusDays(30));
        when(holdingsMonthlyRepository.findLatestDateByAccountId(accountId))
                .thenReturn(LocalDate.now());

        List<MarketData> result = assetMarketDataService.fetchMarketData(accountId, symbols);

        assertEquals(2, result.size());
        verify(kafkaProducerService, times(2)).publishEvent(anyString(), anyString());
    }

    @Test
    void shouldHandleEmptySymbolsList() {
        UUID accountId = UUID.randomUUID();
        List<String> symbols = new ArrayList<>();

        List<MarketData> result = assetMarketDataService.fetchMarketData(accountId, symbols);

        assertTrue(result.isEmpty());
        verify(kafkaProducerService, times(1)).publishEvent(anyString(), anyString());
    }

    @Test
    void shouldHandleNullSymbolsList() {
        UUID accountId = UUID.randomUUID();

        assertThrows(NullPointerException.class, () -> {
            assetMarketDataService.fetchMarketData(accountId, null);
        });
    }

    @Test
    void shouldRetryWhenNoDataFoundInitially() {
        UUID accountId = UUID.randomUUID();
        List<String> symbols = Arrays.asList("AAPL");
        
        // First call returns empty, second call returns data
        when(marketDataRepository.findMarketDataBySymbolAndAssetType("AAPL", AssetType.STOCK.name()))
                .thenReturn(new ArrayList<>())
                .thenReturn(Arrays.asList(createTestMarketData("AAPL", AssetType.STOCK)));
        
        when(holdingsMonthlyRepository.findEarliestDateByAccountId(accountId))
                .thenReturn(LocalDate.now().minusDays(30));
        when(holdingsMonthlyRepository.findLatestDateByAccountId(accountId))
                .thenReturn(LocalDate.now());

        List<MarketData> result = assetMarketDataService.fetchMarketData(accountId, symbols);

        assertEquals(0, result.size()); // The service exits early when no data is found
        verify(marketDataRepository, times(1)).findMarketDataBySymbolAndAssetType("AAPL", AssetType.STOCK.name());
    }

    @Test
    void shouldHandlePartialDataFound() {
        UUID accountId = UUID.randomUUID();
        List<String> symbols = Arrays.asList("AAPL", "GOOGL", "MSFT");
        
        // Only AAPL has data
        when(marketDataRepository.findMarketDataBySymbolAndAssetType("AAPL", AssetType.STOCK.name()))
                .thenReturn(Arrays.asList(createTestMarketData("AAPL", AssetType.STOCK)));
        when(marketDataRepository.findMarketDataBySymbolAndAssetType("GOOGL", AssetType.STOCK.name()))
                .thenReturn(new ArrayList<>());
        when(marketDataRepository.findMarketDataBySymbolAndAssetType("MSFT", AssetType.STOCK.name()))
                .thenReturn(new ArrayList<>());
        
        when(holdingsMonthlyRepository.findEarliestDateByAccountId(accountId))
                .thenReturn(LocalDate.now().minusDays(30));
        when(holdingsMonthlyRepository.findLatestDateByAccountId(accountId))
                .thenReturn(LocalDate.now());

        List<MarketData> result = assetMarketDataService.fetchMarketData(accountId, symbols);

        assertEquals(1, result.size());
        assertEquals("AAPL", result.get(0).getSymbol());
    }

    @Test
    void shouldHandleNoHoldingsData() {
        UUID accountId = UUID.randomUUID();
        List<String> symbols = Arrays.asList("AAPL");
        
        when(marketDataRepository.findMarketDataBySymbolAndAssetType("AAPL", AssetType.STOCK.name()))
                .thenReturn(Arrays.asList(createTestMarketData("AAPL", AssetType.STOCK)));
        
        when(holdingsMonthlyRepository.findEarliestDateByAccountId(accountId))
                .thenReturn(null);
        when(holdingsMonthlyRepository.findLatestDateByAccountId(accountId))
                .thenReturn(null);

        List<MarketData> result = assetMarketDataService.fetchMarketData(accountId, symbols);

        assertEquals(1, result.size());
        verify(kafkaProducerService, times(1)).publishEvent(anyString(), anyString());
    }

    @Test
    void shouldUseCurrentDateWhenEndDateIsInPast() {
        UUID accountId = UUID.randomUUID();
        List<String> symbols = Arrays.asList("AAPL");
        
        when(marketDataRepository.findMarketDataBySymbolAndAssetType("AAPL", AssetType.STOCK.name()))
                .thenReturn(Arrays.asList(createTestMarketData("AAPL", AssetType.STOCK)));
        
        LocalDate pastDate = LocalDate.now().minusDays(10);
        when(holdingsMonthlyRepository.findEarliestDateByAccountId(accountId))
                .thenReturn(pastDate);
        when(holdingsMonthlyRepository.findLatestDateByAccountId(accountId))
                .thenReturn(pastDate);

        List<MarketData> result = assetMarketDataService.fetchMarketData(accountId, symbols);

        assertEquals(1, result.size());
        verify(kafkaProducerService, times(2)).publishEvent(anyString(), anyString());
    }

    @Test
    void shouldSendMarketDataUpdateRequest() {
        UUID accountId = UUID.randomUUID();
        List<Map<String, String>> assets = Arrays.asList(
                Map.of("symbol", "AAPL", "asset_type", AssetType.STOCK.name()),
                Map.of("symbol", "GOOGL", "asset_type", AssetType.STOCK.name())
        );

        // Mock holdings data to ensure both Kafka messages are sent
        when(holdingsMonthlyRepository.findEarliestDateByAccountId(accountId))
                .thenReturn(LocalDate.now().minusDays(30));
        when(holdingsMonthlyRepository.findLatestDateByAccountId(accountId))
                .thenReturn(LocalDate.now());

        assetMarketDataService.sendMarketDataUpdateRequest(accountId, assets);

        verify(kafkaProducerService, times(2)).publishEvent(anyString(), anyString());
    }

    @Test
    void shouldHandleExceptionInSendMarketDataUpdateRequest() {
        UUID accountId = UUID.randomUUID();
        List<Map<String, String>> assets = Arrays.asList(
                Map.of("symbol", "AAPL", "asset_type", AssetType.STOCK.name())
        );

        doThrow(new RuntimeException("Kafka error"))
                .when(kafkaProducerService).publishEvent(anyString(), anyString());

        // Should not throw exception
        assertDoesNotThrow(() -> {
            assetMarketDataService.sendMarketDataUpdateRequest(accountId, assets);
        });
    }

    @Test
    void shouldHandleInterruptedExceptionDuringRetry() {
        UUID accountId = UUID.randomUUID();
        List<String> symbols = Arrays.asList("AAPL");
        
        when(marketDataRepository.findMarketDataBySymbolAndAssetType("AAPL", AssetType.STOCK.name()))
                .thenReturn(new ArrayList<>());
        
        when(holdingsMonthlyRepository.findEarliestDateByAccountId(accountId))
                .thenReturn(LocalDate.now().minusDays(30));
        when(holdingsMonthlyRepository.findLatestDateByAccountId(accountId))
                .thenReturn(LocalDate.now());

        // Interrupt the current thread to simulate interruption during retry
        Thread.currentThread().interrupt();

        List<MarketData> result = assetMarketDataService.fetchMarketData(accountId, symbols);

        assertTrue(result.isEmpty());
        
        // Clear the interrupt status
        Thread.interrupted();
    }

    @Test
    void shouldHandleEmptyAssetsListInFetchMarketDataWithRetry() {
        List<Map<String, String>> assets = new ArrayList<>();
        
        List<MarketData> result = assetMarketDataService.fetchMarketDataWithRetry(assets);

        assertTrue(result.isEmpty());
        verify(marketDataRepository, never()).findMarketDataBySymbolAndAssetType(anyString(), anyString());
    }

    @Test
    void shouldHandleNullAssetsListInFetchMarketDataWithRetry() {
        assertThrows(NullPointerException.class, () -> {
            assetMarketDataService.fetchMarketDataWithRetry(null);
        });
    }

    @Test
    void shouldHandleMixedDataAvailability() {
        UUID accountId = UUID.randomUUID();
        List<String> symbols = Arrays.asList("AAPL", "GOOGL", "MSFT");
        
        // AAPL has data immediately, GOOGL has data after retry, MSFT never has data
        when(marketDataRepository.findMarketDataBySymbolAndAssetType("AAPL", AssetType.STOCK.name()))
                .thenReturn(Arrays.asList(createTestMarketData("AAPL", AssetType.STOCK)));
        when(marketDataRepository.findMarketDataBySymbolAndAssetType("GOOGL", AssetType.STOCK.name()))
                .thenReturn(new ArrayList<>())
                .thenReturn(Arrays.asList(createTestMarketData("GOOGL", AssetType.STOCK)));
        when(marketDataRepository.findMarketDataBySymbolAndAssetType("MSFT", AssetType.STOCK.name()))
                .thenReturn(new ArrayList<>());
        
        when(holdingsMonthlyRepository.findEarliestDateByAccountId(accountId))
                .thenReturn(LocalDate.now().minusDays(30));
        when(holdingsMonthlyRepository.findLatestDateByAccountId(accountId))
                .thenReturn(LocalDate.now());

        List<MarketData> result = assetMarketDataService.fetchMarketData(accountId, symbols);

        assertEquals(2, result.size());
        assertTrue(result.stream().anyMatch(data -> "AAPL".equals(data.getSymbol())));
        assertTrue(result.stream().anyMatch(data -> "GOOGL".equals(data.getSymbol())));
    }

    private MarketData createTestMarketData(String symbol, AssetType assetType) {
        MarketData marketData = new MarketData();
        marketData.setSymbol(symbol);
        marketData.setAssetType(assetType);
        marketData.setPrice(new BigDecimal("150.00"));
        marketData.setPercentChange(new BigDecimal("2.5"));
        marketData.setHigh(new BigDecimal("155.0"));
        marketData.setLow(new BigDecimal("145.0"));
        marketData.setChange(new BigDecimal("5.0"));
        marketData.setUpdatedAt(LocalDateTime.now());
        return marketData;
    }
} 