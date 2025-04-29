package com.fintrack.service;

import com.fintrack.repository.*;
import com.fintrack.component.calculator.PortfolioCalculator;
import com.fintrack.component.chart.BarChart;
import com.fintrack.component.chart.CombinedBarChart;
import com.fintrack.component.chart.PieChart;
import com.fintrack.constants.AssetType;
import com.fintrack.model.*;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.stream.Collectors;

@Service
public class PortfolioService {

    private static final Logger logger = LogManager.getLogger(PortfolioService.class);

    private final HoldingsRepository holdingsRepository;
    private final HoldingsMonthlyRepository holdingsMonthlyRepository;
    private final HoldingsCategoriesRepository holdingsCategoriesRepository;
    private final MarketDataRepository marketDataRepository;
    private final CategoriesRepository categoriesRepository;
    private final SubcategoriesRepository subcategoriesRepository;
    private final MarketDataMonthlyRepository marketDataMonthlyRepository;

    public PortfolioService(
            HoldingsRepository holdingsRepository,
            HoldingsMonthlyRepository holdingsMonthlyRepository,
            HoldingsCategoriesRepository holdingsCategoriesRepository,
            MarketDataRepository marketDataRepository,
            MarketDataMonthlyRepository marketDataMonthlyRepository,
            CategoriesRepository categoriesRepository,
            SubcategoriesRepository subcategoriesRepository) {
        this.holdingsRepository = holdingsRepository;
        this.holdingsMonthlyRepository = holdingsMonthlyRepository;
        this.holdingsCategoriesRepository = holdingsCategoriesRepository;
        this.marketDataRepository = marketDataRepository;
        this.marketDataMonthlyRepository = marketDataMonthlyRepository;
        this.categoriesRepository = categoriesRepository;
        this.subcategoriesRepository = subcategoriesRepository;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> calculatePortfolioData(UUID accountId, String baseCurrency) {
        // Validate input
        if (accountId == null || baseCurrency == null || baseCurrency.isEmpty()) {
            throw new IllegalArgumentException("Account ID and base currency must not be null or empty.");
        }
    
        logger.debug("Calculating portfolio data for account ID: {} and base currency: {}", accountId, baseCurrency);
    
        // Fetch holdings for the given account ID
        List<Holdings> holdings = holdingsRepository.findHoldingsByAccount(accountId);
    
        holdings.forEach(holding -> {
            logger.trace("Holding: symbol={}, quantity={}", holding.getSymbol(), holding.getTotalBalance());
        });
    
        // Fetch market data for the symbols and asset types
        List<Object[]> symbolAssetTypePairs = holdings.stream()
                .map(holding -> new Object[]{holding.getSymbol(), holding.getAssetType()})
                .distinct()
                .collect(Collectors.toList());

        logger.trace("Distinct symbol and asset type pairs: {}", symbolAssetTypePairs);

        List<MarketDataDto> marketDataDtoList = new ArrayList<>();
        symbolAssetTypePairs.stream().forEach(pair -> {
            String symbol = (String) pair[0];
            AssetType assetType = (AssetType) pair[1];

            if (assetType == AssetType.FOREX) {
                // Handle FOREX symbols
                if (symbol.equals(baseCurrency)) {
                    // If the FOREX symbol is the same as the base currency, set price to 1
                    logger.trace("FOREX symbol matches base currency: symbol={}, priceInBaseCurrency=1", symbol);
                    marketDataDtoList.add(new MarketDataDto(symbol + "/" + symbol, BigDecimal.ONE, assetType));
                } else {
                    // Fetch the correct FOREX market data
                    String forexPair = symbol + "/" + baseCurrency;
                    List<MarketData> marketDataList = marketDataRepository.findMarketDataBySymbolAndAssetType(forexPair, assetType.name());
                    if (marketDataList.isEmpty()) {
                        // Try the reverse pair (e.g., USD/AUD instead of AUD/USD)
                        forexPair = baseCurrency + "/" + symbol;
                        marketDataList = marketDataRepository.findMarketDataBySymbolAndAssetType(forexPair, assetType.name());
                        if (!marketDataList.isEmpty()) {
                            // If reverse pair exists, calculate the inverse price
                            MarketData marketData = marketDataList.get(0);
                            logger.trace("Reverse FOREX pair found: forexPair={}, inversePrice={}", forexPair, marketData.getPrice());
                            marketDataDtoList.add(new MarketDataDto(forexPair, marketData.getPrice(), assetType));
                        } else {
                            logger.warn("No FOREX market data found for symbol={}, baseCurrency={}", symbol, baseCurrency);
                        }
                    } else {
                        MarketData marketData = marketDataList.get(0);
                        logger.trace("FOREX market data found: forexPair={}, price={}", forexPair, marketData.getPrice());
                        marketDataDtoList.add(new MarketDataDto(forexPair, marketData.getPrice(), assetType));
                    }
                }
            } else {
                // Handle non-FOREX symbols
                logger.info("Fetching market data for symbol: {}, assetType: {}", symbol, assetType);
                List<MarketData> marketDataList = marketDataRepository.findMarketDataBySymbolAndAssetType(symbol, assetType.name());
                marketDataList.forEach(marketData -> {
                    marketDataDtoList.add(new MarketDataDto(marketData));
                });
            }
        });
    
        marketDataDtoList.forEach(marketData -> {
            logger.trace("Market Data: symbol={}, assetType={}, price={}", marketData.getSymbol(), marketData.getAssetType(), marketData.getPrice());
        });
    
        // Create marketDataMap: String (symbol-assetType) -> MarketDataDto
        Map<String, MarketDataDto> marketDataMap = new HashMap<>();
        for (MarketDataDto marketDataDto : marketDataDtoList) {
            String key = marketDataDto.getSymbol() + "-" + marketDataDto.getAssetType();
            marketDataMap.put(key, marketDataDto);
        }
    
        // Calculate portfolio data
        PortfolioCalculator portfolioCalculator = new PortfolioCalculator(accountId, holdings, marketDataMap, baseCurrency);
        return portfolioCalculator.getPortfolioData();
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> calculatePortfolioPieChartDataEXP(UUID accountId, String categoryName, String baseCurrency) {
        // Validate input
        if (accountId == null || categoryName == null || categoryName.isEmpty() || baseCurrency == null || baseCurrency.isEmpty()) {
            throw new IllegalArgumentException("Account ID, category name and baseCurrency must not be null or empty.");
        }
    
        logger.debug("Calculating portfolio pie chart data for account ID: {} and category name: {}", accountId, categoryName);
    
        // Fetch holdings for the given account ID
        List<Holdings> holdings = holdingsRepository.findHoldingsByAccount(accountId);
    
        holdings.forEach(holding -> {
            logger.trace("Holding: symbol={}, quantity={}", holding.getSymbol(), holding.getTotalBalance());
        });
    
        // Fetch market data for the symbols and asset types
        List<Object[]> symbolAssetTypePairs = holdings.stream()
                .map(holding -> new Object[]{holding.getSymbol(), holding.getAssetType()})
                .distinct()
                .collect(Collectors.toList());

        List<MarketDataDto> marketDataDtoList = new ArrayList<>();

        symbolAssetTypePairs.forEach(pair -> {
            String symbol = (String) pair[0];
            AssetType assetType = (AssetType) pair[1];

            if (assetType == AssetType.FOREX) {
                marketDataDtoList.addAll(fetchForexMarketData(symbol, baseCurrency, assetType));
            } else {
                marketDataDtoList.addAll(fetchNonForexMarketData(symbol, assetType));
            }
        });
    
        // Create marketDataMap: String (symbol-assetType) -> MarketDataDto
        Map<String, MarketDataDto> marketDataMap = new HashMap<>();
        for (MarketDataDto marketDataDto : marketDataDtoList) {
            String key = marketDataDto.getSymbol() + "-" + marketDataDto.getAssetType();
            marketDataMap.put(key, marketDataDto);
        }

        logger.trace("Market Data Map: {}", marketDataMap);
    
        // Use PortfolioCalculator to calculate asset values
        PortfolioCalculator portfolioCalculator = new PortfolioCalculator(accountId, holdings, marketDataMap, baseCurrency);
    
        // If categoryName is "None", generate a simple pie chart
        if ("None".equalsIgnoreCase(categoryName)) {
            PieChart pieChart = new PieChart(portfolioCalculator);
            return pieChart.getData();
        }
    
        // Fetch the category ID for the given account and category name
        Integer categoryId = categoriesRepository.findCategoryIdByAccountIdAndCategoryName(accountId, categoryName);
        if (categoryId == null) {
            throw new IllegalArgumentException("Category not found for the given account and category name.");
        }
    
        // Fetch subcategories for the given account ID and category ID
        List<Category> subcategories = subcategoriesRepository.findSubcategoriesByParentId(accountId, categoryId);
        if (subcategories.isEmpty()) {
            throw new IllegalArgumentException("No subcategories found for the given account and category ID.");
        }
    
        subcategories.forEach(subcategory -> {
            logger.trace("Subcategory: {}", subcategory.getCategoryName());
        });
    
        // Fetch holdings categories for the given account ID
        List<HoldingsCategory> holdingsCategories = holdingsCategoriesRepository.findHoldingsCategoryByAccountId(accountId);
    
        // Generate a pie chart with categories and subcategories
        PieChart pieChart = new PieChart(portfolioCalculator, holdingsCategories, subcategories, categoryName);
        return pieChart.getData();
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> calculatePortfolioBarChartsData(UUID accountId, String categoryName) {
        // Validate input
        if (accountId == null || categoryName == null || categoryName.isEmpty()) {
            throw new IllegalArgumentException("Account ID and category name must not be null or empty.");
        }

        logger.debug("Calculating portfolio bar chart data for account ID: " + accountId + " and category name: " + categoryName);

        // Fetch monthly holdings for the given account ID
        List<HoldingsMonthly> monthlyHoldings = holdingsMonthlyRepository.findByAccountId(accountId);

        monthlyHoldings.forEach(monthlyHolding -> {
            logger.trace("Monthly Holding: " + monthlyHolding.getSymbol() + ", Quantity: " + monthlyHolding.getTotalBalance() + ", Date: " + monthlyHolding.getDate());
        });

        // Use TreeMap to ensure the keys (dates) are sorted in ascending order
        Map<LocalDate, List<Holdings>> holdingsByDate = monthlyHoldings.stream()
        .collect(Collectors.groupingBy(
                HoldingsMonthly::getDate,
                () -> new TreeMap<>(), // Use TreeMap to ensure sorted keys
                Collectors.mapping(HoldingsMonthly::getHoldings, Collectors.toList())
        ));

        List<BarChart> barCharts = new ArrayList<>();

        for (Map.Entry<LocalDate, List<Holdings>> entry : holdingsByDate.entrySet()) {
            LocalDate date = entry.getKey();
            List<Holdings> holdings = entry.getValue();

            // Fetch market data for the symbols
            List<String> symbols = holdings.stream()
                    .map(Holdings::getSymbol)
                    .distinct()
                    .collect(Collectors.toList());

            List<MarketDataDto> marketDataDtoList = marketDataMonthlyRepository.findBySymbolsAndDate(symbols, date).stream()
                    .map(marketDataMonthly -> new MarketDataDto(marketDataMonthly))
                    .collect(Collectors.toList());
            
            // Handle the case when categoryName is "None"
            if ("None".equalsIgnoreCase(categoryName)) {
                BarChart barChart = new BarChart(holdings, marketDataDtoList);
                barChart.setLocalDate(date);
                barCharts.add(barChart);
                continue;
            }

            marketDataDtoList.forEach(marketDtoData -> {
                logger.trace("Market Data: " + marketDtoData.getSymbol() + ", Price: " + marketDtoData.getPrice() + ", Date: " + date);
            });

            // Fetch the category ID for the given account and category name
            Integer categoryId = categoriesRepository.findCategoryIdByAccountIdAndCategoryName(accountId, categoryName);
            if (categoryId == null) {
                throw new IllegalArgumentException("Category not found for the given account and category name.");
            }
            // Fetch subcategories for the given account ID and category ID
            List<Category> subcategories = subcategoriesRepository.findSubcategoriesByParentId(accountId, categoryId);
            if (subcategories.isEmpty()) {
                throw new IllegalArgumentException("No subcategories found for the given account and category ID.");
            }
            subcategories.forEach(subcategory -> {
                logger.trace("Subcategory: " + subcategory.getCategoryName());
            });
            // Fetch holdings categories for the given account ID
            List<HoldingsCategory> holdingsCategories = holdingsCategoriesRepository.findHoldingsCategoryByAccountId(accountId);

            BarChart barChart = new BarChart(holdings, marketDataDtoList, holdingsCategories, subcategories, categoryName);
            barChart.setLocalDate(date);
            barCharts.add(barChart);
        }

        CombinedBarChart combinedBarCharts = new CombinedBarChart(barCharts, categoryName);

        return combinedBarCharts.getCombinedBarChartsData();
    }
    
    private List<MarketDataDto> fetchForexMarketData(String symbol, String baseCurrency, AssetType assetType) {
        List<MarketDataDto> forexMarketDataList = new ArrayList<>();
    
        if (symbol.equals(baseCurrency)) {
            // If the FOREX symbol is the same as the base currency, set price to 1
            logger.trace("FOREX symbol matches base currency: symbol={}, priceInBaseCurrency=1", symbol);
            forexMarketDataList.add(new MarketDataDto(symbol + "/" + symbol, BigDecimal.ONE, assetType));
        } else {
            // Fetch the correct FOREX market data
            String forexPair = symbol + "/" + baseCurrency;
            List<MarketData> marketDataList = marketDataRepository.findMarketDataBySymbolAndAssetType(forexPair, assetType.name());
    
            if (marketDataList.isEmpty()) {
                // Try the reverse pair (e.g., USD/AUD instead of AUD/USD)
                forexPair = baseCurrency + "/" + symbol;
                marketDataList = marketDataRepository.findMarketDataBySymbolAndAssetType(forexPair, assetType.name());
    
                if (!marketDataList.isEmpty()) {
                    // If reverse pair exists, calculate the inverse price
                    MarketData marketData = marketDataList.get(0);
                    BigDecimal inversePrice = BigDecimal.ONE.divide(marketData.getPrice(), 4, RoundingMode.HALF_UP);
                    logger.trace("Reverse FOREX pair found: forexPair={}, inversePrice={}", forexPair, inversePrice);
                    forexMarketDataList.add(new MarketDataDto(forexPair, inversePrice, assetType));
                } else {
                    logger.warn("No FOREX market data found for symbol={}, baseCurrency={}", symbol, baseCurrency);
                }
            } else {
                MarketData marketData = marketDataList.get(0);
                logger.trace("FOREX market data found: forexPair={}, price={}", forexPair, marketData.getPrice());
                forexMarketDataList.add(new MarketDataDto(forexPair, marketData.getPrice(), assetType));
            }
        }
    
        return forexMarketDataList;
    }
    
    private List<MarketDataDto> fetchNonForexMarketData(String symbol, AssetType assetType) {
        List<MarketDataDto> nonForexMarketDataList = new ArrayList<>();
    
        logger.info("Fetching market data for symbol: {}, assetType: {}", symbol, assetType);
        List<MarketData> marketDataList = marketDataRepository.findMarketDataBySymbolAndAssetType(symbol, assetType.name());
    
        marketDataList.forEach(marketData -> {
            nonForexMarketDataList.add(new MarketDataDto(marketData));
        });
    
        return nonForexMarketDataList;
    }
}