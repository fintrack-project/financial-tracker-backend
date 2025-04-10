package com.fintrack.service;

import org.springframework.stereotype.Service;

import com.fintrack.model.HoldingDto;
import com.fintrack.repository.HoldingsRepository;

import java.util.*;

@Service
public class HoldingsService {
    private HoldingsRepository holdingsRepository;

    public HoldingsService(HoldingsRepository holdingsRepository) {
        this.holdingsRepository = holdingsRepository;
    }

    public List<HoldingDto> getLatestHoldingsByAccount(UUID accountId) {
        return holdingsRepository.findLatestHoldingsByAccount(accountId);
    }
}
