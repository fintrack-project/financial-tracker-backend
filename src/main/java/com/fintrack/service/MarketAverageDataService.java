package com.fintrack.service;

import com.fintrack.model.MarketAverageData;
import com.fintrack.repository.MarketAverageDataRepository;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

import java.util.*;

import org.springframework.stereotype.Service;

@Service
public class MarketAverageDataService {

    private MarketAverageDataRepository marketAverageDataRepository;

    public MarketAverageDataService(MarketAverageDataRepository marketAverageDataRepository) {
        this.marketAverageDataRepository = marketAverageDataRepository;
    }

    public Map<String, Object> getMostRecentMarketData(List<String> symbols) {
        Map<String, Object> result = new HashMap<>();
        for (String encodedSymbol : symbols) {
            String symbol = URLDecoder.decode(encodedSymbol, StandardCharsets.UTF_8);
            MarketAverageData mostRecentData = marketAverageDataRepository.findTopBySymbolOrderByTimeDesc(symbol);
            if (mostRecentData != null) {
                result.put(symbol, Map.of(
                    "price", mostRecentData.getPrice(),
                    "percent_change", mostRecentData.getPercentChange()
                ));
            }
        }
        return result;
    }
}
