package com.fintrack.service.finance;

import com.fintrack.model.finance.HoldingsMonthly;
import com.fintrack.repository.finance.HoldingsMonthlyRepository;

import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
public class HoldingsMonthlyService {

    private final HoldingsMonthlyRepository holdingsMonthlyRepository;

    public HoldingsMonthlyService(HoldingsMonthlyRepository holdingsMonthlyRepository) {
        this.holdingsMonthlyRepository = holdingsMonthlyRepository;
    }

    public List<HoldingsMonthly> getMonthlyHoldingsByAccount(UUID accountId) {
        return holdingsMonthlyRepository.findByAccountId(accountId);
    }

    public List<HoldingsMonthly> getMonthlyHoldingsByAccountAfterDate(UUID accountId, LocalDate date) {
        return holdingsMonthlyRepository.findByAccountIdAndDateAfter(accountId, date);
    }
}