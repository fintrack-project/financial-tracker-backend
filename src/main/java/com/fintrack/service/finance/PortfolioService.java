package com.fintrack.service.finance;

import com.fintrack.repository.finance.CategoriesRepository;
import com.fintrack.repository.finance.HoldingsCategoriesRepository;
import com.fintrack.repository.finance.HoldingsMonthlyRepository;
import com.fintrack.repository.finance.HoldingsRepository;
import com.fintrack.repository.finance.SubcategoriesRepository;
import com.fintrack.repository.market.MarketDataMonthlyRepository;
import com.fintrack.repository.market.MarketDataRepository;
import com.fintrack.component.calculator.PortfolioCalculator;
import com.fintrack.component.chart.BarChart;
import com.fintrack.component.chart.CombinedBarChart;
import com.fintrack.component.chart.PieChart;
import com.fintrack.constants.finance.AssetType;
import com.fintrack.dto.market.MarketDataDto;
import com.fintrack.model.finance.Category;
import com.fintrack.model.finance.Holdings;
import com.fintrack.model.finance.HoldingsCategory;
import com.fintrack.model.finance.HoldingsMonthly;
import com.fintrack.model.market.MarketData;
import com.fintrack.model.market.MarketDataMonthly;
import com.fintrack.service.market.MarketDataService;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.stream.Collectors;
import java.util.function.BiFunction;

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
    private final MarketDataService marketDataService;

    public PortfolioService(
            HoldingsRepository holdingsRepository,
            HoldingsMonthlyRepository holdingsMonthlyRepository,
            HoldingsCategoriesRepository holdingsCategoriesRepository,
            MarketDataRepository marketDataRepository,
            MarketDataMonthlyRepository marketDataMonthlyRepository,
            CategoriesRepository categoriesRepository,
            SubcategoriesRepository subcategoriesRepository,
            MarketDataService marketDataService) {
        this.holdingsRepository = holdingsRepository;
        this.holdingsMonthlyRepository = holdingsMonthlyRepository;
        this.holdingsCategoriesRepository = holdingsCategoriesRepository;
        this.marketDataRepository = marketDataRepository;
        this.marketDataMonthlyRepository = marketDataMonthlyRepository;
        this.categoriesRepository = categoriesRepository;
        this.subcategoriesRepository = subcategoriesRepository;
        this.marketDataService = marketDataService;
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
        logHoldings(holdings, null);
    
        // Fetch market data for the symbols and asset types
        List<Object[]> symbolAssetTypePairs = extractDistinctSymbolAssetTypePairs(holdings);
        
        // Ensure we have up-to-date market data for all symbols in the portfolio
        refreshMarketDataForPortfolio(accountId, symbolAssetTypePairs);
        
        List<MarketDataDto> marketDataDtoList = fetchMarketDataForPairs(symbolAssetTypePairs, baseCurrency, null);
    
        marketDataDtoList.forEach(marketData -> {
            logger.trace("Market Data: symbol={}, assetType={}, price={}", marketData.getSymbol(), marketData.getAssetType(), marketData.getPrice());
        });
    
        // Create marketDataMap
        Map<String, MarketDataDto> marketDataMap = createMarketDataMap(marketDataDtoList);
    
        // Calculate portfolio data
        PortfolioCalculator portfolioCalculator = new PortfolioCalculator(accountId, holdings, marketDataMap, baseCurrency);
        return portfolioCalculator.getPortfolioData();
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> calculatePortfolioPieChartData(UUID accountId, String categoryName, String baseCurrency) {
        // Validate input
        if (accountId == null || categoryName == null || categoryName.isEmpty() || baseCurrency == null || baseCurrency.isEmpty()) {
            throw new IllegalArgumentException("Account ID, category name and baseCurrency must not be null or empty.");
        }
    
        logger.debug("Calculating portfolio pie chart data for account ID: {} and category name: {}", accountId, categoryName);
    

        // Fetch holdings for the given account ID
        List<Holdings> holdings = holdingsRepository.findHoldingsByAccount(accountId);

        // Log holdings
        logHoldings(holdings, null);
        
        // Fetch market data for the symbols and asset types
        List<Object[]> symbolAssetTypePairs = extractDistinctSymbolAssetTypePairs(holdings);
        
        // Ensure we have up-to-date market data for all symbols in the portfolio
        refreshMarketDataForPortfolio(accountId, symbolAssetTypePairs);
        
        List<MarketDataDto> marketDataDtoList = fetchMarketDataForPairs(symbolAssetTypePairs, baseCurrency, null);
    
        // Create marketDataMap
        Map<String, MarketDataDto> marketDataMap = createMarketDataMap(marketDataDtoList);

        logger.trace("Market Data Map: {}", marketDataMap);
    
        // Use PortfolioCalculator to calculate asset values
        PortfolioCalculator portfolioCalculator = new PortfolioCalculator(accountId, holdings, marketDataMap, baseCurrency);
    
        // If categoryName is "None", generate a simple pie chart
        if ("None".equalsIgnoreCase(categoryName)) {
            PieChart pieChart = new PieChart(portfolioCalculator);
            return pieChart.getData();
        }

        // Fetch the category ID for the given account and category name
        Map<String, Object> categoryData = fetchCategoryAndSubcategories(accountId, categoryName);

        Integer categoryId = (Integer) categoryData.get("categoryId");
        List<Category> subcategories = (List<Category>) categoryData.get("subcategories");
        List<HoldingsCategory> holdingsCategories = (List<HoldingsCategory>) categoryData.get("holdingsCategories");
    
        // Generate a pie chart with categories and subcategories
        PieChart pieChart = new PieChart(portfolioCalculator, holdingsCategories, subcategories, categoryName);
        return pieChart.getData();
    }
    
    @Transactional(readOnly = true)
    public List<Map<String, Object>> calculatePortfolioBarChartsData(UUID accountId, String categoryName, String baseCurrency) {
        // Validate input
        if (accountId == null || categoryName == null || categoryName.isEmpty()) {
            throw new IllegalArgumentException("Account ID and category name must not be null or empty.");
        }

        logger.debug("Calculating portfolio bar chart data for account ID: " + accountId + " and category name: " + categoryName);

        // Fetch monthly holdings for the given account ID
        List<HoldingsMonthly> monthlyHoldings = holdingsMonthlyRepository.findByAccountId(accountId);

        monthlyHoldings.forEach(monthlyHolding -> {
            logger.trace("Monthly Holding, Date: " + monthlyHolding.getDate() + ", Symbol: " + monthlyHolding.getSymbol() + ", Asset Type: " + monthlyHolding.getAssetType().getAssetTypeName() + ", Quantity: " + monthlyHolding.getTotalBalance());
        });

        // Use TreeMap to ensure the keys (dates) are sorted in ascending order
        Map<LocalDate, List<Holdings>> holdingsByDate = monthlyHoldings.stream()
        .collect(Collectors.groupingBy(
                HoldingsMonthly::getDate,
                () -> new TreeMap<>(), // Use TreeMap to ensure sorted keys
                Collectors.mapping(HoldingsMonthly::getHoldings, Collectors.toList())
        ));

        holdingsByDate.forEach((date, holdings) -> {
            holdings.forEach(holding -> {
                logger.trace("Date: {}, Holding: symbol={}, assetType = {}, quantity={}", date, holding.getSymbol(), holding.getAssetType().getAssetTypeName(), holding.getTotalBalance());
            });
        });

        List<BarChart> barCharts = new ArrayList<>();

        for (Map.Entry<LocalDate, List<Holdings>> entry : holdingsByDate.entrySet()) {
            LocalDate date = entry.getKey();
            List<Holdings> holdings = entry.getValue();

            // Log holdings
            logHoldings(holdings, date);
            
            // Fetch market data
            List<Object[]> symbolAssetTypePairs = extractDistinctSymbolAssetTypePairs(holdings);
            
            // Ensure we have market data for the historical date
            // Note: For historical data, we don't need to refresh as it's not real-time
            List<MarketDataDto> marketDataDtoList = fetchMarketDataForPairs(symbolAssetTypePairs, baseCurrency, date);

            // Create market data map
            Map<String, MarketDataDto> marketDataMap = createMarketDataMap(marketDataDtoList);

            logger.trace("Date = {}, Market Data Map: {}", date, marketDataMap);

            // Use PortfolioCalculator to calculate asset values
            PortfolioCalculator portfolioCalculator = new PortfolioCalculator(accountId, holdings, marketDataMap, baseCurrency);
            
            // Handle the case when categoryName is "None"
            if ("None".equalsIgnoreCase(categoryName)) {
                BarChart barChart = new BarChart(portfolioCalculator);
                barChart.setLocalDate(date);
                barCharts.add(barChart);
                continue;
            }

            // Fetch the category ID for the given account and category name
            Map<String, Object> categoryData = fetchCategoryAndSubcategories(accountId, categoryName);

            Integer categoryId = (Integer) categoryData.get("categoryId");
            List<Category> subcategories = (List<Category>) categoryData.get("subcategories");
            List<HoldingsCategory> holdingsCategories = (List<HoldingsCategory>) categoryData.get("holdingsCategories");

            BarChart barChart = new BarChart(portfolioCalculator, holdingsCategories, subcategories, categoryName);
            barChart.setLocalDate(date);
            barCharts.add(barChart);
        }

        // Add current date holdings if the current date is not the 1st of the month
        LocalDate currentDate = LocalDate.now();
        if (currentDate.getDayOfMonth() != 1) {
            List<Holdings> currentHoldings = holdingsRepository.findHoldingsByAccount(accountId);

            logHoldings(currentHoldings, currentDate);

            if (!currentHoldings.isEmpty()) {
                logger.trace("Adding current holdings for date: {}", currentDate);

                // Fetch market data for the current holdings
                List<Object[]> symbolAssetTypePairs = extractDistinctSymbolAssetTypePairs(currentHoldings);
                
                // Ensure we have up-to-date market data for current holdings
                refreshMarketDataForPortfolio(accountId, symbolAssetTypePairs);
                
                List<MarketDataDto> currentMarketDataDtoList = fetchMarketDataForPairs(symbolAssetTypePairs, baseCurrency, null);

                // Create marketDataMap for current data
                Map<String, MarketDataDto> currentMarketDataMap = createMarketDataMap(currentMarketDataDtoList);

                logger.trace("Current Market Data Map: {}", currentMarketDataMap);

                // Use PortfolioCalculator to calculate asset values for the current date
                PortfolioCalculator portfolioCalculator = new PortfolioCalculator(accountId, currentHoldings, currentMarketDataMap, baseCurrency);

                // Add current holdings and calculated data to the map
                holdingsByDate.put(currentDate, currentHoldings);

                if ("None".equalsIgnoreCase(categoryName)) {
                    BarChart barChart = new BarChart(portfolioCalculator);
                    barChart.setLocalDate(currentDate);
                    barCharts.add(barChart);
                }
                else{
                    // Fetch the category ID for the given account and category name
                    Map<String, Object> categoryData = fetchCategoryAndSubcategories(accountId, categoryName);

                    Integer categoryId = (Integer) categoryData.get("categoryId");
                    List<Category> subcategories = (List<Category>) categoryData.get("subcategories");
                    List<HoldingsCategory> holdingsCategories = (List<HoldingsCategory>) categoryData.get("holdingsCategories");

                    BarChart barChart = new BarChart(portfolioCalculator, holdingsCategories, subcategories, categoryName);
                    barChart.setLocalDate(currentDate);
                    barCharts.add(barChart);
                }
            }
        }

        CombinedBarChart combinedBarCharts = new CombinedBarChart(barCharts, categoryName);

        return combinedBarCharts.getCombinedBarChartsData();
    }

    private List<Object[]> extractDistinctSymbolAssetTypePairs(List<Holdings> holdings) {
        return holdings.stream()
                .map(holding -> new Object[]{holding.getSymbol(), holding.getAssetType()})
                .distinct()
                .collect(Collectors.toList());
    }

    private Map<String, MarketDataDto> createMarketDataMap(List<MarketDataDto> marketDataDtoList) {
        return marketDataDtoList.stream()
                .collect(Collectors.toMap(
                        dto -> dto.getSymbol() + "-" + dto.getAssetType(),
                        dto -> dto
                ));
    }

    private List<MarketDataDto> fetchMarketDataForPairs(List<Object[]> symbolAssetTypePairs, String baseCurrency, LocalDate date) {
        List<MarketDataDto> marketDataDtoList = new ArrayList<>();
    
        symbolAssetTypePairs.forEach(pair -> {
            String symbol = (String) pair[0];
            AssetType assetType = (AssetType) pair[1];
    
            if (assetType == AssetType.FOREX) {
                if (date == null) {
                    marketDataDtoList.addAll(fetchForexMarketData(symbol, baseCurrency, assetType));
                } else {
                    marketDataDtoList.addAll(fetchForexMarketDataByDate(symbol, baseCurrency, assetType, date));
                }
            } else {
                if (date == null) {
                    marketDataDtoList.addAll(fetchNonForexMarketData(symbol, assetType));
                } else {
                    marketDataDtoList.addAll(fetchNonForexMarketDataByDate(symbol, assetType, date));
                }
            }
        });
    
        return marketDataDtoList;
    }

    private void logHoldings(List<Holdings> holdings, LocalDate date) {
        holdings.forEach(holding -> {
            if (date != null) {
                logger.trace("Holding: date={}, symbol={}, assetType={}, quantity={}", date, holding.getSymbol(), holding.getAssetType().getAssetTypeName(), holding.getTotalBalance());
            } else {
                logger.trace("Holding: symbol={}, assetType={}, quantity={}", holding.getSymbol(), holding.getAssetType().getAssetTypeName(), holding.getTotalBalance());
            }
        });
    }

    private Map<String, Object> fetchCategoryAndSubcategories(UUID accountId, String categoryName) {
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
    
        // Return the results as a map
        Map<String, Object> result = new HashMap<>();
        result.put("categoryId", categoryId);
        result.put("subcategories", subcategories);
        result.put("holdingsCategories", holdingsCategories);
        return result;
    }

    private List<MarketDataDto> fetchForexMarketData(String symbol, String baseCurrency, AssetType assetType) {
        return fetchMarketData(
            symbol,
            baseCurrency,
            assetType,
            null,
            true,
            (pair, date) -> marketDataRepository.findMarketDataBySymbolAndAssetType(pair, assetType.getAssetTypeName()),
            (pair, date) -> marketDataRepository.findMarketDataBySymbolAndAssetType(pair, assetType.getAssetTypeName())
        );
    }
    
    private List<MarketDataDto> fetchForexMarketDataByDate(String symbol, String baseCurrency, AssetType assetType, LocalDate date) {
        return fetchMarketData(
            symbol,
            baseCurrency,
            assetType,
            date,
            true,
            (pair, d) -> marketDataMonthlyRepository.findMarketDataBySymbolAndAssetTypeAndDate(pair, assetType.getAssetTypeName(), d),
            (pair, d) -> marketDataMonthlyRepository.findMarketDataBySymbolAndAssetTypeAndDate(pair, assetType.getAssetTypeName(), d)
        );
    }

    private List<MarketDataDto> fetchNonForexMarketData(String symbol, AssetType assetType) {
        return fetchMarketData(
            symbol,
            null,
            assetType,
            null,
            false,
            (pair, date) -> marketDataRepository.findMarketDataBySymbolAndAssetType(pair, assetType.getAssetTypeName()),
            (pair, date) -> Collections.emptyList() // No reverse pair for non-FOREX
        );
    }
    
    private List<MarketDataDto> fetchNonForexMarketDataByDate(String symbol, AssetType assetType, LocalDate date) {
        // First try to get data for the requested date
        List<MarketDataDto> result = fetchMarketData(
            symbol,
            null,
            assetType,
            date,
            false,
            (pair, d) -> marketDataMonthlyRepository.findMarketDataBySymbolAndAssetTypeAndDate(pair, assetType.getAssetTypeName(), d),
            (pair, d) -> Collections.emptyList() // No reverse pair for non-FOREX
        );
        
        // If no data found for the requested date, try to find the most recent data
        if (result.isEmpty()) {
            logger.info("No market data found for symbol={}, assetType={} on date={}. Looking for most recent data.", 
                    symbol, assetType, date);
            
            // Find the most recent date before the requested date that has data
            List<MarketDataMonthly> historicalData = marketDataMonthlyRepository.findBySymbolAndDateRange(
                    symbol, 
                    date.minusMonths(3), // Look back up to 3 months
                    date);
            
            if (!historicalData.isEmpty()) {
                // Sort by date in descending order to get most recent first
                historicalData.sort((a, b) -> b.getDate().compareTo(a.getDate()));
                
                // Get the most recent data
                MarketDataMonthly mostRecentData = historicalData.get(0);
                BigDecimal price = mostRecentData.getPrice();
                
                logger.info("Found most recent data for symbol={}, assetType={} on date={}. Price: {}", 
                        symbol, assetType, mostRecentData.getDate(), price);
                
                // Create a market data DTO with the most recent price
                result.add(new MarketDataDto(symbol, price, assetType));
            }
        }
        
        return result;
    }

    private <T> List<MarketDataDto> fetchMarketData(
            String symbol,
            String baseCurrency,
            AssetType assetType,
            LocalDate date,
            boolean isForex,
            BiFunction<String, LocalDate, List<T>> fetchFunction,
            BiFunction<String, LocalDate, List<T>> fetchReverseFunction) {
    
        List<MarketDataDto> marketDataDtoList = new ArrayList<>();
        logger.info("Fetching market data for symbol: {}, assetType: {}, date: {}", symbol, assetType, date);
    
        if (symbol.equals(baseCurrency)) {
            // If the symbol matches the base currency, set price to 1
            logger.trace("Symbol matches base currency: symbol={}, priceInBaseCurrency=1", symbol);
            marketDataDtoList.add(new MarketDataDto(symbol + "/" + symbol, BigDecimal.ONE, assetType));
        } else {
            // Fetch the correct market data
            String pair = isForex ? symbol + "/" + baseCurrency : symbol;
            List<T> marketDataList = fetchFunction.apply(pair, date);
    
            if (marketDataList.isEmpty() && isForex) {
                // Try the reverse pair for FOREX
                pair = baseCurrency + "/" + symbol;
                marketDataList = fetchReverseFunction.apply(pair, date);
    
                if (!marketDataList.isEmpty()) {
                    // If reverse pair exists, calculate the inverse price
                    T marketData = marketDataList.get(0);
                    BigDecimal price = getPriceFromMarketData(marketData);
                    BigDecimal inversePrice = BigDecimal.ONE.divide(price, 4, RoundingMode.HALF_UP);
                    logger.trace("Reverse pair found: pair={}, inversePrice={}", pair, inversePrice);
                    marketDataDtoList.add(new MarketDataDto(pair, inversePrice, assetType));
                } else {
                    logger.warn("No market data found for symbol={}, baseCurrency={}", symbol, baseCurrency);
                }
            } else if (!marketDataList.isEmpty()) {
                T marketData = marketDataList.get(0);
                BigDecimal price = getPriceFromMarketData(marketData);
                logger.trace("Market data found: pair={}, price={}", pair, price);
                marketDataDtoList.add(new MarketDataDto(pair, price, assetType));
            }
        }
    
        return marketDataDtoList;
    }
    
    private <T> BigDecimal getPriceFromMarketData(T marketData) {
        if (marketData instanceof MarketData) {
            return ((MarketData) marketData).getPrice();
        } else if (marketData instanceof MarketDataMonthly) {
            return ((MarketDataMonthly) marketData).getPrice();
        }
        throw new IllegalArgumentException("Unsupported market data type: " + marketData.getClass().getName());
    }

    /**
     * Ensures that market data is refreshed for all symbols in the portfolio
     * by directly calling MarketDataService instead of relying on Kafka messages
     */
    private void refreshMarketDataForPortfolio(UUID accountId, List<Object[]> symbolAssetTypePairs) {
        // Convert symbol-assetType pairs to the format expected by MarketDataService
        List<Map<String, String>> entities = new ArrayList<>();
        for (Object[] pair : symbolAssetTypePairs) {
            String symbol = (String) pair[0];
            AssetType assetType = (AssetType) pair[1];
            
            Map<String, String> entity = new HashMap<>();
            entity.put("symbol", symbol);
            entity.put("assetType", assetType.getAssetTypeName());
            entities.add(entity);
        }
        
        // Directly call MarketDataService to fetch data instead of using Kafka
        if (!entities.isEmpty()) {
            logger.info("Directly refreshing market data for {} assets", entities.size());
            try {
                List<MarketData> refreshedData = marketDataService.fetchMarketData(accountId, entities);
                logger.info("Successfully refreshed market data for {} assets", refreshedData.size());
            } catch (Exception e) {
                logger.error("Error refreshing market data: {}", e.getMessage(), e);
                // Continue with available data even if refresh fails
            }
        }
    }
}