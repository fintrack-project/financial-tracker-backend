package com.fintrack.service.market;

import com.fintrack.constants.finance.AssetType;
import com.fintrack.repository.finance.HoldingsMonthlyRepository;
import com.fintrack.repository.market.MarketDataRepository;
import com.fintrack.util.KafkaProducerService;

import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Service for handling cryptocurrency-specific market data operations.
 */
@Service
public class CryptoMarketDataService extends AssetMarketDataService {

    private static final String DEFAULT_QUOTE_CURRENCY = "USD";

    public CryptoMarketDataService(
            MarketDataRepository marketDataRepository,
            HoldingsMonthlyRepository holdingsMonthlyRepository,
            KafkaProducerService kafkaProducerService) {
        super(marketDataRepository, holdingsMonthlyRepository, kafkaProducerService);
    }

    @Override
    public AssetType getAssetType() {
        return AssetType.CRYPTO;
    }

    @Override
    public List<Map<String, String>> processSymbols(List<String> symbols) {
        logger.info("Processing {} crypto symbols", symbols.size());
        List<Map<String, String>> assets = new ArrayList<>();
        
        for (String symbol : symbols) {
            // For crypto assets, ensure they are quoted in USD
            // Check if the symbol already contains a quote currency
            if (symbol.contains("/")) {
                Map<String, String> asset = new HashMap<>();
                asset.put("symbol", symbol);
                asset.put("asset_type", getAssetType().getAssetTypeName());
                assets.add(asset);
            } else {
                // Append /USD to the symbol if it's not there already
                String cryptoPair = symbol + "/" + DEFAULT_QUOTE_CURRENCY;
                Map<String, String> asset = new HashMap<>();
                asset.put("symbol", cryptoPair);
                asset.put("asset_type", getAssetType().getAssetTypeName());
                assets.add(asset);
                
                logger.debug("Formatted crypto symbol {} as pair: {}", symbol, cryptoPair);
            }
        }
        
        logger.debug("Processed crypto symbols: {}", assets);
        return assets;
    }
} 