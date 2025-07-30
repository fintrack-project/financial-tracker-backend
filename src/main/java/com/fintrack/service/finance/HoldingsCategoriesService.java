package com.fintrack.service.finance;

import com.fintrack.model.finance.Category;
import com.fintrack.repository.finance.CategoriesRepository;
import com.fintrack.repository.finance.HoldingsCategoriesRepository;
import com.fintrack.repository.finance.SubcategoriesRepository;
import com.fintrack.repository.finance.AssetRepository;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
public class HoldingsCategoriesService {

    private static final Logger logger = LogManager.getLogger(HoldingsCategoriesService.class);

    private final HoldingsCategoriesRepository holdingsCategoriesRepository;
    private final CategoriesRepository categoriesRepository;
    private final SubcategoriesRepository subcategoriesRepository;
    private final AssetRepository assetRepository;

    public HoldingsCategoriesService(
        HoldingsCategoriesRepository holdingsCategoriesRepository, 
        CategoriesRepository categoriesRepository,
        SubcategoriesRepository subcategoriesRepository,
        AssetRepository assetRepository) {
        this.holdingsCategoriesRepository = holdingsCategoriesRepository;
        this.categoriesRepository = categoriesRepository;
        this.subcategoriesRepository = subcategoriesRepository;
        this.assetRepository = assetRepository;
    }

    @Transactional
    public void updateHoldingsCategories(UUID accountId, Map<String, Map<String, String>> holdingsCategories) {
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
    
                // Handle subcategory
                Integer subcategoryId = null;
                if (subcategoryName != null && !subcategoryName.trim().isEmpty()) {
                    subcategoryId = categoriesRepository.findCategoryIdByAccountIdAndCategoryName(accountId, subcategoryName);
                    if (subcategoryId == null) {
                        subcategoryId = categoriesRepository.insertCategory(accountId, subcategoryName, categoryId, 2, 1);
                    }
                }
    
                // Update the holdings_categories table with category and subcategory
                // If subcategoryName is null, it will set the subcategory to null in the database
                holdingsCategoriesRepository.upsertHoldingCategory(
                    accountId,
                    assetName,
                    subcategoryId != null ? subcategoryId : categoryId,
                    categoryName,
                    subcategoryName  // This will be null if subcategoryName is null
                );
            }
        }
    }

    @Transactional
    public void addHoldingsCategories(UUID accountId, Map<String, Map<String, String>> holdingsCategories) {
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
                // Fetch asset names for each subcategory
                List<Map<String, Object>> assetNamesSubcategoryMap = holdingsCategoriesRepository.findHoldingsByCategoryId(accountId, subcategory.getCategoryId());

                for (Map<String, Object> assetNamesToSubcategory : assetNamesSubcategoryMap) {
                    String assetName = (String) assetNamesToSubcategory.get("asset_name");
                    String subcategoryName = (String) assetNamesToSubcategory.get("subcategory");

                    // Add asset and subcategory to the subcategory map
                    subcategoryMap.put(assetName, subcategoryName);
                }
            }

            // Add the category and its subcategories to the response map
            response.put(category.getCategoryName(), subcategoryMap);
        }

        return response;
    }

    /**
     * Clean up orphaned holdings categories for assets that no longer exist
     * This method removes holdings_categories entries for assets that have been deleted
     */
    @Transactional
    public void cleanupOrphanedHoldingsCategories(UUID accountId) {
        logger.info("Cleaning up orphaned holdings categories for account: {}", accountId);
        
        // Remove holdings_categories entries for assets that no longer exist in the asset table
        int deletedCount = holdingsCategoriesRepository.deleteOrphanedHoldingsCategories(accountId);
        
        if (deletedCount > 0) {
            logger.info("Cleaned up {} orphaned holdings categories for account: {}", deletedCount, accountId);
        } else {
            logger.info("No orphaned holdings categories found for account: {}", accountId);
        }
    }

    /**
     * Clean up orphaned holdings categories for specific assets
     * This method removes holdings_categories entries for specific assets that have been deleted
     */
    @Transactional
    public void cleanupOrphanedHoldingsCategoriesForAssets(UUID accountId, List<String> assetNames) {
        if (assetNames == null || assetNames.isEmpty()) {
            logger.info("No asset names provided for cleanup for account: {}", accountId);
            return;
        }
        
        logger.info("Cleaning up orphaned holdings categories for account: {} and assets: {}", accountId, assetNames);
        
        // Remove holdings_categories entries for specific assets that no longer exist
        int deletedCount = holdingsCategoriesRepository.deleteOrphanedHoldingsCategoriesForAssets(accountId, assetNames);
        
        if (deletedCount > 0) {
            logger.info("Cleaned up {} orphaned holdings categories for account: {} and assets: {}", deletedCount, accountId, assetNames);
        } else {
            logger.info("No orphaned holdings categories found for account: {} and assets: {}", accountId, assetNames);
        }
    }

    /**
     * Clean up orphaned assets that no longer have any transactions
     * This method removes asset entries for assets that have no active transactions
     */
    @Transactional
    public void cleanupOrphanedAssets(UUID accountId) {
        logger.info("Cleaning up orphaned assets for account: {}", accountId);
        
        // Remove asset entries for assets that no longer have any active transactions
        int deletedCount = assetRepository.deleteOrphanedAssets(accountId);
        
        if (deletedCount > 0) {
            logger.info("Cleaned up {} orphaned assets for account: {}", deletedCount, accountId);
        } else {
            logger.info("No orphaned assets found for account: {}", accountId);
        }
    }
}
