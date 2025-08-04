package com.fintrack.service.market;

import com.fintrack.constants.finance.AssetType;
import com.fintrack.model.finance.AccountCurrency;
import com.fintrack.model.market.WatchlistData;
import com.fintrack.repository.finance.AccountCurrenciesRepository;
import com.fintrack.repository.market.WatchlistDataRepository;
import com.fintrack.util.KafkaProducerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("WatchlistDataService Tests")
class WatchlistDataServiceTest {

    @Mock
    private WatchlistDataRepository watchlistDataRepository;

    @Mock
    private AccountCurrenciesRepository accountCurrenciesRepository;

    @Mock
    private ForexMarketDataService forexMarketDataService;

    @Mock
    private StockMarketDataService stockMarketDataService;

    @Mock
    private CryptoMarketDataService cryptoMarketDataService;

    @Mock
    private CommodityMarketDataService commodityMarketDataService;

    @Mock
    private KafkaProducerService kafkaProducerService;

    private WatchlistDataService watchlistDataService;

    private static final UUID TEST_ACCOUNT_ID = UUID.randomUUID();
    private static final String TEST_SYMBOL = "AAPL";
    private static final String TEST_FOREX_SYMBOL = "AUD/USD";
    private static final String TEST_CRYPTO_SYMBOL = "BTC/USD";

    @BeforeEach
    void setUp() {
        watchlistDataService = new WatchlistDataService(
            watchlistDataRepository,
            accountCurrenciesRepository,
            forexMarketDataService,
            stockMarketDataService,
            cryptoMarketDataService,
            commodityMarketDataService,
            kafkaProducerService
        );
    }

    @Test
    @DisplayName("Should fetch watchlist data successfully")
    void shouldFetchWatchlistDataSuccessfully() {
        // Given: Valid account ID and asset types
        List<String> assetTypes = Arrays.asList("STOCK", "FOREX");
        List<WatchlistData> expectedData = Arrays.asList(
            createTestWatchlistData(TEST_ACCOUNT_ID, "AAPL", AssetType.STOCK),
            createTestWatchlistData(TEST_ACCOUNT_ID, "AUD/USD", AssetType.FOREX)
        );
        
        when(watchlistDataRepository.findWatchlistDataByAccountIdAndAssetTypes(TEST_ACCOUNT_ID, assetTypes))
            .thenReturn(expectedData);

        // When: Fetching watchlist data
        List<WatchlistData> result = watchlistDataService.fetchWatchlistData(TEST_ACCOUNT_ID, assetTypes);

        // Then: Should return expected data
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals(expectedData, result);
        verify(watchlistDataRepository).findWatchlistDataByAccountIdAndAssetTypes(TEST_ACCOUNT_ID, assetTypes);
    }

    @Test
    @DisplayName("Should add stock watchlist item successfully")
    void shouldAddStockWatchlistItemSuccessfully() {
        // Given: Valid stock watchlist item
        when(watchlistDataRepository.save(any(WatchlistData.class))).thenReturn(createTestWatchlistData(TEST_ACCOUNT_ID, TEST_SYMBOL, AssetType.STOCK));
        when(stockMarketDataService.fetchMarketData(eq(TEST_ACCOUNT_ID), anyList())).thenReturn(Arrays.asList());

        // When: Adding stock watchlist item
        watchlistDataService.addWatchlistItem(TEST_ACCOUNT_ID, TEST_SYMBOL, "STOCK");

        // Then: Should save and request market data
        verify(watchlistDataRepository).save(argThat(data -> 
            data.getAccountId().equals(TEST_ACCOUNT_ID) &&
            data.getSymbol().equals(TEST_SYMBOL) &&
            data.getAssetType().equals(AssetType.STOCK)
        ));
        verify(stockMarketDataService).fetchMarketData(TEST_ACCOUNT_ID, Arrays.asList(TEST_SYMBOL));
    }

    @Test
    @DisplayName("Should add forex watchlist item successfully")
    void shouldAddForexWatchlistItemSuccessfully() {
        // Given: Valid forex watchlist item
        when(watchlistDataRepository.save(any(WatchlistData.class))).thenReturn(createTestWatchlistData(TEST_ACCOUNT_ID, TEST_FOREX_SYMBOL, AssetType.FOREX));
        when(accountCurrenciesRepository.findByAccountId(TEST_ACCOUNT_ID)).thenReturn(Arrays.asList());
        when(accountCurrenciesRepository.save(any(AccountCurrency.class))).thenReturn(new AccountCurrency(TEST_ACCOUNT_ID, "AUD", false));
        when(forexMarketDataService.fetchMarketData(eq(TEST_ACCOUNT_ID), anyList())).thenReturn(Arrays.asList());

        // When: Adding forex watchlist item
        watchlistDataService.addWatchlistItem(TEST_ACCOUNT_ID, TEST_FOREX_SYMBOL, "FOREX");

        // Then: Should save, add currencies, and request market data
        verify(watchlistDataRepository).save(argThat(data -> 
            data.getAccountId().equals(TEST_ACCOUNT_ID) &&
            data.getSymbol().equals(TEST_FOREX_SYMBOL) &&
            data.getAssetType().equals(AssetType.FOREX)
        ));
        verify(accountCurrenciesRepository).findByAccountId(TEST_ACCOUNT_ID);
        verify(accountCurrenciesRepository, times(2)).save(any(AccountCurrency.class));
        verify(forexMarketDataService).fetchMarketData(TEST_ACCOUNT_ID, Arrays.asList(TEST_FOREX_SYMBOL));
    }

    @Test
    @DisplayName("Should add crypto watchlist item successfully")
    void shouldAddCryptoWatchlistItemSuccessfully() {
        // Given: Valid crypto watchlist item
        when(watchlistDataRepository.save(any(WatchlistData.class))).thenReturn(createTestWatchlistData(TEST_ACCOUNT_ID, TEST_CRYPTO_SYMBOL, AssetType.CRYPTO));
        when(cryptoMarketDataService.fetchMarketData(eq(TEST_ACCOUNT_ID), anyList())).thenReturn(Arrays.asList());

        // When: Adding crypto watchlist item
        watchlistDataService.addWatchlistItem(TEST_ACCOUNT_ID, TEST_CRYPTO_SYMBOL, "CRYPTO");

        // Then: Should save and request market data
        verify(watchlistDataRepository).save(argThat(data -> 
            data.getAccountId().equals(TEST_ACCOUNT_ID) &&
            data.getSymbol().equals(TEST_CRYPTO_SYMBOL) &&
            data.getAssetType().equals(AssetType.CRYPTO)
        ));
        verify(cryptoMarketDataService).fetchMarketData(TEST_ACCOUNT_ID, Arrays.asList(TEST_CRYPTO_SYMBOL));
    }

    @Test
    @DisplayName("Should throw exception for invalid asset type")
    void shouldThrowExceptionForInvalidAssetType() {
        // Given: Invalid asset type
        String invalidAssetType = "INVALID";

        // When & Then: Should throw exception
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> watchlistDataService.addWatchlistItem(TEST_ACCOUNT_ID, TEST_SYMBOL, invalidAssetType)
        );
        assertTrue(exception.getMessage().contains("Invalid asset type"));
        verifyNoInteractions(watchlistDataRepository);
    }

    @Test
    @DisplayName("Should remove watchlist item successfully")
    void shouldRemoveWatchlistItemSuccessfully() {
        // Given: Existing watchlist item
        WatchlistData existingItem = createTestWatchlistData(TEST_ACCOUNT_ID, TEST_SYMBOL, AssetType.STOCK);
        when(watchlistDataRepository.findByAccountIdAndSymbolAndAssetType(TEST_ACCOUNT_ID, TEST_SYMBOL, "STOCK"))
            .thenReturn(existingItem);
        doNothing().when(watchlistDataRepository).delete(existingItem);

        // When: Removing watchlist item
        watchlistDataService.removeWatchlistItem(TEST_ACCOUNT_ID, TEST_SYMBOL, "STOCK");

        // Then: Should delete the item
        verify(watchlistDataRepository).findByAccountIdAndSymbolAndAssetType(TEST_ACCOUNT_ID, TEST_SYMBOL, "STOCK");
        verify(watchlistDataRepository).delete(existingItem);
    }

    @Test
    @DisplayName("Should throw exception when removing non-existent item")
    void shouldThrowExceptionWhenRemovingNonExistentItem() {
        // Given: Non-existent watchlist item
        when(watchlistDataRepository.findByAccountIdAndSymbolAndAssetType(TEST_ACCOUNT_ID, TEST_SYMBOL, "STOCK"))
            .thenReturn(null);

        // When & Then: Should throw exception
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> watchlistDataService.removeWatchlistItem(TEST_ACCOUNT_ID, TEST_SYMBOL, "STOCK")
        );
        assertEquals("Item not found in watchlist.", exception.getMessage());
        verify(watchlistDataRepository).findByAccountIdAndSymbolAndAssetType(TEST_ACCOUNT_ID, TEST_SYMBOL, "STOCK");
        verifyNoMoreInteractions(watchlistDataRepository);
    }

    @Test
    @DisplayName("Should handle forex currencies on add")
    void shouldHandleForexCurrenciesOnAdd() {
        // Given: Forex symbol and existing currencies
        String forexSymbol = "EUR/USD";
        List<AccountCurrency> existingCurrencies = Arrays.asList(
            new AccountCurrency(TEST_ACCOUNT_ID, "USD", true)
        );
        
        when(watchlistDataRepository.save(any(WatchlistData.class))).thenReturn(createTestWatchlistData(TEST_ACCOUNT_ID, forexSymbol, AssetType.FOREX));
        when(accountCurrenciesRepository.findByAccountId(TEST_ACCOUNT_ID)).thenReturn(existingCurrencies);
        when(accountCurrenciesRepository.save(any(AccountCurrency.class))).thenReturn(new AccountCurrency(TEST_ACCOUNT_ID, "EUR", false));
        when(forexMarketDataService.fetchMarketData(eq(TEST_ACCOUNT_ID), anyList())).thenReturn(Arrays.asList());

        // When: Adding forex watchlist item
        watchlistDataService.addWatchlistItem(TEST_ACCOUNT_ID, forexSymbol, "FOREX");

        // Then: Should add only EUR currency (USD already exists)
        verify(accountCurrenciesRepository).findByAccountId(TEST_ACCOUNT_ID);
        verify(accountCurrenciesRepository).save(argThat(currency -> 
            currency.getAccountId().equals(TEST_ACCOUNT_ID) &&
            currency.getCurrency().equals("EUR") &&
            !currency.isDefault()
        ));
    }

    @Test
    @DisplayName("Should handle invalid forex symbol format")
    void shouldHandleInvalidForexSymbolFormat() {
        // Given: Invalid forex symbol
        String invalidForexSymbol = "INVALID_SYMBOL";
        when(watchlistDataRepository.save(any(WatchlistData.class))).thenReturn(createTestWatchlistData(TEST_ACCOUNT_ID, invalidForexSymbol, AssetType.FOREX));
        when(forexMarketDataService.fetchMarketData(eq(TEST_ACCOUNT_ID), anyList())).thenReturn(Arrays.asList());

        // When: Adding invalid forex watchlist item
        watchlistDataService.addWatchlistItem(TEST_ACCOUNT_ID, invalidForexSymbol, "FOREX");

        // Then: Should still save the watchlist item but not add currencies
        verify(watchlistDataRepository).save(any(WatchlistData.class));
        verifyNoInteractions(accountCurrenciesRepository);
    }

    @Test
    @DisplayName("Should request market data updates for multiple items")
    void shouldRequestMarketDataUpdatesForMultipleItems() {
        // Given: Multiple watchlist items
        List<WatchlistData> watchlistItems = Arrays.asList(
            createTestWatchlistData(TEST_ACCOUNT_ID, "AAPL", AssetType.STOCK),
            createTestWatchlistData(TEST_ACCOUNT_ID, "GOOGL", AssetType.STOCK),
            createTestWatchlistData(TEST_ACCOUNT_ID, "AUD/USD", AssetType.FOREX),
            createTestWatchlistData(TEST_ACCOUNT_ID, "BTC/USD", AssetType.CRYPTO)
        );
        
        when(stockMarketDataService.fetchMarketData(eq(TEST_ACCOUNT_ID), anyList())).thenReturn(Arrays.asList());
        when(forexMarketDataService.fetchMarketData(eq(TEST_ACCOUNT_ID), anyList())).thenReturn(Arrays.asList());
        when(cryptoMarketDataService.fetchMarketData(eq(TEST_ACCOUNT_ID), anyList())).thenReturn(Arrays.asList());

        // When: Requesting market data updates
        watchlistDataService.requestMarketDataUpdates(TEST_ACCOUNT_ID, watchlistItems);

        // Then: Should request updates for each asset type
        verify(stockMarketDataService).fetchMarketData(TEST_ACCOUNT_ID, Arrays.asList("AAPL", "GOOGL"));
        verify(forexMarketDataService).fetchMarketData(TEST_ACCOUNT_ID, Arrays.asList("AUD/USD"));
        verify(cryptoMarketDataService).fetchMarketData(TEST_ACCOUNT_ID, Arrays.asList("BTC/USD"));
    }

    @Test
    @DisplayName("Should handle empty watchlist items")
    void shouldHandleEmptyWatchlistItems() {
        // Given: Empty watchlist items
        List<WatchlistData> watchlistItems = Arrays.asList();

        // When: Requesting market data updates
        watchlistDataService.requestMarketDataUpdates(TEST_ACCOUNT_ID, watchlistItems);

        // Then: Should not request any market data updates
        verifyNoInteractions(stockMarketDataService, forexMarketDataService, cryptoMarketDataService, commodityMarketDataService);
    }

    @Test
    @DisplayName("Should handle commodity asset type")
    void shouldHandleCommodityAssetType() {
        // Given: Watchlist item with commodity asset type
        WatchlistData commodityItem = createTestWatchlistData(TEST_ACCOUNT_ID, "GOLD", AssetType.COMMODITY);
        List<WatchlistData> watchlistItems = Arrays.asList(commodityItem);
        
        when(commodityMarketDataService.fetchMarketData(eq(TEST_ACCOUNT_ID), anyList())).thenReturn(Arrays.asList());

        // When: Requesting market data updates
        watchlistDataService.requestMarketDataUpdates(TEST_ACCOUNT_ID, watchlistItems);

        // Then: Should request market data updates for commodity
        verify(commodityMarketDataService).fetchMarketData(TEST_ACCOUNT_ID, Arrays.asList("GOLD"));
    }

    private WatchlistData createTestWatchlistData(UUID accountId, String symbol, AssetType assetType) {
        WatchlistData data = new WatchlistData();
        data.setAccountId(accountId);
        data.setSymbol(symbol);
        data.setAssetType(assetType);
        return data;
    }
} 