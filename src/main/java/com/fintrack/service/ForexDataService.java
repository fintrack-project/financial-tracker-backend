package com.fintrack.service;

import com.fintrack.model.ForexData;
import com.fintrack.repository.ForexDataRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ForexDataService {

    private static final Logger logger = LoggerFactory.getLogger(ForexDataService.class);

    private final ForexDataRepository forexDataRepository;

    public ForexDataService(ForexDataRepository forexDataRepository) {
        this.forexDataRepository = forexDataRepository;
    }

    public List<ForexData> fetchForexData(List<String> symbols) {
        logger.info("Fetching forex data for symbols: " + symbols);
        return forexDataRepository.findBySymbols(symbols);
    }
}