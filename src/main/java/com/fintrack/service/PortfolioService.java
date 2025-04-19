package com.fintrack.service;

import com.fintrack.repository.*;
import com.fintrack.component.PieChart;
import com.fintrack.model.*;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class PortfolioService {

    private static final Logger logger = LogManager.getLogger(PortfolioService.class);

    private final HoldingsRepository holdingsRepository;
    private final HoldingsCategoriesRepository holdingsCategoriesRepository;
    private final MarketDataRepository marketDataRepository;
    private final CategoriesRepository categoriesRepository;
    private final SubcategoriesRepository subcategoriesRepository;

    public PortfolioService(
            HoldingsRepository holdingsRepository,
            HoldingsCategoriesRepository holdingsCategoriesRepository,
            MarketDataRepository marketDataRepository,
            CategoriesRepository categoriesRepository,
            SubcategoriesRepository subcategoriesRepository) {
        this.holdingsRepository = holdingsRepository;
        this.holdingsCategoriesRepository = holdingsCategoriesRepository;
        this.marketDataRepository = marketDataRepository;
        this.categoriesRepository = categoriesRepository;
        this.subcategoriesRepository = subcategoriesRepository;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> calculatePortfolioPieChartData(UUID accountId, String categoryName) {
        // Validate input
        if (accountId == null || categoryName == null || categoryName.isEmpty()) {
            throw new IllegalArgumentException("Account ID and category name must not be null or empty.");
        }

        logger.info("Calculating portfolio pie chart data for account ID: " + accountId + " and category name: " + categoryName);

        // Fetch holdings for the given account ID
        List<Holdings> holdings = holdingsRepository.findHoldingsByAccount(accountId);
        holdings.stream()
                .forEach(holding -> logger.info("Holdings: " + holding.getAssetName() + ", Symbol: " + holding.getSymbol() + ", Total Balance: " + holding.getTotalBalance()));

        // Fetch market data for the symbols
        List<String> symbols = holdings.stream()
                .map(Holdings::getSymbol)
                .distinct()
                .collect(Collectors.toList());
        List<MarketData> marketDataList = marketDataRepository.findMarketDataBySymbols(symbols);
        logger.info("Generating Piechart... Market Data: " + marketDataList);

        // Handle the case when categoryName is "None"
        if ("None".equalsIgnoreCase(categoryName)) {
            PieChart pieChart = new PieChart(holdings, marketDataList);
            return pieChart.getData();
        }

        // Fetch the category ID for the given account and category name
        Integer categoryId = categoriesRepository.findCategoryIdByAccountIdAndCategoryName(accountId, categoryName);
        if (categoryId == null) {
            throw new IllegalArgumentException("Category not found for the given account and category name.");
        }
        
        logger.info("Category ID for category name '" + categoryName + "' is: " + categoryId);

        // Fetch holdings categories for the given account ID
        List<HoldingsCategory> holdingsCategories = holdingsCategoriesRepository.findHoldingsCategoryByAccountId(accountId);

        PieChart pieChart = new PieChart(holdings, marketDataList, holdingsCategories, categoryName);
        return pieChart.getData();
    }
}