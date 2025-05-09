package com.fintrack.service.market.interfaces;

import com.fintrack.model.market.WatchlistData;

import java.util.List;
import java.util.UUID;

/**
 * Interface defining operations specific to watchlist data management.
 */
public interface WatchlistDataOperations {
    
    /**
     * Fetches watchlist data for a specific account and asset types.
     * 
     * @param accountId The account ID
     * @param assetTypes List of asset types to fetch
     * @return List of watchlist data items
     */
    List<WatchlistData> fetchWatchlistData(UUID accountId, List<String> assetTypes);
    
    /**
     * Adds an item to the watchlist.
     * 
     * @param accountId The account ID
     * @param symbol The symbol to add
     * @param assetType The asset type of the symbol
     */
    void addWatchlistItem(UUID accountId, String symbol, String assetType);
    
    /**
     * Removes an item from the watchlist.
     * 
     * @param accountId The account ID
     * @param symbol The symbol to remove
     * @param assetType The asset type of the symbol
     * @throws IllegalArgumentException if the item is not found in the watchlist
     */
    void removeWatchlistItem(UUID accountId, String symbol, String assetType);
    
    /**
     * Requests market data updates for multiple watchlist items.
     * 
     * @param accountId The account ID
     * @param watchlistItems List of watchlist data items
     */
    void requestMarketDataUpdates(UUID accountId, List<WatchlistData> watchlistItems);
} 