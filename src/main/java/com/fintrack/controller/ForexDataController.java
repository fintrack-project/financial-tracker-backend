package com.fintrack.controller;

import com.fintrack.model.ForexData;
import com.fintrack.service.ForexDataService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/forex-data")
public class ForexDataController {

    private static final Logger logger = LoggerFactory.getLogger(ForexDataController.class);

    private final ForexDataService forexDataService;

    public ForexDataController(ForexDataService forexDataService) {
        this.forexDataService = forexDataService;
    }

    @GetMapping("/fetch")
    public ResponseEntity<List<ForexData>> fetchForexData(
            @RequestParam UUID accountId,
            @RequestParam List<String> symbols) {
        logger.info("Received request to fetch forex data for accountId: " + accountId + " and symbols: " + symbols);

        // Decode symbols
        List<String> decodedSymbols = symbols.stream()
                .map(symbol -> URLDecoder.decode(symbol, StandardCharsets.UTF_8))
                .collect(Collectors.toList());

        logger.info("Decoded symbols: " + decodedSymbols);

        List<ForexData> forexData = forexDataService.fetchForexData(decodedSymbols);
        return ResponseEntity.ok(forexData);
    }
}