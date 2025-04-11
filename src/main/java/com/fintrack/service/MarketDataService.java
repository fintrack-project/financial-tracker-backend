package com.fintrack.service;

import com.fintrack.model.MarketData;
import com.fintrack.repository.MarketDataRepository;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class MarketDataService {

    private final MarketDataRepository marketDataRepository;

    public MarketDataService(MarketDataRepository marketDataRepository) {
        this.marketDataRepository = marketDataRepository;
    }

    public List<MarketData> fetchMarketDataByAssetNames(List<String> assetNames) {
        // Fetch market data for the specified asset names
        return marketDataRepository.findMarketDataByAssetNames(assetNames);
    }
}