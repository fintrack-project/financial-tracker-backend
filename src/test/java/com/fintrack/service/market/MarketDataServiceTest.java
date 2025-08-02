package com.fintrack.service.market;

import com.fintrack.constants.KafkaTopics;
import com.fintrack.constants.finance.AssetType;
import com.fintrack.model.market.MarketData;
import com.fintrack.util.KafkaProducerService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("MarketDataService Tests")
class MarketDataServiceTest {

    @Mock
    private StockMarketDataService stockMarketDataService;

    @Mock
    private ForexMarketDataService forexMarketDataService;

    @Mock
    private CryptoMarketDataService cryptoMarketDataService;

    @Mock
    private CommodityMarketDataService commodityMarketDataService;

    @Mock
    private KafkaProducerService kafkaProducerService;

    private MarketDataService marketDataService;
    private ObjectMapper objectMapper;

    private static final UUID TEST_ACCOUNT_ID = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        marketDataService = new MarketDataService(
            stockMarketDataService,
            forexMarketDataService,
            cryptoMarketDataService,
            commodityMarketDataService,
            kafkaProducerService
        );
        objectMapper = new ObjectMapper();
    }

    @Test
    @DisplayName("Should fetch market data for stock entities successfully")
    void shouldFetchMarketDataForStockEntitiesSuccessfully() {
        // Given: Stock entities
        List<Map<String, String>> entities = Arrays.asList(
            Map.of("symbol", "AAPL", "assetType", "STOCK"),
            Map.of("symbol", "GOOGL", "assetType", "STOCK")
        );
        List<MarketData> expectedStockResults = Arrays.asList(
            createTestMarketData("AAPL", "STOCK", new BigDecimal("150.00")),
            createTestMarketData("GOOGL", "STOCK", new BigDecimal("2800.00"))
        );

        when(stockMarketDataService.fetchMarketData(TEST_ACCOUNT_ID, Arrays.asList("AAPL", "GOOGL")))
            .thenReturn(expectedStockResults);

        // When: Fetch market data
        List<MarketData> result = marketDataService.fetchMarketData(TEST_ACCOUNT_ID, entities);

        // Then: Should return stock results
        assertEquals(2, result.size());
        assertEquals("AAPL", result.get(0).getSymbol());
        assertEquals("GOOGL", result.get(1).getSymbol());
        verify(stockMarketDataService).fetchMarketData(TEST_ACCOUNT_ID, Arrays.asList("AAPL", "GOOGL"));
    }

    @Test
    @DisplayName("Should fetch market data for forex entities successfully")
    void shouldFetchMarketDataForForexEntitiesSuccessfully() {
        // Given: Forex entities
        List<Map<String, String>> entities = Arrays.asList(
            Map.of("symbol", "EUR/USD", "assetType", "FOREX"),
            Map.of("symbol", "GBP/USD", "assetType", "FOREX")
        );
        List<MarketData> expectedForexResults = Arrays.asList(
            createTestMarketData("EUR/USD", "FOREX", new BigDecimal("1.0850")),
            createTestMarketData("GBP/USD", "FOREX", new BigDecimal("1.2650"))
        );

        when(forexMarketDataService.fetchMarketData(TEST_ACCOUNT_ID, Arrays.asList("EUR/USD", "GBP/USD")))
            .thenReturn(expectedForexResults);

        // When: Fetch market data
        List<MarketData> result = marketDataService.fetchMarketData(TEST_ACCOUNT_ID, entities);

        // Then: Should return forex results
        assertEquals(2, result.size());
        assertEquals("EUR/USD", result.get(0).getSymbol());
        assertEquals("GBP/USD", result.get(1).getSymbol());
        verify(forexMarketDataService).fetchMarketData(TEST_ACCOUNT_ID, Arrays.asList("EUR/USD", "GBP/USD"));
    }

    @Test
    @DisplayName("Should fetch market data for crypto entities successfully")
    void shouldFetchMarketDataForCryptoEntitiesSuccessfully() {
        // Given: Crypto entities
        List<Map<String, String>> entities = Arrays.asList(
            Map.of("symbol", "BTC/USD", "assetType", "CRYPTO"),
            Map.of("symbol", "ETH/USD", "assetType", "CRYPTO")
        );
        List<MarketData> expectedCryptoResults = Arrays.asList(
            createTestMarketData("BTC/USD", "CRYPTO", new BigDecimal("45000.00")),
            createTestMarketData("ETH/USD", "CRYPTO", new BigDecimal("2800.00"))
        );

        when(cryptoMarketDataService.fetchMarketData(TEST_ACCOUNT_ID, Arrays.asList("BTC/USD", "ETH/USD")))
            .thenReturn(expectedCryptoResults);

        // When: Fetch market data
        List<MarketData> result = marketDataService.fetchMarketData(TEST_ACCOUNT_ID, entities);

        // Then: Should return crypto results
        assertEquals(2, result.size());
        assertEquals("BTC/USD", result.get(0).getSymbol());
        assertEquals("ETH/USD", result.get(1).getSymbol());
        verify(cryptoMarketDataService).fetchMarketData(TEST_ACCOUNT_ID, Arrays.asList("BTC/USD", "ETH/USD"));
    }

    @Test
    @DisplayName("Should fetch market data for commodity entities successfully")
    void shouldFetchMarketDataForCommodityEntitiesSuccessfully() {
        // Given: Commodity entities
        List<Map<String, String>> entities = Arrays.asList(
            Map.of("symbol", "GOLD", "assetType", "COMMODITY"),
            Map.of("symbol", "SILVER", "assetType", "COMMODITY")
        );
        List<MarketData> expectedCommodityResults = Arrays.asList(
            createTestMarketData("GOLD", "COMMODITY", new BigDecimal("1950.00")),
            createTestMarketData("SILVER", "COMMODITY", new BigDecimal("24.50"))
        );

        when(commodityMarketDataService.fetchMarketData(TEST_ACCOUNT_ID, Arrays.asList("GOLD", "SILVER")))
            .thenReturn(expectedCommodityResults);

        // When: Fetch market data
        List<MarketData> result = marketDataService.fetchMarketData(TEST_ACCOUNT_ID, entities);

        // Then: Should return commodity results
        assertEquals(2, result.size());
        assertEquals("GOLD", result.get(0).getSymbol());
        assertEquals("SILVER", result.get(1).getSymbol());
        verify(commodityMarketDataService).fetchMarketData(TEST_ACCOUNT_ID, Arrays.asList("GOLD", "SILVER"));
    }

    @Test
    @DisplayName("Should fetch market data for mixed asset types successfully")
    void shouldFetchMarketDataForMixedAssetTypesSuccessfully() {
        // Given: Mixed asset types
        List<Map<String, String>> entities = Arrays.asList(
            Map.of("symbol", "AAPL", "assetType", "STOCK"),
            Map.of("symbol", "EUR/USD", "assetType", "FOREX"),
            Map.of("symbol", "BTC/USD", "assetType", "CRYPTO"),
            Map.of("symbol", "GOLD", "assetType", "COMMODITY")
        );

        List<MarketData> expectedStockResults = Arrays.asList(
            createTestMarketData("AAPL", "STOCK", new BigDecimal("150.00"))
        );
        List<MarketData> expectedForexResults = Arrays.asList(
            createTestMarketData("EUR/USD", "FOREX", new BigDecimal("1.0850"))
        );
        List<MarketData> expectedCryptoResults = Arrays.asList(
            createTestMarketData("BTC/USD", "CRYPTO", new BigDecimal("45000.00"))
        );
        List<MarketData> expectedCommodityResults = Arrays.asList(
            createTestMarketData("GOLD", "COMMODITY", new BigDecimal("1950.00"))
        );

        when(stockMarketDataService.fetchMarketData(TEST_ACCOUNT_ID, Arrays.asList("AAPL")))
            .thenReturn(expectedStockResults);
        when(forexMarketDataService.fetchMarketData(TEST_ACCOUNT_ID, Arrays.asList("EUR/USD")))
            .thenReturn(expectedForexResults);
        when(cryptoMarketDataService.fetchMarketData(TEST_ACCOUNT_ID, Arrays.asList("BTC/USD")))
            .thenReturn(expectedCryptoResults);
        when(commodityMarketDataService.fetchMarketData(TEST_ACCOUNT_ID, Arrays.asList("GOLD")))
            .thenReturn(expectedCommodityResults);

        // When: Fetch market data
        List<MarketData> result = marketDataService.fetchMarketData(TEST_ACCOUNT_ID, entities);

        // Then: Should return all results
        assertEquals(4, result.size());
        verify(stockMarketDataService).fetchMarketData(TEST_ACCOUNT_ID, Arrays.asList("AAPL"));
        verify(forexMarketDataService).fetchMarketData(TEST_ACCOUNT_ID, Arrays.asList("EUR/USD"));
        verify(cryptoMarketDataService).fetchMarketData(TEST_ACCOUNT_ID, Arrays.asList("BTC/USD"));
        verify(commodityMarketDataService).fetchMarketData(TEST_ACCOUNT_ID, Arrays.asList("GOLD"));
    }

    @Test
    @DisplayName("Should handle entities with null symbol or assetType")
    void shouldHandleEntitiesWithNullSymbolOrAssetType() {
        // Given: Entities with null values
        List<Map<String, String>> entities = Arrays.asList(
            Map.of("symbol", "AAPL", "assetType", "STOCK"),
            createMap("symbol", null, "assetType", "STOCK"),
            createMap("symbol", "GOOGL", "assetType", null),
            createMap("symbol", null, "assetType", null)
        );

        List<MarketData> expectedStockResults = Arrays.asList(
            createTestMarketData("AAPL", "STOCK", new BigDecimal("150.00"))
        );

        when(stockMarketDataService.fetchMarketData(TEST_ACCOUNT_ID, Arrays.asList("AAPL")))
            .thenReturn(expectedStockResults);

        // When: Fetch market data
        List<MarketData> result = marketDataService.fetchMarketData(TEST_ACCOUNT_ID, entities);

        // Then: Should only process valid entities
        assertEquals(1, result.size());
        assertEquals("AAPL", result.get(0).getSymbol());
        verify(stockMarketDataService).fetchMarketData(TEST_ACCOUNT_ID, Arrays.asList("AAPL"));
    }

    @Test
    @DisplayName("Should return empty list when no valid entities")
    void shouldReturnEmptyListWhenNoValidEntities() {
        // Given: No valid entities
        List<Map<String, String>> entities = Arrays.asList(
            createMap("symbol", null, "assetType", "STOCK"),
            createMap("symbol", "GOOGL", "assetType", null),
            createMap("symbol", null, "assetType", null)
        );

        // When: Fetch market data
        List<MarketData> result = marketDataService.fetchMarketData(TEST_ACCOUNT_ID, entities);

        // Then: Should return empty list
        assertTrue(result.isEmpty());
        verifyNoInteractions(stockMarketDataService, forexMarketDataService, cryptoMarketDataService, commodityMarketDataService);
    }

    @Test
    @DisplayName("Should get update request topic correctly")
    void shouldGetUpdateRequestTopicCorrectly() {
        // When: Get update request topic
        KafkaTopics topic = marketDataService.getUpdateRequestTopic();

        // Then: Should return correct topic
        assertEquals(KafkaTopics.MARKET_DATA_UPDATE_REQUEST, topic);
    }

    @Test
    @DisplayName("Should create update request payload with symbols correctly")
    void shouldCreateUpdateRequestPayloadWithSymbolsCorrectly() {
        // Given: Data with symbols
        Map<String, Object> data = new HashMap<>();
        data.put("symbols", Arrays.asList("AAPL", "GOOGL", "MSFT"));
        data.put("otherField", "value");

        // When: Create update request payload
        Map<String, Object> result = marketDataService.createUpdateRequestPayload(data);

        // Then: Should convert symbols to assets format
        assertFalse(result.containsKey("symbols"));
        assertTrue(result.containsKey("assets"));
        assertTrue(result.containsKey("otherField"));
        assertEquals("value", result.get("otherField"));

        @SuppressWarnings("unchecked")
        List<Map<String, String>> assets = (List<Map<String, String>>) result.get("assets");
        assertEquals(3, assets.size());
        assertEquals("AAPL", assets.get(0).get("symbol"));
        assertEquals("STOCK", assets.get(0).get("asset_type"));
        assertEquals("GOOGL", assets.get(1).get("symbol"));
        assertEquals("STOCK", assets.get(1).get("asset_type"));
        assertEquals("MSFT", assets.get(2).get("symbol"));
        assertEquals("STOCK", assets.get(2).get("asset_type"));
    }

    @Test
    @DisplayName("Should create update request payload without symbols correctly")
    void shouldCreateUpdateRequestPayloadWithoutSymbolsCorrectly() {
        // Given: Data without symbols
        Map<String, Object> data = new HashMap<>();
        data.put("field1", "value1");
        data.put("field2", "value2");

        // When: Create update request payload
        Map<String, Object> result = marketDataService.createUpdateRequestPayload(data);

        // Then: Should return data unchanged
        assertEquals(data, result);
    }

    @Test
    @DisplayName("Should handle market data update complete message successfully")
    void shouldHandleMarketDataUpdateCompleteMessageSuccessfully() {
        // Given: Valid message
        String message = "{\"assets\":[{\"symbol\":\"AAPL\",\"asset_type\":\"STOCK\"}]}";

        // When: Handle market data update complete
        assertDoesNotThrow(() -> {
            marketDataService.onMarketDataUpdateComplete(message);
        });

        // Then: Should handle message without exception
        // Note: This method currently only logs the message
    }

    @Test
    @DisplayName("Should handle invalid market data update complete message")
    void shouldHandleInvalidMarketDataUpdateCompleteMessage() {
        // Given: Invalid message
        String message = "invalid json message";

        // When: Handle market data update complete
        assertDoesNotThrow(() -> {
            marketDataService.onMarketDataUpdateComplete(message);
        });

        // Then: Should handle invalid message without exception
        // Note: This method catches exceptions and logs them
    }

    @Test
    @DisplayName("Should validate asset type constants")
    void shouldValidateAssetTypeConstants() {
        // Given: Asset type constants
        AssetType stock = AssetType.STOCK;
        AssetType forex = AssetType.FOREX;
        AssetType crypto = AssetType.CRYPTO;
        AssetType commodity = AssetType.COMMODITY;

        // When & Then: Should have correct asset type names
        assertEquals("STOCK", stock.getAssetTypeName());
        assertEquals("FOREX", forex.getAssetTypeName());
        assertEquals("CRYPTO", crypto.getAssetTypeName());
        assertEquals("COMMODITY", commodity.getAssetTypeName());
    }

    @Test
    @DisplayName("Should validate Kafka topics constants")
    void shouldValidateKafkaTopicsConstants() {
        // Given: Kafka topics constants
        KafkaTopics updateRequest = KafkaTopics.MARKET_DATA_UPDATE_REQUEST;
        KafkaTopics updateComplete = KafkaTopics.MARKET_DATA_UPDATE_COMPLETE;

        // When & Then: Should have correct topic names
        assertNotNull(updateRequest.getTopicName());
        assertNotNull(updateComplete.getTopicName());
        assertTrue(updateRequest.getTopicName().length() > 0);
        assertTrue(updateComplete.getTopicName().length() > 0);
    }

    @Test
    @DisplayName("Should validate market data creation with correct data")
    void shouldValidateMarketDataCreationWithCorrectData() {
        // Given: Test market data
        MarketData marketData = createTestMarketData("AAPL", "STOCK", new BigDecimal("150.00"));

        // When & Then: Should have correct data
        assertEquals("AAPL", marketData.getSymbol());
        assertEquals(AssetType.STOCK, marketData.getAssetType());
        assertEquals(new BigDecimal("150.00"), marketData.getPrice());
        assertNotNull(marketData.getUpdatedAt());
    }

    @Test
    @DisplayName("Should validate market data with different asset types")
    void shouldValidateMarketDataWithDifferentAssetTypes() {
        // Given: Market data with different asset types
        MarketData stockData = createTestMarketData("AAPL", "STOCK", new BigDecimal("150.00"));
        MarketData forexData = createTestMarketData("EUR/USD", "FOREX", new BigDecimal("1.0850"));
        MarketData cryptoData = createTestMarketData("BTC/USD", "CRYPTO", new BigDecimal("45000.00"));
        MarketData commodityData = createTestMarketData("GOLD", "COMMODITY", new BigDecimal("1950.00"));

        // When & Then: Should have correct asset types
        assertEquals(AssetType.STOCK, stockData.getAssetType());
        assertEquals(AssetType.FOREX, forexData.getAssetType());
        assertEquals(AssetType.CRYPTO, cryptoData.getAssetType());
        assertEquals(AssetType.COMMODITY, commodityData.getAssetType());
    }

    @Test
    @DisplayName("Should validate market data with different price ranges")
    void shouldValidateMarketDataWithDifferentPriceRanges() {
        // Given: Market data with different price ranges
        MarketData lowPriceData = createTestMarketData("PENNY", "STOCK", new BigDecimal("0.01"));
        MarketData mediumPriceData = createTestMarketData("AAPL", "STOCK", new BigDecimal("150.00"));
        MarketData highPriceData = createTestMarketData("GOOGL", "STOCK", new BigDecimal("2800.00"));

        // When & Then: Should have correct prices
        assertEquals(new BigDecimal("0.01"), lowPriceData.getPrice());
        assertEquals(new BigDecimal("150.00"), mediumPriceData.getPrice());
        assertEquals(new BigDecimal("2800.00"), highPriceData.getPrice());
    }

    @Test
    @DisplayName("Should validate market data timestamp")
    void shouldValidateMarketDataTimestamp() {
        // Given: Market data
        MarketData marketData = createTestMarketData("AAPL", "STOCK", new BigDecimal("150.00"));

        // When & Then: Should have valid timestamp
        assertNotNull(marketData.getUpdatedAt());
        assertTrue(marketData.getUpdatedAt().isBefore(LocalDateTime.now().plusSeconds(1)));
        assertTrue(marketData.getUpdatedAt().isAfter(LocalDateTime.now().minusSeconds(1)));
    }

    private Map<String, String> createMap(String key1, String value1, String key2, String value2) {
        Map<String, String> map = new HashMap<>();
        map.put(key1, value1);
        map.put(key2, value2);
        return map;
    }

    private MarketData createTestMarketData(String symbol, String assetType, BigDecimal price) {
        MarketData marketData = new MarketData();
        marketData.setSymbol(symbol);
        marketData.setAssetType(AssetType.valueOf(assetType));
        marketData.setPrice(price);
        marketData.setUpdatedAt(LocalDateTime.now());
        return marketData;
    }
} 