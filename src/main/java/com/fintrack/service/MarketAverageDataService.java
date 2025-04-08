package com.fintrack.service;

import com.fintrack.model.MarketAverageData;
import com.fintrack.repository.MarketAverageDataRepository;
import java.util.*;

import org.springframework.beans.factory.annotation.Autowired;

public class MarketAverageDataService {
    @Autowired
    private MarketAverageDataRepository marketAverageDataRepository;

    public Map<String, Object> getMostRecentMarketData(List<String> symbols) {
        Map<String, Object> result = new HashMap<>();
        for (String symbol : symbols) {
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
