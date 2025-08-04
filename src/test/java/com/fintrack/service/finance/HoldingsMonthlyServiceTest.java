package com.fintrack.service.finance;

import com.fintrack.model.finance.HoldingsMonthly;
import com.fintrack.model.finance.Transaction;
import com.fintrack.repository.finance.HoldingsMonthlyRepository;
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
import java.util.*;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("HoldingsMonthlyService Tests")
class HoldingsMonthlyServiceTest {

    @Mock
    private HoldingsMonthlyRepository holdingsMonthlyRepository;
    
    @Mock
    private TransactionRepository transactionRepository;

    private HoldingsMonthlyService holdingsMonthlyService;
    private UUID testAccountId;

    @BeforeEach
    void setUp() {
        holdingsMonthlyService = new HoldingsMonthlyService(
            holdingsMonthlyRepository,
            transactionRepository
        );
        testAccountId = UUID.randomUUID();
    }

    @Test
    @DisplayName("Should get monthly holdings by account successfully")
    void shouldGetMonthlyHoldingsByAccountSuccessfully() {
        // Given: Monthly holdings for the account
        List<HoldingsMonthly> expectedHoldings = createSampleMonthlyHoldings();
        when(holdingsMonthlyRepository.findByAccountId(testAccountId))
            .thenReturn(expectedHoldings);

        // When: Getting monthly holdings by account
        List<HoldingsMonthly> result = holdingsMonthlyService.getMonthlyHoldingsByAccount(testAccountId);

        // Then: Should return the expected holdings
        assertNotNull(result);
        assertEquals(expectedHoldings.size(), result.size());
        verify(holdingsMonthlyRepository).findByAccountId(testAccountId);
    }

    @Test
    @DisplayName("Should get monthly holdings by account after date successfully")
    void shouldGetMonthlyHoldingsByAccountAfterDateSuccessfully() {
        // Given: Monthly holdings after a specific date
        LocalDate afterDate = LocalDate.of(2024, 1, 15);
        List<HoldingsMonthly> expectedHoldings = createSampleMonthlyHoldings();
        when(holdingsMonthlyRepository.findByAccountIdAndDateAfter(testAccountId, afterDate))
            .thenReturn(expectedHoldings);

        // When: Getting monthly holdings after date
        List<HoldingsMonthly> result = holdingsMonthlyService.getMonthlyHoldingsByAccountAfterDate(testAccountId, afterDate);

        // Then: Should return the expected holdings
        assertNotNull(result);
        assertEquals(expectedHoldings.size(), result.size());
        verify(holdingsMonthlyRepository).findByAccountIdAndDateAfter(testAccountId, afterDate);
    }

    @Test
    @DisplayName("Should force recalculate monthly holdings successfully")
    void shouldForceRecalculateMonthlyHoldingsSuccessfully() {
        // Given: Transactions for recalculation
        List<Transaction> transactions = createSampleTransactions();
        when(transactionRepository.findByAccountIdOrderByDateDesc(testAccountId))
            .thenReturn(transactions);
        when(holdingsMonthlyRepository.deleteByAccountId(testAccountId))
            .thenReturn(5);

        // When: Force recalculating monthly holdings
        holdingsMonthlyService.forceRecalculateMonthlyHoldings(testAccountId);

        // Then: Should delete existing holdings and save new ones
        verify(transactionRepository).findByAccountIdOrderByDateDesc(testAccountId);
        verify(holdingsMonthlyRepository).deleteByAccountId(testAccountId);
        verify(holdingsMonthlyRepository, times(2)).save(any(HoldingsMonthly.class));
    }

    @Test
    @DisplayName("Should update monthly holdings for account with single month successfully")
    void shouldUpdateMonthlyHoldingsForAccountWithSingleMonthSuccessfully() {
        // Given: Transactions in a single month
        List<Transaction> transactions = createSampleTransactionsSingleMonth();
        when(transactionRepository.findByAccountIdOrderByDateDesc(testAccountId))
            .thenReturn(transactions);
        when(holdingsMonthlyRepository.deleteByAccountId(testAccountId))
            .thenReturn(0);

        // When: Updating monthly holdings
        holdingsMonthlyService.updateMonthlyHoldingsForAccount(testAccountId);

        // Then: Should process transactions and save monthly holdings
        verify(transactionRepository).findByAccountIdOrderByDateDesc(testAccountId);
        verify(holdingsMonthlyRepository).deleteByAccountId(testAccountId);
        verify(holdingsMonthlyRepository, times(2)).save(any(HoldingsMonthly.class));
    }

    @Test
    @DisplayName("Should update monthly holdings for account with multiple months successfully")
    void shouldUpdateMonthlyHoldingsForAccountWithMultipleMonthsSuccessfully() {
        // Given: Transactions across multiple months
        List<Transaction> transactions = createSampleTransactionsMultipleMonths();
        when(transactionRepository.findByAccountIdOrderByDateDesc(testAccountId))
            .thenReturn(transactions);
        when(holdingsMonthlyRepository.deleteByAccountId(testAccountId))
            .thenReturn(3);

        // When: Updating monthly holdings
        holdingsMonthlyService.updateMonthlyHoldingsForAccount(testAccountId);

        // Then: Should process transactions and save monthly holdings for each month
        verify(transactionRepository).findByAccountIdOrderByDateDesc(testAccountId);
        verify(holdingsMonthlyRepository).deleteByAccountId(testAccountId);
        verify(holdingsMonthlyRepository, times(3)).save(any(HoldingsMonthly.class));
    }

    @Test
    @DisplayName("Should handle update monthly holdings with no transactions")
    void shouldHandleUpdateMonthlyHoldingsWithNoTransactions() {
        // Given: No transactions for the account
        List<Transaction> transactions = new ArrayList<>();
        when(transactionRepository.findByAccountIdOrderByDateDesc(testAccountId))
            .thenReturn(transactions);
        when(holdingsMonthlyRepository.deleteByAccountId(testAccountId))
            .thenReturn(0);

        // When: Updating monthly holdings with no transactions
        holdingsMonthlyService.updateMonthlyHoldingsForAccount(testAccountId);

        // Then: Should delete existing holdings but not save any new ones
        verify(transactionRepository).findByAccountIdOrderByDateDesc(testAccountId);
        verify(holdingsMonthlyRepository).deleteByAccountId(testAccountId);
        verify(holdingsMonthlyRepository, never()).save(any(HoldingsMonthly.class));
    }

    @Test
    @DisplayName("Should handle update monthly holdings with zero balance transactions")
    void shouldHandleUpdateMonthlyHoldingsWithZeroBalanceTransactions() {
        // Given: Transactions that result in zero balance
        List<Transaction> transactions = createSampleTransactionsWithZeroBalance();
        when(transactionRepository.findByAccountIdOrderByDateDesc(testAccountId))
            .thenReturn(transactions);
        when(holdingsMonthlyRepository.deleteByAccountId(testAccountId))
            .thenReturn(1);

        // When: Updating monthly holdings with zero balance transactions
        holdingsMonthlyService.updateMonthlyHoldingsForAccount(testAccountId);

        // Then: Should process transactions and save monthly holdings with zero balance
        verify(transactionRepository).findByAccountIdOrderByDateDesc(testAccountId);
        verify(holdingsMonthlyRepository).deleteByAccountId(testAccountId);
        verify(holdingsMonthlyRepository, times(1)).save(any(HoldingsMonthly.class));
    }

    @Test
    @DisplayName("Should handle update monthly holdings with negative balance transactions")
    void shouldHandleUpdateMonthlyHoldingsWithNegativeBalanceTransactions() {
        // Given: Transactions that result in negative balance
        List<Transaction> transactions = createSampleTransactionsWithNegativeBalance();
        when(transactionRepository.findByAccountIdOrderByDateDesc(testAccountId))
            .thenReturn(transactions);
        when(holdingsMonthlyRepository.deleteByAccountId(testAccountId))
            .thenReturn(1);

        // When: Updating monthly holdings with negative balance transactions
        holdingsMonthlyService.updateMonthlyHoldingsForAccount(testAccountId);

        // Then: Should process transactions and save monthly holdings with negative balance
        verify(transactionRepository).findByAccountIdOrderByDateDesc(testAccountId);
        verify(holdingsMonthlyRepository).deleteByAccountId(testAccountId);
        verify(holdingsMonthlyRepository, times(1)).save(any(HoldingsMonthly.class));
    }

    @Test
    @DisplayName("Should handle update monthly holdings with large numbers")
    void shouldHandleUpdateMonthlyHoldingsWithLargeNumbers() {
        // Given: Transactions with large numbers
        List<Transaction> transactions = createSampleTransactionsWithLargeNumbers();
        when(transactionRepository.findByAccountIdOrderByDateDesc(testAccountId))
            .thenReturn(transactions);
        when(holdingsMonthlyRepository.deleteByAccountId(testAccountId))
            .thenReturn(1);

        // When: Updating monthly holdings with large numbers
        holdingsMonthlyService.updateMonthlyHoldingsForAccount(testAccountId);

        // Then: Should process transactions and save monthly holdings with large numbers
        verify(transactionRepository).findByAccountIdOrderByDateDesc(testAccountId);
        verify(holdingsMonthlyRepository).deleteByAccountId(testAccountId);
        verify(holdingsMonthlyRepository, times(1)).save(any(HoldingsMonthly.class));
    }

    // Helper methods to create test data
    private List<HoldingsMonthly> createSampleMonthlyHoldings() {
        List<HoldingsMonthly> holdings = new ArrayList<>();
        
        HoldingsMonthly holding1 = new HoldingsMonthly();
        holding1.setAccountId(testAccountId);
        holding1.setAssetName("AAPL");
        holding1.setSymbol("AAPL");
        holding1.setUnit("USD");
        holding1.setAssetType(AssetType.STOCK);
        holding1.setDate(LocalDate.of(2024, 1, 31));
        holding1.setTotalBalance(new BigDecimal("100.00"));
        holdings.add(holding1);

        HoldingsMonthly holding2 = new HoldingsMonthly();
        holding2.setAccountId(testAccountId);
        holding2.setAssetName("GOOGL");
        holding2.setSymbol("GOOGL");
        holding2.setUnit("USD");
        holding2.setAssetType(AssetType.STOCK);
        holding2.setDate(LocalDate.of(2024, 1, 31));
        holding2.setTotalBalance(new BigDecimal("50.00"));
        holdings.add(holding2);

        return holdings;
    }

    private List<Transaction> createSampleTransactions() {
        List<Transaction> transactions = new ArrayList<>();
        
        Transaction tx1 = new Transaction();
        tx1.setAccountId(testAccountId);
        tx1.setAssetName("AAPL");
        tx1.setSymbol("AAPL");
        tx1.setUnit("USD");
        tx1.setAssetType(AssetType.STOCK);
        tx1.setDate(LocalDate.of(2024, 1, 15));
        tx1.setCredit(new BigDecimal("100.00"));
        tx1.setDebit(BigDecimal.ZERO);
        tx1.setDeletedAt(null);
        transactions.add(tx1);

        Transaction tx2 = new Transaction();
        tx2.setAccountId(testAccountId);
        tx2.setAssetName("GOOGL");
        tx2.setSymbol("GOOGL");
        tx2.setUnit("USD");
        tx2.setAssetType(AssetType.STOCK);
        tx2.setDate(LocalDate.of(2024, 1, 16));
        tx2.setCredit(new BigDecimal("50.00"));
        tx2.setDebit(BigDecimal.ZERO);
        tx2.setDeletedAt(null);
        transactions.add(tx2);

        return transactions;
    }

    private List<Transaction> createSampleTransactionsSingleMonth() {
        List<Transaction> transactions = new ArrayList<>();
        
        Transaction tx1 = new Transaction();
        tx1.setAccountId(testAccountId);
        tx1.setAssetName("AAPL");
        tx1.setSymbol("AAPL");
        tx1.setUnit("USD");
        tx1.setAssetType(AssetType.STOCK);
        tx1.setDate(LocalDate.of(2024, 1, 15));
        tx1.setCredit(new BigDecimal("100.00"));
        tx1.setDebit(BigDecimal.ZERO);
        tx1.setDeletedAt(null);
        transactions.add(tx1);

        Transaction tx2 = new Transaction();
        tx2.setAccountId(testAccountId);
        tx2.setAssetName("GOOGL");
        tx2.setSymbol("GOOGL");
        tx2.setUnit("USD");
        tx2.setAssetType(AssetType.STOCK);
        tx2.setDate(LocalDate.of(2024, 1, 16));
        tx2.setCredit(new BigDecimal("50.00"));
        tx2.setDebit(BigDecimal.ZERO);
        tx2.setDeletedAt(null);
        transactions.add(tx2);

        return transactions;
    }

    private List<Transaction> createSampleTransactionsMultipleMonths() {
        List<Transaction> transactions = new ArrayList<>();
        
        // January transactions
        Transaction tx1 = new Transaction();
        tx1.setAccountId(testAccountId);
        tx1.setAssetName("AAPL");
        tx1.setSymbol("AAPL");
        tx1.setUnit("USD");
        tx1.setAssetType(AssetType.STOCK);
        tx1.setDate(LocalDate.of(2024, 1, 15));
        tx1.setCredit(new BigDecimal("100.00"));
        tx1.setDebit(BigDecimal.ZERO);
        tx1.setDeletedAt(null);
        transactions.add(tx1);

        // February transactions
        Transaction tx2 = new Transaction();
        tx2.setAccountId(testAccountId);
        tx2.setAssetName("AAPL");
        tx2.setSymbol("AAPL");
        tx2.setUnit("USD");
        tx2.setAssetType(AssetType.STOCK);
        tx2.setDate(LocalDate.of(2024, 2, 15));
        tx2.setCredit(new BigDecimal("50.00"));
        tx2.setDebit(BigDecimal.ZERO);
        tx2.setDeletedAt(null);
        transactions.add(tx2);

        Transaction tx3 = new Transaction();
        tx3.setAccountId(testAccountId);
        tx3.setAssetName("GOOGL");
        tx3.setSymbol("GOOGL");
        tx3.setUnit("USD");
        tx3.setAssetType(AssetType.STOCK);
        tx3.setDate(LocalDate.of(2024, 2, 16));
        tx3.setCredit(new BigDecimal("25.00"));
        tx3.setDebit(BigDecimal.ZERO);
        tx3.setDeletedAt(null);
        transactions.add(tx3);

        return transactions;
    }

    private List<Transaction> createSampleTransactionsWithZeroBalance() {
        List<Transaction> transactions = new ArrayList<>();
        
        Transaction tx1 = new Transaction();
        tx1.setAccountId(testAccountId);
        tx1.setAssetName("AAPL");
        tx1.setSymbol("AAPL");
        tx1.setUnit("USD");
        tx1.setAssetType(AssetType.STOCK);
        tx1.setDate(LocalDate.of(2024, 1, 15));
        tx1.setCredit(new BigDecimal("100.00"));
        tx1.setDebit(BigDecimal.ZERO);
        tx1.setDeletedAt(null);
        transactions.add(tx1);

        Transaction tx2 = new Transaction();
        tx2.setAccountId(testAccountId);
        tx2.setAssetName("AAPL");
        tx2.setSymbol("AAPL");
        tx2.setUnit("USD");
        tx2.setAssetType(AssetType.STOCK);
        tx2.setDate(LocalDate.of(2024, 1, 16));
        tx2.setCredit(BigDecimal.ZERO);
        tx2.setDebit(new BigDecimal("100.00"));
        tx2.setDeletedAt(null);
        transactions.add(tx2);

        return transactions;
    }

    private List<Transaction> createSampleTransactionsWithNegativeBalance() {
        List<Transaction> transactions = new ArrayList<>();
        
        Transaction tx1 = new Transaction();
        tx1.setAccountId(testAccountId);
        tx1.setAssetName("AAPL");
        tx1.setSymbol("AAPL");
        tx1.setUnit("USD");
        tx1.setAssetType(AssetType.STOCK);
        tx1.setDate(LocalDate.of(2024, 1, 15));
        tx1.setCredit(BigDecimal.ZERO);
        tx1.setDebit(new BigDecimal("100.00"));
        tx1.setDeletedAt(null);
        transactions.add(tx1);

        return transactions;
    }

    private List<Transaction> createSampleTransactionsWithLargeNumbers() {
        List<Transaction> transactions = new ArrayList<>();
        
        Transaction tx1 = new Transaction();
        tx1.setAccountId(testAccountId);
        tx1.setAssetName("AAPL");
        tx1.setSymbol("AAPL");
        tx1.setUnit("USD");
        tx1.setAssetType(AssetType.STOCK);
        tx1.setDate(LocalDate.of(2024, 1, 15));
        tx1.setCredit(new BigDecimal("999999.99"));
        tx1.setDebit(BigDecimal.ZERO);
        tx1.setDeletedAt(null);
        transactions.add(tx1);

        return transactions;
    }
} 