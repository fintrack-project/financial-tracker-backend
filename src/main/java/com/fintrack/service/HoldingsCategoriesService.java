package com.fintrack.service;

import com.fintrack.repository.HoldingsCategoriesRepository;
import com.fintrack.repository.CategoriesRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
public class HoldingsCategoriesService {

    private final HoldingsCategoriesRepository holdingsCategoriesRepository;
    private final CategoriesRepository categoriesRepository;

    public HoldingsCategoriesService(
      HoldingsCategoriesRepository holdingsCategoriesRepository, 
      CategoriesRepository categoriesRepository) {
        this.holdingsCategoriesRepository = holdingsCategoriesRepository;
        this.categoriesRepository = categoriesRepository;
    }

    @Transactional
    public void updateHoldingsCategories(UUID accountId, Map<String, Map<String, String>> holdingsCategories) {
        System.out.println("Received holdingsCategories: " + holdingsCategories);
    
        for (Map.Entry<String, Map<String, String>> categoryEntry : holdingsCategories.entrySet()) {
            String categoryName = categoryEntry.getKey();
            Map<String, String> assets = categoryEntry.getValue();
    
            // Validate category name
            if (categoryName == null || categoryName.trim().isEmpty()) {
                throw new IllegalArgumentException("Category name cannot be null or empty.");
            }
    
            // Find or create the category
            Integer categoryId = categoriesRepository.findCategoryIdByAccountIdAndCategoryName(accountId, categoryName);
            if (categoryId == null) {
                categoryId = categoriesRepository.insertCategory(accountId, categoryName, null, 1, 1);
            }
    
            for (Map.Entry<String, String> assetEntry : assets.entrySet()) {
                String assetName = assetEntry.getKey();
                String subcategoryName = assetEntry.getValue();
    
                // Validate asset name
                if (assetName == null || assetName.trim().isEmpty()) {
                    throw new IllegalArgumentException("Asset name cannot be null or empty.");
                }
    
                // Find or create the subcategory (if provided)
                Integer subcategoryId = null;
                if (subcategoryName != null && !subcategoryName.trim().isEmpty()) {
                    subcategoryId = categoriesRepository.findCategoryIdByAccountIdAndCategoryName(accountId, subcategoryName);
                    if (subcategoryId == null) {
                        subcategoryId = categoriesRepository.insertCategory(accountId, subcategoryName, categoryId, 2, 1);
                    }
                }
    
                // Update the holdings_categories table with category and subcategory
                holdingsCategoriesRepository.upsertHoldingCategory(
                    accountId,
                    assetName,
                    subcategoryId != null ? subcategoryId : categoryId,
                    categoryName,
                    subcategoryName
                );
            }
        }
    }

    @Transactional
    public void addHoldingsCategories(UUID accountId, Map<String, Map<String, String>> holdingsCategories) {
        System.out.println("Received holdingsCategories: " + holdingsCategories);
    
        for (Map.Entry<String, Map<String, String>> categoryEntry : holdingsCategories.entrySet()) {
            String categoryName = categoryEntry.getKey();
            Map<String, String> assets = categoryEntry.getValue();
    
            // Validate category name
            if (categoryName == null || categoryName.trim().isEmpty()) {
                throw new IllegalArgumentException("Category name cannot be null or empty.");
            }
    
            // Find or create the category
            Integer categoryId = categoriesRepository.findCategoryIdByAccountIdAndCategoryName(accountId, categoryName);
            if (categoryId == null) {
                categoryId = categoriesRepository.insertCategory(accountId, categoryName, null, 1, 1);
            }
    
            for (Map.Entry<String, String> assetEntry : assets.entrySet()) {
                String assetName = assetEntry.getKey();
                String subcategoryName = assetEntry.getValue();
    
                // Validate asset name
                if (assetName == null || assetName.trim().isEmpty()) {
                    throw new IllegalArgumentException("Asset name cannot be null or empty.");
                }
    
                // Find or create the subcategory (if provided)
                Integer subcategoryId = null;
                if (subcategoryName != null && !subcategoryName.trim().isEmpty()) {
                    subcategoryId = categoriesRepository.findCategoryIdByAccountIdAndCategoryName(accountId, subcategoryName);
                    if (subcategoryId == null) {
                        subcategoryId = categoriesRepository.insertCategory(accountId, subcategoryName, categoryId, 2, 1);
                    }
                }
    
                // Insert the new holding into the holdings_categories table
                holdingsCategoriesRepository.insertHoldingCategory(
                    accountId,
                    assetName,
                    subcategoryId != null ? subcategoryId : categoryId,
                    categoryName,
                    subcategoryName
                );
            }
        }
    }

    @Transactional
    public void removeHoldingsCategory(UUID accountId, String category) {
        // Validate input
        if (category == null || category.trim().isEmpty()) {
            throw new IllegalArgumentException("Category name cannot be null or empty.");
        }
    
        // Remove the holdings category from the database
        holdingsCategoriesRepository.deleteByAccountIdAndCategory(accountId, category);
    }

    @Transactional(readOnly = true)
    public Map<String, Map<String, String>> fetchHoldingsCategories(UUID accountId) {
        // Fetch holdings categories from the repository
        List<Map<String, Object>> holdings = holdingsCategoriesRepository.findHoldingsByAccountId(accountId);
    
        // Prepare the response map
        Map<String, Map<String, String>> response = new LinkedHashMap<>();
    
        for (Map<String, Object> holding : holdings) {
            String category = (String) holding.get("category");
            String assetName = (String) holding.get("asset_name");
            String subcategory = (String) holding.get("subcategory");
    
            // Group by category
            response.computeIfAbsent(category, k -> new LinkedHashMap<>()).put(assetName, subcategory);
        }
    
        return response;
    } 
}
