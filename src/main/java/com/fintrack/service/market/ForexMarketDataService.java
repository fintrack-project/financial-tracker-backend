package com.fintrack.service.market;

import com.fintrack.constants.KafkaTopics;
import com.fintrack.constants.finance.AssetType;
import com.fintrack.model.finance.AccountCurrency;
import com.fintrack.repository.finance.AccountCurrenciesRepository;
import com.fintrack.repository.finance.HoldingsMonthlyRepository;
import com.fintrack.repository.market.MarketDataRepository;
import com.fintrack.service.market.base.AssetMarketDataProviderBase;
import com.fintrack.util.KafkaProducerService;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Service for handling forex-specific market data operations.
 */
@Service
public class ForexMarketDataService extends AssetMarketDataProviderBase {

    private final AccountCurrenciesRepository accountCurrenciesRepository;
    private static final String DEFAULT_BASE_CURRENCY = "USD";

    public ForexMarketDataService(
            MarketDataRepository marketDataRepository,
            HoldingsMonthlyRepository holdingsMonthlyRepository,
            KafkaProducerService kafkaProducerService,
            AccountCurrenciesRepository accountCurrenciesRepository) {
        super(marketDataRepository, holdingsMonthlyRepository, kafkaProducerService);
        this.accountCurrenciesRepository = accountCurrenciesRepository;
    }

    @Override
    public AssetType getAssetType() {
        return AssetType.FOREX;
    }

    @Override
    public List<Map<String, String>> processSymbols(List<String> symbols) {
        logger.info("Processing {} forex symbols", symbols.size());
        List<Map<String, String>> assets = new ArrayList<>();
        
        for (String symbol : symbols) {
            // Check if the symbol already contains a currency pair format
            if (symbol.contains("/")) {
                Map<String, String> asset = new HashMap<>();
                asset.put("symbol", symbol);
                asset.put("asset_type", getAssetType().getAssetTypeName());
                assets.add(asset);
            } else {
                // If not, format it as a currency pair with base currency
                // We'll use USD as default base currency if not specified
                if (!symbol.equals(DEFAULT_BASE_CURRENCY)) {
                    String forexPair = symbol + "/" + DEFAULT_BASE_CURRENCY;
                    Map<String, String> asset = new HashMap<>();
                    asset.put("symbol", forexPair);
                    asset.put("asset_type", getAssetType().getAssetTypeName());
                    assets.add(asset);
                    
                    logger.debug("Formatted single currency {} as forex pair: {}", symbol, forexPair);
                } else {
                    logger.debug("Skipping base currency {} as it doesn't need conversion", symbol);
                }
            }
        }
        
        logger.debug("Processed forex symbols: {}", assets);
        return assets;
    }
    
    /**
     * Process symbols for a specific account, using the account's preferred base currency.
     * 
     * @param accountId The account ID
     * @param symbols List of symbol strings
     * @return List of processed symbol-asset type pairs ready for request
     */
    public List<Map<String, String>> processSymbolsForAccount(UUID accountId, List<String> symbols) {
        logger.info("Processing {} forex symbols for account {}", symbols.size(), accountId);
        List<Map<String, String>> assets = new ArrayList<>();
        
        // Get the account's default currency
        String baseCurrency = getAccountBaseCurrency(accountId);
        
        for (String symbol : symbols) {
            // Check if the symbol already contains a currency pair format
            if (symbol.contains("/")) {
                Map<String, String> asset = new HashMap<>();
                asset.put("symbol", symbol);
                asset.put("asset_type", getAssetType().getAssetTypeName());
                assets.add(asset);
            } else {
                // Skip if the currency is the same as the base currency
                if (!symbol.equals(baseCurrency)) {
                    String forexPair = baseCurrency + "/" + symbol;
                    Map<String, String> asset = new HashMap<>();
                    asset.put("symbol", forexPair);
                    asset.put("asset_type", getAssetType().getAssetTypeName());
                    assets.add(asset);
                    
                    logger.debug("Formatted single currency {} with base currency {} as forex pair: {}", 
                                symbol, baseCurrency, forexPair);
                } else {
                    logger.debug("Skipping base currency {} as it doesn't need conversion", symbol);
                }
            }
        }
        
        logger.debug("Processed forex symbols for account {}: {}", accountId, assets);
        return assets;
    }
    
    /**
     * Get the account's base currency.
     * 
     * @param accountId The account ID
     * @return The account's base currency, defaulting to USD if not found
     */
    private String getAccountBaseCurrency(UUID accountId) {
        try {
            List<AccountCurrency> currencies = accountCurrenciesRepository.findByAccountId(accountId);
            for (AccountCurrency currency : currencies) {
                if (currency.isDefault()) {
                    return currency.getCurrency();
                }
            }
        } catch (Exception e) {
            logger.error("Error getting account base currency: {}", e.getMessage());
        }
        
        // Default to USD if no default currency found
        return DEFAULT_BASE_CURRENCY;
    }

    @Override
    @KafkaListener(topics = "#{T(com.fintrack.constants.KafkaTopics).MARKET_DATA_UPDATE_COMPLETE.getTopicName()}", 
                  groupId = "forex-market-data-group", 
                  properties = "#{{'spring.json.value.default.type=java.util.Map'}}")
    public void onMarketDataUpdateComplete(String message) {
        logger.info("Received market data update complete message for FOREX: {}", message);
        
        try {
            Map<String, Object> payload = objectMapper.readValue(message, Map.class);
            
            // Process only if it contains forex assets
            if (payload.containsKey("assets")) {
                List<Map<String, Object>> assets = (List<Map<String, Object>>) payload.get("assets");
                assets.stream()
                    .filter(asset -> getAssetType().getAssetTypeName().equals(asset.get("asset_type")))
                    .forEach(asset -> logger.debug("Processed FOREX update for: {}", asset.get("symbol")));
            }
        } catch (Exception e) {
            logger.error("Failed to process market data update complete message: {}", e.getMessage());
        }
    }
} 