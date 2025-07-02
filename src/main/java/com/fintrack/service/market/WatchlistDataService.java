package com.fintrack.service.market;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fintrack.constants.KafkaTopics;
import com.fintrack.constants.finance.AssetType;
import com.fintrack.model.finance.AccountCurrency;
import com.fintrack.model.market.WatchlistData;
import com.fintrack.repository.finance.AccountCurrenciesRepository;
import com.fintrack.repository.finance.HoldingsMonthlyRepository;
import com.fintrack.repository.market.WatchlistDataRepository;
import com.fintrack.service.market.interfaces.WatchlistDataOperations;
import com.fintrack.util.KafkaProducerService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;

/**
 * Service for managing watchlist data and requesting market data updates for watchlist items.
 */
@Service
public class WatchlistDataService implements WatchlistDataOperations {

    private static final Logger logger = LoggerFactory.getLogger(WatchlistDataService.class);

    private final WatchlistDataRepository watchlistDataRepository;
    private final AccountCurrenciesRepository accountCurrenciesRepository;
    private final ForexMarketDataService forexMarketDataService;
    private final StockMarketDataService stockMarketDataService;
    private final CryptoMarketDataService cryptoMarketDataService;
    private final CommodityMarketDataService commodityMarketDataService;
    private final KafkaProducerService kafkaProducerService;

    public WatchlistDataService(
        WatchlistDataRepository watchlistDataRepository,
        AccountCurrenciesRepository accountCurrenciesRepository,
        ForexMarketDataService forexMarketDataService,
        StockMarketDataService stockMarketDataService,
        CryptoMarketDataService cryptoMarketDataService,
        CommodityMarketDataService commodityMarketDataService,
        KafkaProducerService kafkaProducerService) {
        this.watchlistDataRepository = watchlistDataRepository;
        this.accountCurrenciesRepository = accountCurrenciesRepository;
        this.forexMarketDataService = forexMarketDataService;
        this.stockMarketDataService = stockMarketDataService;
        this.cryptoMarketDataService = cryptoMarketDataService;
        this.commodityMarketDataService = commodityMarketDataService;
        this.kafkaProducerService = kafkaProducerService;
    }

    @Override
    public List<WatchlistData> fetchWatchlistData(UUID accountId, List<String> assetTypes) {
        logger.info("Fetching watchlist data for accountId: {} and assetTypes: {}", accountId, assetTypes);
        return watchlistDataRepository.findWatchlistDataByAccountIdAndAssetTypes(accountId, assetTypes);
    }

    /**
     * Safely convert a string asset type to AssetType enum with proper error handling.
     * 
     * @param assetType The asset type string to convert
     * @param symbol The symbol for logging purposes
     * @return The AssetType enum value
     * @throws IllegalArgumentException if the asset type is invalid
     */
    private AssetType convertToAssetType(String assetType, String symbol) {
        try {
            return AssetType.valueOf(assetType.toUpperCase());
        } catch (IllegalArgumentException e) {
            logger.error("Invalid asset type '{}' provided for symbol '{}'. Valid types are: {}", 
                assetType, symbol, java.util.Arrays.toString(AssetType.values()));
            throw new IllegalArgumentException("Invalid asset type: " + assetType + 
                ". Valid types are: " + java.util.Arrays.toString(AssetType.values()));
        }
    }

    @Override
    public void addWatchlistItem(UUID accountId, String symbol, String assetType) {
        logger.info("Adding item to watchlist: accountId={}, symbol={}, assetType={}", accountId, symbol, assetType);

        WatchlistData watchlistData = new WatchlistData();
        watchlistData.setAccountId(accountId);
        watchlistData.setSymbol(symbol);
        
        // Convert string assetType to AssetType enum with proper error handling
        AssetType assetTypeEnum = convertToAssetType(assetType, symbol);
        watchlistData.setAssetType(assetTypeEnum);

        watchlistDataRepository.save(watchlistData);

        // Handle specific asset type processing
        if (AssetType.FOREX.getAssetTypeName().equalsIgnoreCase(assetType)) {
            handleForexCurrenciesOnAdd(accountId, symbol);
        }
        
        // Request market data update for the new watchlist item
        requestMarketDataUpdate(accountId, symbol, assetType);
    }

    @Override
    public void removeWatchlistItem(UUID accountId, String symbol, String assetType) {
        logger.info("Removing item from watchlist: accountId={}, symbol={}, assetType={}", accountId, symbol, assetType);

        WatchlistData watchlistData = watchlistDataRepository.findByAccountIdAndSymbolAndAssetType(accountId, symbol, assetType);
        if (watchlistData != null) {
            watchlistDataRepository.delete(watchlistData); // Perform a hard delete

            if (AssetType.FOREX.getAssetTypeName().equalsIgnoreCase(assetType)) {
                handleForexCurrenciesOnRemove(accountId, symbol);
            }
        } else {
            throw new IllegalArgumentException("Item not found in watchlist.");
        }
    }

    /**
     * Request market data update for a specific watchlist item based on its asset type.
     * 
     * @param accountId The account ID
     * @param symbol The symbol
     * @param assetType The asset type
     */
    private void requestMarketDataUpdate(UUID accountId, String symbol, String assetType) {
        try {
            if (AssetType.STOCK.getAssetTypeName().equalsIgnoreCase(assetType)) {
                logger.info("Requesting stock market data update for symbol: {}", symbol);
                stockMarketDataService.fetchMarketData(accountId, List.of(symbol));
            } else if (AssetType.FOREX.getAssetTypeName().equalsIgnoreCase(assetType)) {
                logger.info("Requesting forex market data update for symbol: {}", symbol);
                forexMarketDataService.fetchMarketData(accountId, List.of(symbol));
            } else if (AssetType.CRYPTO.getAssetTypeName().equalsIgnoreCase(assetType)) {
                logger.info("Requesting crypto market data update for symbol: {}", symbol);
                cryptoMarketDataService.fetchMarketData(accountId, List.of(symbol));
            } else if (AssetType.COMMODITY.getAssetTypeName().equalsIgnoreCase(assetType)) {
                logger.info("Requesting commodity market data update for symbol: {}", symbol);
                commodityMarketDataService.fetchMarketData(accountId, List.of(symbol));
            } else {
                logger.warn("Unknown asset type: {}. No market data update requested.", assetType);
            }
        } catch (Exception e) {
            logger.error("Error requesting market data update for symbol {}: {}", symbol, e.getMessage());
        }
    }

    @Override
    public void requestMarketDataUpdates(UUID accountId, List<WatchlistData> watchlistItems) {
        logger.info("Requesting market data updates for {} watchlist items", watchlistItems.size());
        
        // Group by asset type for more efficient processing
        Map<String, List<String>> symbolsByAssetType = new HashMap<>();
        
        for (WatchlistData item : watchlistItems) {
            String assetTypeStr = item.getAssetType().getAssetTypeName();
            symbolsByAssetType
                .computeIfAbsent(assetTypeStr, k -> new ArrayList<>())
                .add(item.getSymbol());
        }
        
        // Process each asset type
        for (Map.Entry<String, List<String>> entry : symbolsByAssetType.entrySet()) {
            String assetType = entry.getKey();
            List<String> symbols = entry.getValue();
            
            try {
                if (AssetType.STOCK.getAssetTypeName().equalsIgnoreCase(assetType)) {
                    logger.info("Requesting stock market data updates for {} symbols", symbols.size());
                    stockMarketDataService.fetchMarketData(accountId, symbols);
                } else if (AssetType.FOREX.getAssetTypeName().equalsIgnoreCase(assetType)) {
                    logger.info("Requesting forex market data updates for {} symbols", symbols.size());
                    forexMarketDataService.fetchMarketData(accountId, symbols);
                } else if (AssetType.CRYPTO.getAssetTypeName().equalsIgnoreCase(assetType)) {
                    logger.info("Requesting crypto market data updates for {} symbols", symbols.size());
                    cryptoMarketDataService.fetchMarketData(accountId, symbols);
                } else if (AssetType.COMMODITY.getAssetTypeName().equalsIgnoreCase(assetType)) {
                    logger.info("Requesting commodity market data updates for {} symbols", symbols.size());
                    commodityMarketDataService.fetchMarketData(accountId, symbols);
                } else {
                    logger.warn("Unknown asset type: {}. No market data updates requested.", assetType);
                }
            } catch (Exception e) {
                logger.error("Error requesting market data updates for asset type {}: {}", assetType, e.getMessage());
            }
        }
    }

    private void handleForexCurrenciesOnAdd(UUID accountId, String symbol) {
        String[] currencies = symbol.split("/"); // Split the symbol into currencies (e.g., "AUD/USD" -> ["AUD", "USD"])
        if (currencies.length != 2) {
            logger.warn("Invalid FOREX symbol format: {}", symbol);
            return;
        }

        String currencyFrom = currencies[0];
        String currencyTo = currencies[1];

        logger.info("Handling FOREX currencies for accountId: {}, currencyFrom: {}, currencyTo: {}", accountId, currencyFrom, currencyTo);

        // Fetch existing currencies for the account
        List<AccountCurrency> existingCurrencies = accountCurrenciesRepository.findByAccountId(accountId);
        List<String> existingCurrencyCodes = existingCurrencies.stream()
                .map(AccountCurrency::getCurrency)
                .toList();
    
        logger.info("Existing currencies for account {}: {}", accountId, existingCurrencyCodes);
    
        // Filter out currencies that need to be added
        List<String> currenciesToAdd = List.of(currencyFrom, currencyTo).stream()
                .filter(currency -> !existingCurrencyCodes.contains(currency))
                .toList();

        logger.info("Currencies to add for account {}: {}", accountId, currenciesToAdd);

        // Add the filtered currencies
        for (String currency : currenciesToAdd) {
            try {
                accountCurrenciesRepository.save(new AccountCurrency(accountId, currency, false));
                logger.info("Added currency {} for account {}", currency, accountId);
            } catch (Exception e) {
                logger.error("Failed to add currency {} for account {}: {}", currency, accountId, e.getMessage());
            }
        }

        // Note: Market data update is now handled by the requestMarketDataUpdate method
    }

    private void handleForexCurrenciesOnRemove(UUID accountId, String symbol) {
        String[] currencies = symbol.split("/"); // Split the symbol into currencies (e.g., "AUD/USD" -> ["AUD", "USD"])
        if (currencies.length != 2) {
            logger.warn("Invalid FOREX symbol format: {}", symbol);
            return;
        }

        String currencyFrom = currencies[0];
        String currencyTo = currencies[1];

        // Fetch all remaining FOREX watchlist items for the account
        List<WatchlistData> remainingForexData = watchlistDataRepository.findWatchlistDataByAccountIdAndAssetTypes(accountId, List.of(AssetType.FOREX.getAssetTypeName()));
        List<String> remainingSymbols = remainingForexData.stream()
                .map(WatchlistData::getSymbol)
                .toList();

        logger.info("Remaining FOREX symbols for account {}: {}", accountId, remainingSymbols);

        boolean currencyFromStillUsed = remainingSymbols.stream().anyMatch(s -> s.contains(currencyFrom));
        boolean currencyToStillUsed = remainingSymbols.stream().anyMatch(s -> s.contains(currencyTo));

        // Remove currencies that are no longer used
        if (!currencyFromStillUsed) {
            removeCurrencyIfNotDefault(accountId, currencyFrom);
        }
        if (!currencyToStillUsed) {
            removeCurrencyIfNotDefault(accountId, currencyTo);
        }
    }

    private void removeCurrencyIfNotDefault(UUID accountId, String currency) {
        AccountCurrency accountCurrency = accountCurrenciesRepository.findByAccountIdAndCurrency(accountId, currency)
                .orElse(null);
        if (accountCurrency != null && !accountCurrency.isDefault()) {
            accountCurrenciesRepository.delete(accountCurrency);
            logger.info("Removed currency {} for account {}", currency, accountId);
        } else if (accountCurrency != null && accountCurrency.isDefault()) {
            logger.info("Currency {} is the default currency for account {} and will not be removed.", currency, accountId);
        }
    }

    // TODO: Ensure thread-safe operations for simultaneous updates to the watchlist and account_currencies table.
}