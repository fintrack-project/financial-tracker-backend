package com.fintrack.controller;

import com.fintrack.model.MarketData;
import com.fintrack.service.MarketDataService;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.util.List;

@Controller
public class MarketDataWebSocketController {

    private final SimpMessagingTemplate messagingTemplate;
    private final MarketDataService marketDataService;

    public MarketDataWebSocketController(SimpMessagingTemplate messagingTemplate, MarketDataService marketDataService) {
        this.messagingTemplate = messagingTemplate;
        this.marketDataService = marketDataService;
    }

    public void sendUpdatedMarketData(List<String> assetNames) {
        // Fetch the updated market data
        List<MarketData> marketData = marketDataService.fetchMarketDataByAssetNames(assetNames);

        // Send the data to the WebSocket topic
        messagingTemplate.convertAndSend("/topic/market-data", marketData);
    }
}