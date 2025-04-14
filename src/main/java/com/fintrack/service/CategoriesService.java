package com.fintrack.service;

import com.fintrack.repository.CategoriesRepository;
import com.fintrack.repository.HoldingsCategoriesRepository;
import com.fintrack.model.Category;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class CategoriesService {

    private final CategoriesRepository categoriesRepository;
    private final HoldingsCategoriesRepository holdingsCategoriesRepository;

    public CategoriesService(CategoriesRepository categoriesRepository, HoldingsCategoriesRepository holdingsCategoriesRepository) {
        this.categoriesRepository = categoriesRepository;
        this.holdingsCategoriesRepository = holdingsCategoriesRepository;
    }

    @Transactional
    public void updateCategoriesWithSubcategories(UUID accountId, List<Map<String, Object>> categories) {
        for (Map<String, Object> categoryData : categories) {
            // Extract category_name and subcategories
            String categoryName = (String) categoryData.get("category_name");
            List<String> subcategories = (List<String>) categoryData.get("subcategories");
    
            // Validate input
            if (categoryName == null || categoryName.trim().isEmpty()) {
                throw new IllegalArgumentException("Category name cannot be null or empty.");
            }
    
            // Find or create the category
            Integer categoryId = categoriesRepository.findCategoryIdByAccountIdAndCategoryName(accountId, categoryName);
            if (categoryId == null) {
                // Insert the category if it doesn't exist
                categoryId = categoriesRepository.insertCategory(accountId, categoryName, null, 1, 1);
            }

            // Handle subcategories
            if (subcategories == null || subcategories.isEmpty()) {
                // If subcategories is empty, delete all subcategories for the category but keep the category
                categoriesRepository.deleteByParentId(accountId, categoryId);
            } else {
                // Delete existing subcategories for the category
                categoriesRepository.deleteByParentId(accountId, categoryId);

                // Insert the new subcategories
                for (int priority = 1; priority <= subcategories.size(); priority++) {
                    String subcategoryName = subcategories.get(priority - 1);
                    categoriesRepository.insertCategory(accountId, subcategoryName, categoryId, 2, priority);
                }
            }
        }
    }

    @Transactional
    public void updateHoldingsCategories(UUID accountId, List<Map<String, Object>> holdingsData) {

        System.out.println("Received holdingsData: " + holdingsData);

        for (Map<String, Object> holding : holdingsData) {
            // Extract asset_name, category, and subcategory
            String assetName = (String) holding.get("asset_name");
            String categoryName = (String) holding.get("category");
            String subcategoryName = (String) holding.get("subcategory");

            System.out.println("Processing holding: " + holding);
    
            // Validate asset_name and category
            if (assetName == null || assetName.trim().isEmpty()) {
                throw new IllegalArgumentException("Asset name cannot be null or empty.");
            }
            if (categoryName == null || categoryName.trim().isEmpty()) {
                throw new IllegalArgumentException("Category name cannot be null or empty.");
            }
    
            // Find or create the category
            Integer categoryId = categoriesRepository.findCategoryIdByAccountIdAndCategoryName(accountId, categoryName);
            if (categoryId == null) {
                categoryId = categoriesRepository.insertCategory(accountId, categoryName, null, 1, 1);
            }
    
            // Find or create the subcategory (if provided)
            Integer subcategoryId = null;
            if (subcategoryName != null && !subcategoryName.trim().isEmpty()) {
                subcategoryId = categoriesRepository.findCategoryIdByAccountIdAndCategoryName(accountId, subcategoryName);
                if (subcategoryId == null) {
                    subcategoryId = categoriesRepository.insertCategory(accountId, subcategoryName, categoryId, 2, 1);
                }
            }
    
            // Update the holdings_categories table
            holdingsCategoriesRepository.upsertHoldingCategory(accountId, assetName, subcategoryId != null ? subcategoryId : categoryId);
        }
    }

    @Transactional
    public void updateSubcategoriesByCategoryName(UUID accountId, Map<String, Object> subcategoryData) {
        // Extract category_name and subcategories from the request
        String categoryName = (String) subcategoryData.get("category_name");
        List<String> subcategories = (List<String>) subcategoryData.get("subcategories");

        // Validate input
        if (categoryName == null || categoryName.trim().isEmpty()) {
            throw new IllegalArgumentException("Category name cannot be null or empty.");
        }
    
        // Find the categoryId for the given category_name
        Integer categoryId = categoriesRepository.findCategoryIdByAccountIdAndCategoryName(accountId, categoryName);
        if (categoryId == null) {
            throw new IllegalArgumentException("Category with name '" + categoryName + "' does not exist.");
        }

        if (subcategories == null || subcategories.isEmpty()) {
            categoriesRepository.deleteByParentId(accountId, categoryId);
            return;
        }
    
        // Delete existing subcategories for the parent category
        categoriesRepository.deleteByParentId(accountId, categoryId);
    
        // Insert the new subcategories
        for (int priority = 1; priority <= subcategories.size(); priority++) {
            String subcategoryName = subcategories.get(priority - 1);
            categoriesRepository.insertCategory(accountId, subcategoryName, categoryId, 2, priority);
        }
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getCategoriesAndSubcategories(UUID accountId) {
        // Fetch all top-level categories (parent_id is NULL)
        List<Category> categories = categoriesRepository.findCategoriesByAccountId(accountId);
    
        // Prepare the response
        Map<String, List<String>> subcategoriesMap = new HashMap<>();
    
        for (Category category : categories) {
            // Fetch subcategories for each category
            List<Category> subcategories = categoriesRepository.findSubcategoriesByParentId(accountId, category.getCategoryId());
    
            // Format subcategories as a list of names
            List<String> subcategoryNames = subcategories.stream()
                    .map(Category::getCategoryName)
                    .collect(Collectors.toList());
    
            // Add to the subcategories map
            subcategoriesMap.put(category.getCategoryName(), subcategoryNames);
        }

        // Format the top-level categories as a list of names
        List<String> categoryNames = categories.stream()
        .map(Category::getCategoryName)
        .collect(Collectors.toList());
    
        // Return the response
        Map<String, Object> response = new HashMap<>();
        response.put("categories", categoryNames);
        response.put("subcategories", subcategoriesMap);
        return response;
    }
}