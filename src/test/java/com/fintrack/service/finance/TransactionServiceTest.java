package com.fintrack.service.finance;

import com.fintrack.model.finance.Transaction;
import com.fintrack.model.finance.Asset;
import com.fintrack.component.transaction.PreviewTransaction;
import com.fintrack.component.transaction.OverviewTransaction;
import com.fintrack.repository.finance.TransactionRepository;
import com.fintrack.repository.finance.AssetRepository;
import com.fintrack.repository.finance.HoldingsMonthlyRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

import com.fintrack.constants.finance.AssetType;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("TransactionService Tests")
class TransactionServiceTest {

    @Mock
    private TransactionRepository transactionRepository;
    
    @Mock
    private AssetRepository assetRepository;
    
    @Mock
    private HoldingsMonthlyRepository holdingsMonthlyRepository;
    
    @Mock
    private HoldingsService holdingsService;
    
    @Mock
    private HoldingsMonthlyService holdingsMonthlyService;
    
    @Mock
    private HoldingsCategoriesService holdingsCategoriesService;

    private TransactionService transactionService;
    private UUID testAccountId;

    @BeforeEach
    void setUp() {
        transactionService = new TransactionService(
            transactionRepository,
            assetRepository,
            holdingsMonthlyRepository,
            holdingsService,
            holdingsMonthlyService,
            holdingsCategoriesService
        );
        testAccountId = UUID.randomUUID();
    }

    @Test
    @DisplayName("Should retrieve transactions by account ID")
    void shouldRetrieveTransactionsByAccountId() {
        // Given: Sample transactions exist for the account
        List<Transaction> expectedTransactions = createSampleTransactions();
        when(transactionRepository.findByAccountIdOrderByDateDesc(testAccountId))
            .thenReturn(expectedTransactions);

        // When: Retrieving transactions by account ID
        List<Transaction> result = transactionService.getTransactionsByAccountId(testAccountId);

        // Then: Should return the expected transactions
        assertNotNull(result);
        assertEquals(expectedTransactions.size(), result.size());
        verify(transactionRepository).findByAccountIdOrderByDateDesc(testAccountId);
    }

    @Test
    @DisplayName("Should retrieve overview transactions by account ID")
    void shouldRetrieveOverviewTransactionsByAccountId() {
        // Given: Sample transactions exist for the account
        List<Transaction> transactions = createSampleTransactions();
        when(transactionRepository.findByAccountIdOrderByDateDesc(testAccountId))
            .thenReturn(transactions);

        // When: Retrieving overview transactions by account ID
        List<OverviewTransaction> result = transactionService.getOverviewTransactionsByAccountId(testAccountId);

        // Then: Should return overview transactions successfully
        assertNotNull(result);
        verify(transactionRepository).findByAccountIdOrderByDateDesc(testAccountId);
    }

    @Test
    @DisplayName("Should retrieve overview transactions with date range")
    void shouldRetrieveOverviewTransactionsWithDateRange() {
        // Given: Date range and sample transactions exist for the account
        LocalDate startDate = LocalDate.of(2024, 1, 1);
        LocalDate endDate = LocalDate.of(2024, 12, 31);
        List<Transaction> transactions = createSampleTransactions();
        
        when(transactionRepository.findByAccountIdAndDateBetweenOrderByDateDesc(testAccountId, startDate, endDate))
            .thenReturn(transactions);

        // When: Retrieving overview transactions with date range
        List<OverviewTransaction> result = transactionService.getOverviewTransactionsByAccountIdAndDateRange(
            testAccountId, startDate, endDate);

        // Then: Should return overview transactions for the specified date range
        assertNotNull(result);
        verify(transactionRepository).findByAccountIdAndDateBetweenOrderByDateDesc(testAccountId, startDate, endDate);
    }

    @Test
    @DisplayName("Should save all transactions successfully")
    void shouldSaveAllTransactions() {
        // Given: Sample transactions to be saved
        List<Transaction> transactions = createSampleTransactions();
        when(transactionRepository.save(any(Transaction.class))).thenReturn(transactions.get(0));

        // When: Saving all transactions for the account
        transactionService.saveAllTransactions(testAccountId, transactions);

        // Then: Should save each transaction individually
        verify(transactionRepository, times(transactions.size())).save(any(Transaction.class));
        // Note: saveAllTransactions only saves transactions, not assets
    }

    @Test
    @DisplayName("Should soft delete transactions by IDs")
    void shouldSoftDeleteTransactionsByIds() {
        // Given: List of transaction IDs to be soft deleted
        List<Long> transactionIds = Arrays.asList(1L, 2L, 3L);
        when(transactionRepository.softDeleteByTransactionIds(transactionIds)).thenReturn(transactionIds.size());

        // When: Soft deleting transactions by their IDs
        transactionService.softDeleteByTransactionIds(transactionIds);

        // Then: Should call repository to soft delete the transactions
        verify(transactionRepository).softDeleteByTransactionIds(transactionIds);
    }

    @Test
    @DisplayName("Should confirm transactions with new transactions and deletions")
    void shouldConfirmTransactionsWithNewAndDeletions() {
        // Given: Preview transactions with new transactions and deletions to be confirmed
        List<PreviewTransaction> previewTransactions = createSamplePreviewTransactions();
        
        // Mock the preview transactions processing
        when(transactionRepository.save(any(Transaction.class))).thenReturn(createSampleTransactions().get(0));
        when(transactionRepository.softDeleteByTransactionIds(anyList())).thenReturn(1);
        when(transactionRepository.findAllById(anyList())).thenReturn(createSampleTransactions());

        // When: Confirming the preview transactions
        transactionService.confirmTransactions(testAccountId, previewTransactions);

        // Then: Should save new transactions, soft delete marked transactions, and update related services
        verify(transactionRepository, times(1)).save(any(Transaction.class)); // Only new transaction
        verify(transactionRepository).softDeleteByTransactionIds(anyList());
        verify(holdingsService).updateHoldingsForAccount(testAccountId);
        verify(holdingsMonthlyService).updateMonthlyHoldingsForAccount(testAccountId);
        verify(holdingsCategoriesService).cleanupOrphanedAssets(testAccountId);
        verify(holdingsCategoriesService).cleanupOrphanedHoldingsCategories(testAccountId);
    }

    @Test
    @DisplayName("Should ensure assets exist for transactions")
    void shouldEnsureAssetsExist() {
        // Given: Preview transactions with assets that don't exist in the database
        List<PreviewTransaction> previewTransactions = createSamplePreviewTransactions();
        when(assetRepository.findByAccountIdAndAssetName(any(UUID.class), anyString()))
            .thenReturn(Optional.empty());

        // When: Ensuring assets exist for the transactions
        transactionService.ensureAssetsExist(testAccountId, previewTransactions);

        // Then: Should create new assets for each transaction
        verify(assetRepository, times(previewTransactions.size())).save(any(Asset.class));
    }

    @Test
    @DisplayName("Should process preview transactions correctly")
    void shouldProcessPreviewTransactions() {
        // Given: Sample transactions to be processed
        List<Transaction> transactions = createSampleTransactions();

        // When: Processing preview transactions
        List<Transaction> result = transactionService.processPreviewTransactions(transactions);

        // Then: Should return processed transactions with correct count
        assertNotNull(result);
        assertEquals(transactions.size(), result.size());
    }

    @Test
    @DisplayName("Should handle empty transaction list")
    void shouldHandleEmptyTransactionList() {
        // Given: Account with no transactions
        List<Transaction> emptyTransactions = new ArrayList<>();
        when(transactionRepository.findByAccountIdOrderByDateDesc(testAccountId))
            .thenReturn(emptyTransactions);

        // When: Retrieving transactions for the account
        List<Transaction> result = transactionService.getTransactionsByAccountId(testAccountId);

        // Then: Should return empty list
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("Should handle null date range parameters")
    void shouldHandleNullDateRangeParameters() {
        // Given: Sample transactions and null date range parameters
        List<Transaction> transactions = createSampleTransactions();
        when(transactionRepository.findByAccountIdOrderByDateDesc(testAccountId))
            .thenReturn(transactions);

        // When: Retrieving overview transactions with null date range
        List<OverviewTransaction> result = transactionService.getOverviewTransactionsByAccountIdAndDateRange(
            testAccountId, null, null);

        // Then: Should fall back to retrieving all transactions without date filter
        assertNotNull(result);
        verify(transactionRepository).findByAccountIdOrderByDateDesc(testAccountId);
    }

    // Helper methods to create test data
    private List<Transaction> createSampleTransactions() {
        List<Transaction> transactions = new ArrayList<>();
        
        Transaction transaction1 = new Transaction();
        transaction1.setTransactionId(1L);
        transaction1.setAccountId(testAccountId);
        transaction1.setAssetName("AAPL");
        transaction1.setDate(LocalDate.of(2024, 1, 15));
        transaction1.setCredit(new BigDecimal("100.00"));
        transaction1.setDebit(BigDecimal.ZERO);
        transaction1.setAssetType(AssetType.STOCK);
        transactions.add(transaction1);

        Transaction transaction2 = new Transaction();
        transaction2.setTransactionId(2L);
        transaction2.setAccountId(testAccountId);
        transaction2.setAssetName("GOOGL");
        transaction2.setDate(LocalDate.of(2024, 1, 16));
        transaction2.setCredit(BigDecimal.ZERO);
        transaction2.setDebit(new BigDecimal("50.00"));
        transaction2.setAssetType(AssetType.STOCK);
        transactions.add(transaction2);

        return transactions;
    }

    private List<PreviewTransaction> createSamplePreviewTransactions() {
        List<PreviewTransaction> previewTransactions = new ArrayList<>();
        
        // New transaction (no transactionId and no accountId)
        PreviewTransaction newTransaction = new PreviewTransaction();
        newTransaction.setTransactionId(null); // New transaction
        newTransaction.setAccountId(null); // New transaction
        newTransaction.setAssetName("TSLA");
        newTransaction.setDate(LocalDate.of(2024, 1, 17));
        newTransaction.setCredit(new BigDecimal("200.00"));
        newTransaction.setDebit(BigDecimal.ZERO);
        newTransaction.setAssetType(AssetType.STOCK);
        newTransaction.setMarkDelete(false);
        previewTransactions.add(newTransaction);

        // Transaction to delete
        PreviewTransaction deleteTransaction = new PreviewTransaction();
        deleteTransaction.setTransactionId(1L);
        deleteTransaction.setAccountId(testAccountId);
        deleteTransaction.setAssetName("AAPL");
        deleteTransaction.setDate(LocalDate.of(2024, 1, 15));
        deleteTransaction.setCredit(new BigDecimal("100.00"));
        deleteTransaction.setDebit(BigDecimal.ZERO);
        deleteTransaction.setAssetType(AssetType.STOCK);
        deleteTransaction.setMarkDelete(true);
        previewTransactions.add(deleteTransaction);

        return previewTransactions;
    }
} 