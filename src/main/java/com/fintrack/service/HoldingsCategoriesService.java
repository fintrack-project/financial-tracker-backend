package com.fintrack.service;

import com.fintrack.repository.HoldingsCategoriesRepository;
import com.fintrack.repository.CategoriesRepository;
import com.fintrack.repository.SubcategoriesRepository;
import com.fintrack.model.Category;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
public class HoldingsCategoriesService {

    private final HoldingsCategoriesRepository holdingsCategoriesRepository;
    private final CategoriesRepository categoriesRepository;
    private final SubcategoriesRepository subcategoriesRepository;

    public HoldingsCategoriesService(
        HoldingsCategoriesRepository holdingsCategoriesRepository, 
        CategoriesRepository categoriesRepository,
        SubcategoriesRepository subcategoriesRepository) {
        this.holdingsCategoriesRepository = holdingsCategoriesRepository;
        this.categoriesRepository = categoriesRepository;
        this.subcategoriesRepository = subcategoriesRepository;
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
        // Fetch all top-level categories ordered by priority
        List<Category> categories = categoriesRepository.findCategoriesByAccountIdOrderedByPriority(accountId);

        // Prepare the response map using LinkedHashMap to preserve order
        Map<String, Map<String, String>> response = new LinkedHashMap<>();

        for (Category category : categories) {
            // Fetch subcategories for each category ordered by priority
            List<Category> subcategories = subcategoriesRepository.findSubcategoriesByParentId(accountId, category.getCategoryId());

            // Prepare a LinkedHashMap for subcategories to preserve order
            Map<String, String> subcategoryMap = new LinkedHashMap<>();

            for (Category subcategory : subcategories) {
                // Fetch holdings for each subcategory
                List<Map<String, Object>> holdings = holdingsCategoriesRepository.findHoldingsByCategoryId(accountId, subcategory.getCategoryId());

                for (Map<String, Object> holding : holdings) {
                    String assetName = (String) holding.get("asset_name");
                    String subcategoryName = (String) holding.get("subcategory");

                    // Add asset and subcategory to the subcategory map
                    subcategoryMap.put(assetName, subcategoryName);
                }
            }

            // Add the category and its subcategories to the response map
            response.put(category.getCategoryName(), subcategoryMap);
        }

        return response;
    } 
}
