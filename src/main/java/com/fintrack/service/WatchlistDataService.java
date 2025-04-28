package com.fintrack.service;

import com.fintrack.constants.AssetType;
import com.fintrack.model.AccountCurrency;
import com.fintrack.model.WatchlistData;
import com.fintrack.repository.WatchlistDataRepository;
import com.fintrack.repository.AccountCurrenciesRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class WatchlistDataService {

    private static final Logger logger = LoggerFactory.getLogger(WatchlistDataService.class);

    private final WatchlistDataRepository watchlistDataRepository;
    private final AccountCurrenciesRepository accountCurrenciesRepository;
    // private final KafkaProducerService kafkaProducerService;

    public WatchlistDataService(WatchlistDataRepository watchlistDataRepository
        , AccountCurrenciesRepository accountCurrenciesRepository) {
        this.watchlistDataRepository = watchlistDataRepository;
        this.accountCurrenciesRepository = accountCurrenciesRepository;
    }

    public List<WatchlistData> fetchWatchlistData(UUID accountId, List<String> assetTypes) {
        logger.info("Fetching watchlist data for accountId: " + accountId + " and assetTypes: " + assetTypes);
        return watchlistDataRepository.findWatchlistDataByAccountIdAndAssetTypes(accountId, assetTypes);
    }

    public void addWatchlistItem(UUID accountId, String symbol, String assetType) {
        logger.info("Adding item to watchlist: accountId={}, symbol={}, assetType={}", accountId, symbol, assetType);

        WatchlistData watchlistData = new WatchlistData();
        watchlistData.setAccountId(accountId);
        watchlistData.setSymbol(symbol);
        watchlistData.setAssetType(assetType);

        watchlistDataRepository.save(watchlistData);

        if ("FOREX".equalsIgnoreCase(assetType)) {
            handleForexCurrenciesOnAdd(accountId, symbol);
        }
    }

    public void removeWatchlistItem(UUID accountId, String symbol, String assetType) {
        logger.info("Removing item from watchlist: accountId={}, symbol={}, assetType={}", accountId, symbol, assetType);

        WatchlistData watchlistData = watchlistDataRepository.findByAccountIdAndSymbolAndAssetType(accountId, symbol, assetType);
        if (watchlistData != null) {
            watchlistDataRepository.delete(watchlistData); // Perform a hard delete

            if ("FOREX".equalsIgnoreCase(assetType)) {
                handleForexCurrenciesOnRemove(accountId, symbol);
            }
        } else {
            throw new IllegalArgumentException("Item not found in watchlist.");
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

        // If both currencies are new, send a Kafka message
        if (currenciesToAdd.contains(currencyFrom) && currenciesToAdd.contains(currencyTo)) {
            logger.info("No link between {} and existing currencies. Sending Kafka message for update.", symbol);
            // kafkaProducerService.sendCurrencyUpdateMessage(accountId, List.of(currencyFrom, currencyTo));
            // TODO: Handle Kafka response and update logic in the future
        }
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
        } else if (accountCurrency != null && accountCurrency.isDefault()) {
            logger.info("Currency {} is the default currency for account {} and will not be removed.", currency, accountId);
        }
    }

    // TODO: Ensure thread-safe operations for simultaneous updates to the watchlist and account_currencies table.
}