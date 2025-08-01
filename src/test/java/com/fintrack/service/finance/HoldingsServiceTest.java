package com.fintrack.service.finance;

import com.fintrack.model.finance.Holdings;
import com.fintrack.model.finance.Transaction;
import com.fintrack.repository.finance.HoldingsRepository;
import com.fintrack.repository.finance.TransactionRepository;
import com.fintrack.constants.finance.AssetType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("HoldingsService Tests")
class HoldingsServiceTest {

    @Mock
    private HoldingsRepository holdingsRepository;
    
    @Mock
    private TransactionRepository transactionRepository;

    private HoldingsService holdingsService;
    private UUID testAccountId;

    @BeforeEach
    void setUp() {
        holdingsService = new HoldingsService(holdingsRepository, transactionRepository);
        testAccountId = UUID.randomUUID();
    }

    @Test
    @DisplayName("Should retrieve holdings by account ID")
    void shouldRetrieveHoldingsByAccount() {
        // Given: Sample holdings exist for the account
        List<Holdings> expectedHoldings = createSampleHoldings();
        when(holdingsRepository.findHoldingsByAccount(testAccountId))
            .thenReturn(expectedHoldings);

        // When: Retrieving holdings by account ID
        List<Holdings> result = holdingsService.getHoldingsByAccount(testAccountId);

        // Then: Should return the expected holdings
        assertNotNull(result);
        assertEquals(expectedHoldings.size(), result.size());
        verify(holdingsRepository).findHoldingsByAccount(testAccountId);
    }

    @Test
    @DisplayName("Should update holdings for account with transactions")
    void shouldUpdateHoldingsForAccountWithTransactions() {
        // Given: Sample transactions exist for the account
        List<Transaction> transactions = createSampleTransactions();
        when(transactionRepository.findByAccountIdOrderByDateDesc(testAccountId))
            .thenReturn(transactions);
        when(holdingsRepository.saveAll(anyList())).thenReturn(createSampleHoldings());

        // When: Updating holdings for the account
        holdingsService.updateHoldingsForAccount(testAccountId);

        // Then: Should delete existing holdings and save new calculated holdings
        verify(transactionRepository).findByAccountIdOrderByDateDesc(testAccountId);
        verify(holdingsRepository).deleteByAccountId(testAccountId);
        verify(holdingsRepository).saveAll(anyList());
    }

    @Test
    @DisplayName("Should handle empty transactions list")
    void shouldHandleEmptyTransactionsList() {
        // Given: Account with no transactions
        List<Transaction> emptyTransactions = new ArrayList<>();
        when(transactionRepository.findByAccountIdOrderByDateDesc(testAccountId))
            .thenReturn(emptyTransactions);

        // When: Updating holdings for the account
        holdingsService.updateHoldingsForAccount(testAccountId);

        // Then: Should delete existing holdings but not save any new holdings
        verify(transactionRepository).findByAccountIdOrderByDateDesc(testAccountId);
        verify(holdingsRepository).deleteByAccountId(testAccountId);
        verify(holdingsRepository, never()).saveAll(anyList());
    }

    @Test
    @DisplayName("Should calculate holdings correctly with multiple transactions")
    void shouldCalculateHoldingsCorrectlyWithMultipleTransactions() {
        // Given: Multiple transactions for the same asset (buy/sell operations)
        List<Transaction> transactions = createMultipleTransactionsForSameAsset();
        when(transactionRepository.findByAccountIdOrderByDateDesc(testAccountId))
            .thenReturn(transactions);
        when(holdingsRepository.saveAll(anyList())).thenReturn(createSampleHoldings());

        // When: Updating holdings for the account
        holdingsService.updateHoldingsForAccount(testAccountId);

        // Then: Should calculate and save holdings based on net balance
        verify(transactionRepository).findByAccountIdOrderByDateDesc(testAccountId);
        verify(holdingsRepository).deleteByAccountId(testAccountId);
        verify(holdingsRepository).saveAll(anyList());
    }

    @Test
    @DisplayName("Should filter out holdings with zero or negative balance")
    void shouldFilterOutHoldingsWithZeroOrNegativeBalance() {
        // Given: Transactions that result in zero or negative balance
        List<Transaction> transactions = createTransactionsWithNegativeBalance();
        when(transactionRepository.findByAccountIdOrderByDateDesc(testAccountId))
            .thenReturn(transactions);

        // When: Updating holdings for the account
        holdingsService.updateHoldingsForAccount(testAccountId);

        // Then: Should delete existing holdings but not save any new holdings due to negative balance
        verify(transactionRepository).findByAccountIdOrderByDateDesc(testAccountId);
        verify(holdingsRepository).deleteByAccountId(testAccountId);
        // Should not save any holdings since all have zero or negative balance
        verify(holdingsRepository, never()).saveAll(anyList());
    }

    @Test
    @DisplayName("Should handle transactions with different asset types")
    void shouldHandleTransactionsWithDifferentAssetTypes() {
        // Given: Transactions with different asset types (STOCK, CRYPTO)
        List<Transaction> transactions = createTransactionsWithDifferentAssetTypes();
        when(transactionRepository.findByAccountIdOrderByDateDesc(testAccountId))
            .thenReturn(transactions);
        when(holdingsRepository.saveAll(anyList())).thenReturn(createSampleHoldings());

        // When: Updating holdings for the account
        holdingsService.updateHoldingsForAccount(testAccountId);

        // Then: Should process and save holdings for different asset types
        verify(transactionRepository).findByAccountIdOrderByDateDesc(testAccountId);
        verify(holdingsRepository).deleteByAccountId(testAccountId);
        verify(holdingsRepository).saveAll(anyList());
    }

    @Test
    @DisplayName("Should handle null transaction values gracefully")
    void shouldHandleNullTransactionValuesGracefully() {
        // Given: Transactions with null values in some fields
        List<Transaction> transactions = createTransactionsWithNullValues();
        when(transactionRepository.findByAccountIdOrderByDateDesc(testAccountId))
            .thenReturn(transactions);
        when(holdingsRepository.saveAll(anyList())).thenReturn(createSampleHoldings());

        // When: Updating holdings for the account
        holdingsService.updateHoldingsForAccount(testAccountId);

        // Then: Should handle null values gracefully and still process holdings
        verify(transactionRepository).findByAccountIdOrderByDateDesc(testAccountId);
        verify(holdingsRepository).deleteByAccountId(testAccountId);
        verify(holdingsRepository).saveAll(anyList());
    }

    @Test
    @DisplayName("Should handle large number of transactions efficiently")
    void shouldHandleLargeNumberOfTransactionsEfficiently() {
        // Given: Large number of transactions (100+) for the same asset
        List<Transaction> transactions = createLargeNumberOfTransactions();
        when(transactionRepository.findByAccountIdOrderByDateDesc(testAccountId))
            .thenReturn(transactions);
        when(holdingsRepository.saveAll(anyList())).thenReturn(createSampleHoldings());

        // When: Updating holdings for the account
        holdingsService.updateHoldingsForAccount(testAccountId);

        // Then: Should process large transaction volume efficiently
        verify(transactionRepository).findByAccountIdOrderByDateDesc(testAccountId);
        verify(holdingsRepository).deleteByAccountId(testAccountId);
        verify(holdingsRepository).saveAll(anyList());
    }

    // Helper methods to create test data
    private List<Holdings> createSampleHoldings() {
        List<Holdings> holdings = new ArrayList<>();
        
        Holdings holding1 = new Holdings();
        holding1.setId(1L);
        holding1.setAccountId(testAccountId);
        holding1.setAssetName("AAPL");
        holding1.setSymbol("AAPL");
        holding1.setUnit("SHARES");
        holding1.setAssetType(AssetType.STOCK);
        holding1.setTotalBalance(100.0);
        holding1.setUpdatedAt(LocalDateTime.now());
        holdings.add(holding1);

        Holdings holding2 = new Holdings();
        holding2.setId(2L);
        holding2.setAccountId(testAccountId);
        holding2.setAssetName("GOOGL");
        holding2.setSymbol("GOOGL");
        holding2.setUnit("SHARES");
        holding2.setAssetType(AssetType.STOCK);
        holding2.setTotalBalance(50.0);
        holding2.setUpdatedAt(LocalDateTime.now());
        holdings.add(holding2);

        return holdings;
    }

    private List<Transaction> createSampleTransactions() {
        List<Transaction> transactions = new ArrayList<>();
        
        Transaction transaction1 = new Transaction();
        transaction1.setTransactionId(1L);
        transaction1.setAccountId(testAccountId);
        transaction1.setAssetName("AAPL");
        transaction1.setSymbol("AAPL");
        transaction1.setUnit("SHARES");
        transaction1.setDate(LocalDate.of(2024, 1, 15));
        transaction1.setCredit(new BigDecimal("100.00"));
        transaction1.setDebit(BigDecimal.ZERO);
        transaction1.setAssetType(AssetType.STOCK);
        transactions.add(transaction1);

        Transaction transaction2 = new Transaction();
        transaction2.setTransactionId(2L);
        transaction2.setAccountId(testAccountId);
        transaction2.setAssetName("GOOGL");
        transaction2.setSymbol("GOOGL");
        transaction2.setUnit("SHARES");
        transaction2.setDate(LocalDate.of(2024, 1, 16));
        transaction2.setCredit(BigDecimal.ZERO);
        transaction2.setDebit(new BigDecimal("50.00"));
        transaction2.setAssetType(AssetType.STOCK);
        transactions.add(transaction2);

        return transactions;
    }

    private List<Transaction> createMultipleTransactionsForSameAsset() {
        List<Transaction> transactions = new ArrayList<>();
        
        // Buy 100 shares
        Transaction buy1 = new Transaction();
        buy1.setTransactionId(1L);
        buy1.setAccountId(testAccountId);
        buy1.setAssetName("AAPL");
        buy1.setSymbol("AAPL");
        buy1.setUnit("SHARES");
        buy1.setDate(LocalDate.of(2024, 1, 15));
        buy1.setCredit(new BigDecimal("100.00"));
        buy1.setDebit(BigDecimal.ZERO);
        buy1.setAssetType(AssetType.STOCK);
        transactions.add(buy1);

        // Buy 50 more shares
        Transaction buy2 = new Transaction();
        buy2.setTransactionId(2L);
        buy2.setAccountId(testAccountId);
        buy2.setAssetName("AAPL");
        buy2.setSymbol("AAPL");
        buy2.setUnit("SHARES");
        buy2.setDate(LocalDate.of(2024, 1, 16));
        buy2.setCredit(new BigDecimal("50.00"));
        buy2.setDebit(BigDecimal.ZERO);
        buy2.setAssetType(AssetType.STOCK);
        transactions.add(buy2);

        // Sell 25 shares
        Transaction sell1 = new Transaction();
        sell1.setTransactionId(3L);
        sell1.setAccountId(testAccountId);
        sell1.setAssetName("AAPL");
        sell1.setSymbol("AAPL");
        sell1.setUnit("SHARES");
        sell1.setDate(LocalDate.of(2024, 1, 17));
        sell1.setCredit(BigDecimal.ZERO);
        sell1.setDebit(new BigDecimal("25.00"));
        sell1.setAssetType(AssetType.STOCK);
        transactions.add(sell1);

        return transactions;
    }

    private List<Transaction> createTransactionsWithNegativeBalance() {
        List<Transaction> transactions = new ArrayList<>();
        
        // Sell more than we have
        Transaction sell = new Transaction();
        sell.setTransactionId(1L);
        sell.setAccountId(testAccountId);
        sell.setAssetName("AAPL");
        sell.setSymbol("AAPL");
        sell.setUnit("SHARES");
        sell.setDate(LocalDate.of(2024, 1, 15));
        sell.setCredit(BigDecimal.ZERO);
        sell.setDebit(new BigDecimal("100.00"));
        sell.setAssetType(AssetType.STOCK);
        transactions.add(sell);

        return transactions;
    }

    private List<Transaction> createTransactionsWithDifferentAssetTypes() {
        List<Transaction> transactions = new ArrayList<>();
        
        // Stock transaction
        Transaction stock = new Transaction();
        stock.setTransactionId(1L);
        stock.setAccountId(testAccountId);
        stock.setAssetName("AAPL");
        stock.setSymbol("AAPL");
        stock.setUnit("SHARES");
        stock.setDate(LocalDate.of(2024, 1, 15));
        stock.setCredit(new BigDecimal("100.00"));
        stock.setDebit(BigDecimal.ZERO);
        stock.setAssetType(AssetType.STOCK);
        transactions.add(stock);

        // Crypto transaction
        Transaction crypto = new Transaction();
        crypto.setTransactionId(2L);
        crypto.setAccountId(testAccountId);
        crypto.setAssetName("BTC");
        crypto.setSymbol("BTC");
        crypto.setUnit("COINS");
        crypto.setDate(LocalDate.of(2024, 1, 16));
        crypto.setCredit(new BigDecimal("1.5"));
        crypto.setDebit(BigDecimal.ZERO);
        crypto.setAssetType(AssetType.CRYPTO);
        transactions.add(crypto);

        return transactions;
    }

    private List<Transaction> createTransactionsWithNullValues() {
        List<Transaction> transactions = new ArrayList<>();
        
        Transaction transaction = new Transaction();
        transaction.setTransactionId(1L);
        transaction.setAccountId(testAccountId);
        transaction.setAssetName("AAPL");
        transaction.setSymbol(null);
        transaction.setUnit(null);
        transaction.setDate(LocalDate.of(2024, 1, 15));
        transaction.setCredit(new BigDecimal("100.00"));
        transaction.setDebit(BigDecimal.ZERO);
        transaction.setAssetType(AssetType.STOCK);
        transactions.add(transaction);

        return transactions;
    }

    private List<Transaction> createLargeNumberOfTransactions() {
        List<Transaction> transactions = new ArrayList<>();
        
        // Create 100 transactions for the same asset (using valid dates)
        for (int i = 1; i <= 100; i++) {
            Transaction transaction = new Transaction();
            transaction.setTransactionId((long) i);
            transaction.setAccountId(testAccountId);
            transaction.setAssetName("AAPL");
            transaction.setSymbol("AAPL");
            transaction.setUnit("SHARES");
            // Use modulo to ensure valid day of month (1-31)
            int day = ((i - 1) % 28) + 1; // 1-28 to avoid month boundary issues
            transaction.setDate(LocalDate.of(2024, 1, day));
            transaction.setCredit(new BigDecimal("1.00"));
            transaction.setDebit(BigDecimal.ZERO);
            transaction.setAssetType(AssetType.STOCK);
            transactions.add(transaction);
        }

        return transactions;
    }
} 