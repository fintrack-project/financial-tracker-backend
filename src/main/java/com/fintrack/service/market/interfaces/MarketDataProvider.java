package com.fintrack.service.market.interfaces;

import java.util.List;
import java.util.UUID;

/**
 * Interface defining common operations for market data providers.
 * This interface is implemented by services that provide market data,
 * including stocks, forex, crypto, commodities, and market indices.
 */
public interface MarketDataProvider {
    
    /**
     * Updates market data for the specified symbols.
     * This method triggers data requests to external systems or services.
     * 
     * @param symbols List of symbols to update
     */
    void requestMarketDataUpdate(List<String> symbols);
    
    /**
     * Updates market data for a specific symbol.
     * This is a convenience method that wraps the list-based method.
     * 
     * @param symbol The symbol to update
     */
    default void requestMarketDataUpdate(String symbol) {
        requestMarketDataUpdate(List.of(symbol));
    }
    
    /**
     * Updates market data for the specified symbols for a specific account.
     * This allows for account-specific market data processing.
     * 
     * @param accountId The account ID requesting the update
     * @param symbols List of symbols to update
     */
    void requestMarketDataUpdate(UUID accountId, List<String> symbols);
    
    /**
     * Updates market data for a specific symbol for a specific account.
     * This is a convenience method that wraps the list-based method.
     * 
     * @param accountId The account ID requesting the update
     * @param symbol The symbol to update
     */
    default void requestMarketDataUpdate(UUID accountId, String symbol) {
        requestMarketDataUpdate(accountId, List.of(symbol));
    }
} 