import com.fintrack.repository.*;

import com.fintrack.model.*;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class PortfolioService {

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
        // Fetch the category ID for the given account and category name
        Integer categoryId = categoriesRepository.findCategoryIdByAccountIdAndCategoryName(accountId, categoryName);

        if (categoryId == null) {
            throw new IllegalArgumentException("Category not found for the given account and category name.");
        }

        // Fetch subcategories for the given category ID
        List<Category> subcategories = subcategoriesRepository.findSubcategoriesByParentId(accountId, categoryId);

        // Fetch asset name and subcateory map for the category
        List<Map<String, Object>> assetNamesSubcategoryEntries = holdingsCategoriesRepository.findHoldingsByCategoryId(accountId, categoryId);
        Map<String, Object> assetNamesSubcategoryMap = assetNamesSubcategoryEntries.stream()
        .collect(Collectors.toMap(
                entry -> (String) entry.get("asset_name"),
                entry -> entry.get("subcategory_name")
        ));

        // Fetch holdings for the given account ID
        List<Holdings> holdings = holdingsRepository.findHoldingsByAccount(accountId);

        // Extract symbols and total balances
        Map<String, Double> symbolToBalanceMap = new HashMap<>();
        for (Holdings holding : holdings) {
            String symbol = holding.getSymbol();
            Double totalBalance = holding.getTotalBalance();
            symbolToBalanceMap.put(symbol, totalBalance);
        }

        // Fetch market data for the symbols
        List<String> symbols = new ArrayList<>(symbolToBalanceMap.keySet());
        List<MarketData> marketDataList = marketDataRepository.findMarketDataBySymbols(symbols);

        // Map to store total values per subcategory
        Map<String, Double> subcategoryTotals = new HashMap<>();
        
        for(Holdings holding : holdings) {
            String symbol = holding.getSymbol();
            Double totalBalance = holding.getTotalBalance();
            String subcategory = (String) assetNamesSubcategoryMap.get(holding.getAssetName());

            // Calculate total value
            if (totalBalance != null) {
                double totalValue = totalBalance * 0; // Placeholder for price, to be replaced with actual price from market data
                subcategoryTotals.put(subcategory, subcategoryTotals.getOrDefault(subcategory, 0.0) + totalValue);
            }
        }

        // Convert the map to a list of maps and sort by total value descending
        List<Map<String, Object>> pieChartData = subcategoryTotals.entrySet().stream()
                .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder())) // Sort by total value descending
                .map(entry -> Map.<String, Object>of(
                        "name", entry.getKey(),
                        "value", entry.getValue(),
                        "color", getRandomColor() // Assign a random color
                ))
                .collect(Collectors.toList());

        return pieChartData;
    }

    private String getRandomColor() {
        Random random = new Random();
        int r = random.nextInt(256);
        int g = random.nextInt(256);
        int b = random.nextInt(256);
        return String.format("#%02x%02x%02x", r, g, b); // Return color in hex format
    }
}