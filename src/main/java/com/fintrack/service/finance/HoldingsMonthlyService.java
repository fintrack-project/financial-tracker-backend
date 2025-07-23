package com.fintrack.service.finance;

import com.fintrack.model.finance.Transaction;
import com.fintrack.repository.finance.TransactionRepository;
import com.fintrack.model.finance.HoldingsMonthly;
import com.fintrack.repository.finance.HoldingsMonthlyRepository;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;

@Service
public class HoldingsMonthlyService {

    private final HoldingsMonthlyRepository holdingsMonthlyRepository;
    private final TransactionRepository transactionRepository;

    public HoldingsMonthlyService(HoldingsMonthlyRepository holdingsMonthlyRepository, TransactionRepository transactionRepository) {
        this.holdingsMonthlyRepository = holdingsMonthlyRepository;
        this.transactionRepository = transactionRepository;
    }

    public List<HoldingsMonthly> getMonthlyHoldingsByAccount(UUID accountId) {
        return holdingsMonthlyRepository.findByAccountId(accountId);
    }

    public List<HoldingsMonthly> getMonthlyHoldingsByAccountAfterDate(UUID accountId, LocalDate date) {
        return holdingsMonthlyRepository.findByAccountIdAndDateAfter(accountId, date);
    }

    /**
     * Recalculate and update monthly holdings for the given account based on all transactions.
     */
    public void updateMonthlyHoldingsForAccount(UUID accountId) {
        // Fetch all transactions for the account
        List<Transaction> transactions = transactionRepository.findByAccountIdOrderByDateDesc(accountId);
        // Group transactions by YearMonth and assetName
        Map<YearMonth, Map<String, HoldingsMonthly>> monthAssetToHoldings = new HashMap<>();
        for (Transaction tx : transactions) {
            YearMonth ym = YearMonth.from(tx.getDate());
            String assetName = tx.getAssetName();
            String symbol = tx.getSymbol();
            String unit = tx.getUnit();
            BigDecimal netQty = tx.getCredit().subtract(tx.getDebit());
            LocalDate date = ym.atEndOfMonth();
            monthAssetToHoldings.putIfAbsent(ym, new HashMap<>());
            Map<String, HoldingsMonthly> assetMap = monthAssetToHoldings.get(ym);
            if (!assetMap.containsKey(assetName)) {
                HoldingsMonthly holding = new HoldingsMonthly();
                holding.setAccountId(accountId);
                holding.setAssetName(assetName);
                holding.setSymbol(symbol);
                holding.setUnit(unit);
                holding.setAssetType(tx.getAssetType());
                holding.setDate(date);
                holding.setTotalBalance(BigDecimal.ZERO);
                assetMap.put(assetName, holding);
            }
            HoldingsMonthly holding = assetMap.get(assetName);
            holding.setTotalBalance(holding.getTotalBalance().add(netQty));
        }
        // Remove existing monthly holdings for the account
        holdingsMonthlyRepository.deleteByAccountId(accountId);
        // Write new monthly holdings
        for (Map<String, HoldingsMonthly> assetMap : monthAssetToHoldings.values()) {
            for (HoldingsMonthly holding : assetMap.values()) {
                holdingsMonthlyRepository.save(holding);
            }
        }
    }
}