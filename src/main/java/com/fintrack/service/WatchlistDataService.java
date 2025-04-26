package com.fintrack.service;

import com.fintrack.model.WatchlistData;
import com.fintrack.repository.WatchlistDataRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class WatchlistDataService {

    private static final Logger logger = LoggerFactory.getLogger(WatchlistDataService.class);

    private final WatchlistDataRepository watchlistDataRepository;

    public WatchlistDataService(WatchlistDataRepository watchlistDataRepository) {
        this.watchlistDataRepository = watchlistDataRepository;
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
    }

    public void removeWatchlistItem(UUID accountId, String symbol, String assetType) {
        logger.info("Removing item from watchlist: accountId={}, symbol={}, assetType={}", accountId, symbol, assetType);

        WatchlistData watchlistData = watchlistDataRepository.findByAccountIdAndSymbolAndAssetType(accountId, symbol, assetType);
        if (watchlistData != null) {
            watchlistDataRepository.delete(watchlistData); // Perform a hard delete
        } else {
            throw new IllegalArgumentException("Item not found in watchlist.");
        }
    }
}