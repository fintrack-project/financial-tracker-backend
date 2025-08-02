package com.fintrack.service.finance;

import com.fintrack.dto.market.MarketDataDto;
import com.fintrack.model.finance.Category;
import com.fintrack.model.finance.Holdings;
import com.fintrack.model.finance.HoldingsCategory;
import com.fintrack.model.finance.HoldingsMonthly;
import com.fintrack.repository.finance.CategoriesRepository;
import com.fintrack.repository.finance.HoldingsCategoriesRepository;
import com.fintrack.repository.finance.HoldingsMonthlyRepository;
import com.fintrack.repository.finance.HoldingsRepository;
import com.fintrack.repository.finance.SubcategoriesRepository;
import com.fintrack.repository.market.MarketDataMonthlyRepository;
import com.fintrack.repository.market.MarketDataRepository;
import com.fintrack.service.market.MarketDataService;
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
@DisplayName("PortfolioService Tests")
class PortfolioServiceTest {

    @Mock
    private HoldingsRepository holdingsRepository;
    
    @Mock
    private HoldingsMonthlyRepository holdingsMonthlyRepository;
    
    @Mock
    private HoldingsCategoriesRepository holdingsCategoriesRepository;
    
    @Mock
    private MarketDataRepository marketDataRepository;
    
    @Mock
    private CategoriesRepository categoriesRepository;
    
    @Mock
    private SubcategoriesRepository subcategoriesRepository;
    
    @Mock
    private MarketDataMonthlyRepository marketDataMonthlyRepository;
    
    @Mock
    private MarketDataService marketDataService;

    private PortfolioService portfolioService;
    private UUID testAccountId;
    private String testBaseCurrency;

    @BeforeEach
    void setUp() {
        portfolioService = new PortfolioService(
            holdingsRepository,
            holdingsMonthlyRepository,
            holdingsCategoriesRepository,
            marketDataRepository,
            marketDataMonthlyRepository,
            categoriesRepository,
            subcategoriesRepository,
            marketDataService
        );
        testAccountId = UUID.randomUUID();
        testBaseCurrency = "USD";
    }

    @Test
    @DisplayName("Should throw exception when calculating portfolio data with null account ID")
    void shouldThrowExceptionWhenCalculatingPortfolioDataWithNullAccountId() {
        // Given: Null account ID
        UUID accountId = null;

        // When & Then: Should throw IllegalArgumentException
        assertThrows(IllegalArgumentException.class, () -> {
            portfolioService.calculatePortfolioData(accountId, testBaseCurrency);
        });
    }

    @Test
    @DisplayName("Should throw exception when calculating portfolio data with null base currency")
    void shouldThrowExceptionWhenCalculatingPortfolioDataWithNullBaseCurrency() {
        // Given: Null base currency
        String baseCurrency = null;

        // When & Then: Should throw IllegalArgumentException
        assertThrows(IllegalArgumentException.class, () -> {
            portfolioService.calculatePortfolioData(testAccountId, baseCurrency);
        });
    }

    @Test
    @DisplayName("Should throw exception when calculating portfolio data with empty base currency")
    void shouldThrowExceptionWhenCalculatingPortfolioDataWithEmptyBaseCurrency() {
        // Given: Empty base currency
        String baseCurrency = "";

        // When & Then: Should throw IllegalArgumentException
        assertThrows(IllegalArgumentException.class, () -> {
            portfolioService.calculatePortfolioData(testAccountId, baseCurrency);
        });
    }

    @Test
    @DisplayName("Should throw exception when calculating pie chart data with null account ID")
    void shouldThrowExceptionWhenCalculatingPieChartDataWithNullAccountId() {
        // Given: Null account ID
        UUID accountId = null;

        // When & Then: Should throw IllegalArgumentException
        assertThrows(IllegalArgumentException.class, () -> {
            portfolioService.calculatePortfolioPieChartData(accountId, "Technology", testBaseCurrency);
        });
    }

    @Test
    @DisplayName("Should throw exception when calculating pie chart data with null category name")
    void shouldThrowExceptionWhenCalculatingPieChartDataWithNullCategoryName() {
        // Given: Null category name
        String categoryName = null;

        // When & Then: Should throw IllegalArgumentException
        assertThrows(IllegalArgumentException.class, () -> {
            portfolioService.calculatePortfolioPieChartData(testAccountId, categoryName, testBaseCurrency);
        });
    }

    @Test
    @DisplayName("Should throw exception when calculating pie chart data with empty category name")
    void shouldThrowExceptionWhenCalculatingPieChartDataWithEmptyCategoryName() {
        // Given: Empty category name
        String categoryName = "";

        // When & Then: Should throw IllegalArgumentException
        assertThrows(IllegalArgumentException.class, () -> {
            portfolioService.calculatePortfolioPieChartData(testAccountId, categoryName, testBaseCurrency);
        });
    }

    @Test
    @DisplayName("Should throw exception when calculating pie chart data with null base currency")
    void shouldThrowExceptionWhenCalculatingPieChartDataWithNullBaseCurrency() {
        // Given: Null base currency
        String baseCurrency = null;

        // When & Then: Should throw IllegalArgumentException
        assertThrows(IllegalArgumentException.class, () -> {
            portfolioService.calculatePortfolioPieChartData(testAccountId, "Technology", baseCurrency);
        });
    }

    @Test
    @DisplayName("Should throw exception when calculating pie chart data with empty base currency")
    void shouldThrowExceptionWhenCalculatingPieChartDataWithEmptyBaseCurrency() {
        // Given: Empty base currency
        String baseCurrency = "";

        // When & Then: Should throw IllegalArgumentException
        assertThrows(IllegalArgumentException.class, () -> {
            portfolioService.calculatePortfolioPieChartData(testAccountId, "Technology", baseCurrency);
        });
    }

    @Test
    @DisplayName("Should throw exception when calculating bar charts data with null account ID")
    void shouldThrowExceptionWhenCalculatingBarChartsDataWithNullAccountId() {
        // Given: Null account ID
        UUID accountId = null;

        // When & Then: Should throw IllegalArgumentException
        assertThrows(IllegalArgumentException.class, () -> {
            portfolioService.calculatePortfolioBarChartsData(accountId, "Technology", testBaseCurrency);
        });
    }

    @Test
    @DisplayName("Should throw exception when calculating bar charts data with null category name")
    void shouldThrowExceptionWhenCalculatingBarChartsDataWithNullCategoryName() {
        // Given: Null category name
        String categoryName = null;

        // When & Then: Should throw IllegalArgumentException
        assertThrows(IllegalArgumentException.class, () -> {
            portfolioService.calculatePortfolioBarChartsData(testAccountId, categoryName, testBaseCurrency);
        });
    }

    @Test
    @DisplayName("Should throw exception when calculating bar charts data with empty category name")
    void shouldThrowExceptionWhenCalculatingBarChartsDataWithEmptyCategoryName() {
        // Given: Empty category name
        String categoryName = "";

        // When & Then: Should throw IllegalArgumentException
        assertThrows(IllegalArgumentException.class, () -> {
            portfolioService.calculatePortfolioBarChartsData(testAccountId, categoryName, testBaseCurrency);
        });
    }

    @Test
    @DisplayName("Should handle portfolio calculation with empty holdings")
    void shouldHandlePortfolioCalculationWithEmptyHoldings() {
        // Given: Empty holdings
        List<Holdings> holdings = new ArrayList<>();
        
        when(holdingsRepository.findHoldingsByAccount(testAccountId))
            .thenReturn(holdings);

        // When: Calculating portfolio data with empty holdings
        List<Map<String, Object>> result = portfolioService.calculatePortfolioData(testAccountId, testBaseCurrency);

        // Then: Should return empty result
        assertNotNull(result);
        verify(holdingsRepository).findHoldingsByAccount(testAccountId);
    }

    @Test
    @DisplayName("Should handle portfolio calculation with large holdings dataset")
    void shouldHandlePortfolioCalculationWithLargeHoldingsDataset() {
        // Given: Large holdings dataset
        List<Holdings> holdings = createLargeHoldingsDataset();
        
        when(holdingsRepository.findHoldingsByAccount(testAccountId))
            .thenReturn(holdings);

        // When: Calculating portfolio data with large dataset
        List<Map<String, Object>> result = portfolioService.calculatePortfolioData(testAccountId, testBaseCurrency);

        // Then: Should handle large dataset efficiently
        assertNotNull(result);
        verify(holdingsRepository).findHoldingsByAccount(testAccountId);
    }

    // Helper methods to create test data
    private List<Holdings> createLargeHoldingsDataset() {
        List<Holdings> holdings = new ArrayList<>();
        
        for (int i = 1; i <= 100; i++) {
            Holdings holding = new Holdings();
            holding.setAccountId(testAccountId);
            holding.setAssetName("STOCK" + i);
            holding.setSymbol("STOCK" + i);
            holding.setUnit("USD");
            holding.setAssetType(AssetType.STOCK);
            holding.setTotalBalance(i * 10.0);
            holdings.add(holding);
        }

        return holdings;
    }
} 