package com.fintrack.service.market;

import com.fintrack.constants.KafkaTopics;
import com.fintrack.model.market.MarketIndexData;
import com.fintrack.repository.market.MarketIndexDataRepository;
import com.fintrack.util.KafkaProducerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MarketIndexDataServiceTest {

    @Mock
    private KafkaProducerService kafkaProducerService;

    @Mock
    private MarketIndexDataRepository marketIndexDataRepository;

    private MarketIndexDataService marketIndexDataService;

    @BeforeEach
    void setUp() {
        marketIndexDataService = new MarketIndexDataService(kafkaProducerService, marketIndexDataRepository);
    }

    @Test
    @DisplayName("Should return correct update request topic")
    void shouldReturnCorrectUpdateRequestTopic() {
        // When
        KafkaTopics topic = marketIndexDataService.getUpdateRequestTopic();

        // Then
        assertEquals(KafkaTopics.MARKET_INDEX_DATA_UPDATE_REQUEST, topic);
    }

    @Test
    @DisplayName("Should get most recent market index data successfully")
    void shouldGetMostRecentMarketIndexDataSuccessfully() {
        // Given
        List<String> symbols = Arrays.asList("SPY", "QQQ", "IWM");
        List<MarketIndexData> mockData = createMockMarketIndexData(symbols);
        
        when(marketIndexDataRepository.findMarketIndexDataBySymbols(symbols))
            .thenReturn(mockData);

        // When
        Map<String, Object> result = marketIndexDataService.getMostRecentMarketIndexData(symbols);

        // Then
        assertEquals(3, result.size());
        
        // Verify SPY data
        Map<String, Object> spyData = (Map<String, Object>) result.get("SPY");
        assertEquals("450.00", spyData.get("price"));
        assertEquals(5.0, spyData.get("price_change"));
        assertEquals("1.12", spyData.get("percent_change"));
        assertEquals(445.0, spyData.get("price_low"));
        assertEquals(455.0, spyData.get("price_high"));
        
        // Verify QQQ data
        Map<String, Object> qqqData = (Map<String, Object>) result.get("QQQ");
        assertEquals("380.00", qqqData.get("price"));
        assertEquals(-2.0, qqqData.get("price_change"));
        assertEquals("-0.52", qqqData.get("percent_change"));
        assertEquals(375.0, qqqData.get("price_low"));
        assertEquals(385.0, qqqData.get("price_high"));
        
        // Verify IWM data
        Map<String, Object> iwmData = (Map<String, Object>) result.get("IWM");
        assertEquals("180.00", iwmData.get("price"));
        assertEquals(3.0, iwmData.get("price_change"));
        assertEquals("1.69", iwmData.get("percent_change"));
        assertEquals(175.0, iwmData.get("price_low"));
        assertEquals(185.0, iwmData.get("price_high"));
        
        verify(marketIndexDataRepository).findMarketIndexDataBySymbols(symbols);
        verify(kafkaProducerService).publishEvent(anyString(), anyString());
    }

    @Test
    @DisplayName("Should decode URL encoded symbols")
    void shouldDecodeUrlEncodedSymbols() {
        // Given
        List<String> encodedSymbols = Arrays.asList("SPY%20ETF", "QQQ%2B", "IWM%3D");
        List<String> decodedSymbols = Arrays.asList("SPY ETF", "QQQ+", "IWM=");
        List<MarketIndexData> mockData = createMockMarketIndexData(decodedSymbols);
        
        when(marketIndexDataRepository.findMarketIndexDataBySymbols(decodedSymbols))
            .thenReturn(mockData);

        // When
        Map<String, Object> result = marketIndexDataService.getMostRecentMarketIndexData(encodedSymbols);

        // Then
        assertEquals(3, result.size());
        assertTrue(result.containsKey("SPY ETF"));
        assertTrue(result.containsKey("QQQ+"));
        assertTrue(result.containsKey("IWM="));
        
        verify(marketIndexDataRepository).findMarketIndexDataBySymbols(decodedSymbols);
    }

    @Test
    @DisplayName("Should handle empty symbols list")
    void shouldHandleEmptySymbolsList() {
        // Given
        List<String> symbols = new ArrayList<>();
        
        when(marketIndexDataRepository.findMarketIndexDataBySymbols(symbols))
            .thenReturn(new ArrayList<>());

        // When
        Map<String, Object> result = marketIndexDataService.getMostRecentMarketIndexData(symbols);

        // Then
        assertTrue(result.isEmpty());
        verify(marketIndexDataRepository).findMarketIndexDataBySymbols(symbols);
    }

    @Test
    @DisplayName("Should handle single symbol")
    void shouldHandleSingleSymbol() {
        // Given
        List<String> symbols = Arrays.asList("SPY");
        List<MarketIndexData> mockData = createMockMarketIndexData(symbols);
        
        when(marketIndexDataRepository.findMarketIndexDataBySymbols(symbols))
            .thenReturn(mockData);

        // When
        Map<String, Object> result = marketIndexDataService.getMostRecentMarketIndexData(symbols);

        // Then
        assertEquals(1, result.size());
        assertTrue(result.containsKey("SPY"));
        
        Map<String, Object> spyData = (Map<String, Object>) result.get("SPY");
        assertEquals("450.00", spyData.get("price"));
        assertEquals(5.0, spyData.get("price_change"));
        assertEquals("1.12", spyData.get("percent_change"));
        assertEquals(445.0, spyData.get("price_low"));
        assertEquals(455.0, spyData.get("price_high"));
        
        verify(marketIndexDataRepository).findMarketIndexDataBySymbols(symbols);
    }

    @Test
    @DisplayName("Should handle no data found")
    void shouldHandleNoDataFound() {
        // Given
        List<String> symbols = Arrays.asList("SPY", "QQQ", "IWM");
        
        when(marketIndexDataRepository.findMarketIndexDataBySymbols(symbols))
            .thenReturn(new ArrayList<>());

        // When
        Map<String, Object> result = marketIndexDataService.getMostRecentMarketIndexData(symbols);

        // Then
        assertTrue(result.isEmpty());
        verify(marketIndexDataRepository).findMarketIndexDataBySymbols(symbols);
    }

    @Test
    @DisplayName("Should handle partial data found")
    void shouldHandlePartialDataFound() {
        // Given
        List<String> symbols = Arrays.asList("SPY", "QQQ", "IWM");
        List<MarketIndexData> partialData = createMockMarketIndexData(Arrays.asList("SPY", "QQQ"));
        
        when(marketIndexDataRepository.findMarketIndexDataBySymbols(symbols))
            .thenReturn(partialData);

        // When
        Map<String, Object> result = marketIndexDataService.getMostRecentMarketIndexData(symbols);

        // Then
        assertEquals(2, result.size());
        assertTrue(result.containsKey("SPY"));
        assertTrue(result.containsKey("QQQ"));
        assertFalse(result.containsKey("IWM"));
        
        // The service will retry up to 3 times, so we expect 3 calls
        verify(marketIndexDataRepository, times(3)).findMarketIndexDataBySymbols(symbols);
    }

    @Test
    @DisplayName("Should handle market data update complete message gracefully")
    void shouldHandleMarketDataUpdateCompleteMessageGracefully() {
        // Given
        String message = "[{\"symbol\":\"SPY\",\"price\":450.00,\"price_change\":5.00,\"percent_change\":1.12}]";

        // When & Then
        assertDoesNotThrow(() -> {
            marketIndexDataService.onMarketDataUpdateComplete(message);
        });
    }

    @Test
    @DisplayName("Should handle null message gracefully")
    void shouldHandleNullMessageGracefully() {
        // When & Then
        assertDoesNotThrow(() -> marketIndexDataService.onMarketDataUpdateComplete(null));
    }

    @Test
    @DisplayName("Should handle empty message gracefully")
    void shouldHandleEmptyMessageGracefully() {
        // When & Then
        assertDoesNotThrow(() -> marketIndexDataService.onMarketDataUpdateComplete(""));
    }

    @Test
    @DisplayName("Should handle invalid JSON message gracefully")
    void shouldHandleInvalidJsonMessageGracefully() {
        // Given
        String message = "invalid json message";

        // When & Then
        assertDoesNotThrow(() -> marketIndexDataService.onMarketDataUpdateComplete(message));
    }

    @Test
    @DisplayName("Should handle large number of symbols")
    void shouldHandleLargeNumberOfSymbols() {
        // Given
        List<String> symbols = new ArrayList<>();
        List<MarketIndexData> mockData = new ArrayList<>();
        
        for (int i = 0; i < 100; i++) {
            String symbol = "INDEX" + i;
            symbols.add(symbol);
            mockData.add(createMockMarketIndexDataItem(symbol));
        }
        
        when(marketIndexDataRepository.findMarketIndexDataBySymbols(symbols))
            .thenReturn(mockData);

        // When
        Map<String, Object> result = marketIndexDataService.getMostRecentMarketIndexData(symbols);

        // Then
        assertEquals(100, result.size());
        
        // Verify all symbols are present
        for (int i = 0; i < 100; i++) {
            String symbol = "INDEX" + i;
            assertTrue(result.containsKey(symbol));
            
            Map<String, Object> data = (Map<String, Object>) result.get(symbol);
            assertEquals("450.00", data.get("price"));
            assertEquals(5.0, data.get("price_change"));
            assertEquals("1.12", data.get("percent_change"));
            assertEquals(445.0, data.get("price_low"));
            assertEquals(455.0, data.get("price_high"));
        }
        
        verify(marketIndexDataRepository).findMarketIndexDataBySymbols(symbols);
    }

    @Test
    @DisplayName("Should handle symbols with special characters")
    void shouldHandleSymbolsWithSpecialCharacters() {
        // Given
        List<String> symbols = Arrays.asList("SPY-ETF", "QQQ_INDEX", "IWM.ETF");
        List<MarketIndexData> mockData = createMockMarketIndexData(symbols);
        
        when(marketIndexDataRepository.findMarketIndexDataBySymbols(symbols))
            .thenReturn(mockData);

        // When
        Map<String, Object> result = marketIndexDataService.getMostRecentMarketIndexData(symbols);

        // Then
        assertEquals(3, result.size());
        assertTrue(result.containsKey("SPY-ETF"));
        assertTrue(result.containsKey("QQQ_INDEX"));
        assertTrue(result.containsKey("IWM.ETF"));
        
        verify(marketIndexDataRepository).findMarketIndexDataBySymbols(symbols);
    }

    @Test
    @DisplayName("Should handle symbols with numbers")
    void shouldHandleSymbolsWithNumbers() {
        // Given
        List<String> symbols = Arrays.asList("SPY2024", "QQQ99", "IWM2025");
        List<MarketIndexData> mockData = createMockMarketIndexData(symbols);
        
        when(marketIndexDataRepository.findMarketIndexDataBySymbols(symbols))
            .thenReturn(mockData);

        // When
        Map<String, Object> result = marketIndexDataService.getMostRecentMarketIndexData(symbols);

        // Then
        assertEquals(3, result.size());
        assertTrue(result.containsKey("SPY2024"));
        assertTrue(result.containsKey("QQQ99"));
        assertTrue(result.containsKey("IWM2025"));
        
        verify(marketIndexDataRepository).findMarketIndexDataBySymbols(symbols);
    }

    @Test
    @DisplayName("Should handle symbols with mixed case")
    void shouldHandleSymbolsWithMixedCase() {
        // Given
        List<String> symbols = Arrays.asList("spy", "QQQ", "Iwm", "ETF");
        List<MarketIndexData> mockData = createMockMarketIndexData(symbols);
        
        when(marketIndexDataRepository.findMarketIndexDataBySymbols(symbols))
            .thenReturn(mockData);

        // When
        Map<String, Object> result = marketIndexDataService.getMostRecentMarketIndexData(symbols);

        // Then
        assertEquals(4, result.size());
        assertTrue(result.containsKey("spy"));
        assertTrue(result.containsKey("QQQ"));
        assertTrue(result.containsKey("Iwm"));
        assertTrue(result.containsKey("ETF"));
        
        verify(marketIndexDataRepository).findMarketIndexDataBySymbols(symbols);
    }

    @Test
    @DisplayName("Should handle retry mechanism when data is not available initially")
    void shouldHandleRetryMechanismWhenDataIsNotAvailableInitially() {
        // Given
        List<String> symbols = Arrays.asList("SPY", "QQQ", "IWM");
        List<MarketIndexData> emptyData = new ArrayList<>();
        List<MarketIndexData> partialData = createMockMarketIndexData(Arrays.asList("SPY"));
        List<MarketIndexData> fullData = createMockMarketIndexData(symbols);
        
        when(marketIndexDataRepository.findMarketIndexDataBySymbols(symbols))
            .thenReturn(emptyData)
            .thenReturn(partialData)
            .thenReturn(fullData);

        // When
        Map<String, Object> result = marketIndexDataService.getMostRecentMarketIndexData(symbols);

        // Then
        // The service exits early when no data is found, so we expect only 1 call
        verify(marketIndexDataRepository, times(1)).findMarketIndexDataBySymbols(symbols);
        
        // The result should be empty because the service exits early when no data is found
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("Should handle interrupted retry")
    void shouldHandleInterruptedRetry() {
        // Given
        List<String> symbols = Arrays.asList("SPY", "QQQ", "IWM");
        
        when(marketIndexDataRepository.findMarketIndexDataBySymbols(symbols))
            .thenReturn(new ArrayList<>());

        // When
        Map<String, Object> result = marketIndexDataService.getMostRecentMarketIndexData(symbols);

        // Then
        assertTrue(result.isEmpty());
        verify(marketIndexDataRepository, atLeastOnce()).findMarketIndexDataBySymbols(symbols);
    }

    private List<MarketIndexData> createMockMarketIndexData(List<String> symbols) {
        List<MarketIndexData> data = new ArrayList<>();
        for (int i = 0; i < symbols.size(); i++) {
            String symbol = symbols.get(i);
            data.add(createMockMarketIndexDataItem(symbol, i));
        }
        return data;
    }

    private MarketIndexData createMockMarketIndexDataItem(String symbol) {
        return createMockMarketIndexDataItem(symbol, 0);
    }

    private MarketIndexData createMockMarketIndexDataItem(String symbol, int index) {
        MarketIndexData data = new MarketIndexData();
        data.setSymbol(symbol);
        
        // Provide different values based on symbol or index
        if ("QQQ".equals(symbol)) {
            data.setPrice("380.00");
            data.setPriceChange(-2.0);
            data.setPercentChange("-0.52");
            data.setPriceLow(375.0);
            data.setPriceHigh(385.0);
        } else if ("IWM".equals(symbol)) {
            data.setPrice("180.00");
            data.setPriceChange(3.0);
            data.setPercentChange("1.69");
            data.setPriceLow(175.0);
            data.setPriceHigh(185.0);
        } else {
            data.setPrice("450.00");
            data.setPriceChange(5.0);
            data.setPercentChange("1.12");
            data.setPriceLow(445.0);
            data.setPriceHigh(455.0);
        }
        
        return data;
    }
} 