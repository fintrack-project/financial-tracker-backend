package com.fintrack.service.market;

import com.fintrack.constants.finance.AssetType;
import com.fintrack.repository.finance.HoldingsMonthlyRepository;
import com.fintrack.repository.market.MarketDataRepository;
import com.fintrack.util.KafkaProducerService;

import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Service for handling stock-specific market data operations.
 */
@Service
public class StockMarketDataService extends AssetMarketDataService {

    public StockMarketDataService(
            MarketDataRepository marketDataRepository,
            HoldingsMonthlyRepository holdingsMonthlyRepository,
            KafkaProducerService kafkaProducerService) {
        super(marketDataRepository, holdingsMonthlyRepository, kafkaProducerService);
    }

    @Override
    public AssetType getAssetType() {
        return AssetType.STOCK;
    }

    @Override
    public List<Map<String, String>> processSymbols(List<String> symbols) {
        logger.info("Processing {} stock symbols", symbols.size());
        
        // For stock assets, we can simply use the symbols as provided
        List<Map<String, String>> assets = new ArrayList<>();
        for (String symbol : symbols) {
            Map<String, String> asset = new HashMap<>();
            asset.put("symbol", symbol);
            asset.put("asset_type", getAssetType().getAssetTypeName());
            assets.add(asset);
        }
        
        logger.debug("Processed stock symbols: {}", assets);
        return assets;
    }
} 