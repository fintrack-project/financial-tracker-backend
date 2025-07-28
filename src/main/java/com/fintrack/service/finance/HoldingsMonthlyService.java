package com.fintrack.service.finance;

import com.fintrack.model.finance.Transaction;
import com.fintrack.repository.finance.TransactionRepository;
import com.fintrack.model.finance.HoldingsMonthly;
import com.fintrack.repository.finance.HoldingsMonthlyRepository;

import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;

@Service
public class HoldingsMonthlyService {

    private static final Logger logger = LoggerFactory.getLogger(HoldingsMonthlyService.class);
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
     * Force recalculation of monthly holdings for the given account.
     * This ensures monthly holdings match current holdings.
     */
    public void forceRecalculateMonthlyHoldings(UUID accountId) {
        logger.info("Force recalculating monthly holdings for account: {}", accountId);
        updateMonthlyHoldingsForAccount(accountId);
    }

    /**
     * Recalculate and update monthly holdings for the given account based on all transactions.
     */
    public void updateMonthlyHoldingsForAccount(UUID accountId) {
        // Fetch all transactions for the account, ordered by date ascending
        List<Transaction> transactions = transactionRepository.findByAccountIdOrderByDateDesc(accountId);
        transactions.sort(Comparator.comparing(Transaction::getDate)); // Ensure ascending order
        
        logger.info("Updating monthly holdings for account: {}, found {} transactions", accountId, transactions.size());
        
        // Debug: Log some transactions to verify they're not soft-deleted
        for (Transaction tx : transactions) {
            logger.debug("Transaction: Date={}, Asset={}, Credit={}, Debit={}, DeletedAt={}", 
                        tx.getDate(), tx.getAssetName(), tx.getCredit(), tx.getDebit(), tx.getDeletedAt());
        }

        // Map: assetName -> running total
        Map<String, BigDecimal> runningTotals = new HashMap<>();
        // Map: YearMonth -> assetName -> HoldingsMonthly
        Map<YearMonth, Map<String, HoldingsMonthly>> monthAssetToHoldings = new HashMap<>();

        // Get all months present in the transactions
        Set<YearMonth> allMonths = new TreeSet<>();
        for (Transaction tx : transactions) {
            allMonths.add(YearMonth.from(tx.getDate()));
        }

        // For each month in order, process all transactions up to and including that month
        for (YearMonth ym : allMonths) {
            LocalDate endOfMonth = ym.atEndOfMonth();
            // For each asset, calculate cumulative sum up to endOfMonth
            for (Transaction tx : transactions) {
                if (tx.getDate().isAfter(endOfMonth)) continue;
                String assetName = tx.getAssetName();
                String symbol = tx.getSymbol();
                String unit = tx.getUnit();
                BigDecimal netQty = tx.getCredit().subtract(tx.getDebit());
                runningTotals.put(assetName, runningTotals.getOrDefault(assetName, BigDecimal.ZERO).add(netQty));
                // Prepare HoldingsMonthly for this month/asset
                monthAssetToHoldings.putIfAbsent(ym, new HashMap<>());
                Map<String, HoldingsMonthly> assetMap = monthAssetToHoldings.get(ym);
                if (!assetMap.containsKey(assetName)) {
                    HoldingsMonthly holding = new HoldingsMonthly();
                    holding.setAccountId(accountId);
                    holding.setAssetName(assetName);
                    holding.setSymbol(symbol);
                    holding.setUnit(unit);
                    holding.setAssetType(tx.getAssetType());
                    holding.setDate(endOfMonth);
                    holding.setTotalBalance(BigDecimal.ZERO);
                    assetMap.put(assetName, holding);
                }
                HoldingsMonthly holding = assetMap.get(assetName);
                holding.setTotalBalance(runningTotals.get(assetName));
            }
        }
        // Remove existing monthly holdings for the account
        int deletedCount = holdingsMonthlyRepository.deleteByAccountId(accountId);
        logger.info("Deleted {} existing monthly holdings for account: {}", deletedCount, accountId);
        
        // Debug: Log the monthly holdings being calculated
        logger.info("Calculated monthly holdings:");
        for (Map<String, HoldingsMonthly> assetMap : monthAssetToHoldings.values()) {
            for (HoldingsMonthly holding : assetMap.values()) {
                logger.info("  {} - {}: {}", holding.getDate(), holding.getAssetName(), holding.getTotalBalance());
            }
        }
        
        // Write new monthly holdings
        for (Map<String, HoldingsMonthly> assetMap : monthAssetToHoldings.values()) {
            for (HoldingsMonthly holding : assetMap.values()) {
                holdingsMonthlyRepository.save(holding);
            }
        }
    }
}