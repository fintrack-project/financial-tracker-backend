package com.fintrack.controller;

import com.fintrack.model.ForexData;
import com.fintrack.service.ForexDataService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/forex-data")
public class ForexDataController {

    private static final Logger logger = LoggerFactory.getLogger(ForexDataController.class);

    private final ForexDataService forexDataService;

    public ForexDataController(ForexDataService forexDataService) {
        this.forexDataService = forexDataService;
    }

    @PostMapping("/fetch")
    public ResponseEntity<List<ForexData>> fetchForexData(@RequestBody List<String> symbols) {
        logger.info("Received request to fetch forex data for symbols: " + symbols);
        List<ForexData> forexData = forexDataService.fetchForexData(symbols);
        return ResponseEntity.ok(forexData);
    }
}