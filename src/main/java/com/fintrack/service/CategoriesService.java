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
            } else {
                // Delete existing subcategories for the category
                categoriesRepository.deleteByParentId(accountId, categoryId);
            }
    
            // Insert the new subcategories
            if (subcategories != null && !subcategories.isEmpty()) {
                for (int priority = 1; priority <= subcategories.size(); priority++) {
                    String subcategoryName = subcategories.get(priority - 1);
                    categoriesRepository.insertCategory(accountId, subcategoryName, categoryId, 2, priority);
                }
            }
        }
    }

    @Transactional
    public void updateHoldingsCategories(UUID accountId, List<Map<String, Object>> holdingsCategories) {
        // Update holdings categories
        for (Map<String, Object> holdingCategory : holdingsCategories) {
            String assetName = (String) holdingCategory.get("asset_name");
            Integer categoryId = (Integer) holdingCategory.get("category_id");

            holdingsCategoriesRepository.upsertHoldingCategory(accountId, assetName, categoryId);
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
        if (subcategories == null || subcategories.isEmpty()) {
            throw new IllegalArgumentException("Subcategories cannot be null or empty.");
        }
    
        // Find the categoryId for the given category_name
        Integer categoryId = categoriesRepository.findCategoryIdByAccountIdAndCategoryName(accountId, categoryName);
        if (categoryId == null) {
            throw new IllegalArgumentException("Category with name '" + categoryName + "' does not exist.");
        }
    
        // Delete existing subcategories for the parent category
        categoriesRepository.deleteByParentId(accountId, categoryId);
    
        // Insert the new subcategories
        for (int priority = 1; priority <= subcategories.size(); priority++) {
            String subcategoryName = subcategories.get(priority - 1);
            categoriesRepository.insertCategory(accountId, subcategoryName, categoryId, 2, priority);
        }
    }

    @Transactional
    public void deleteSubcategory(UUID accountId, String subcategoryName, Integer parentId) {
        // Validate input
        if (subcategoryName == null || subcategoryName.trim().isEmpty()) {
            throw new IllegalArgumentException("Subcategory name cannot be null or empty.");
        }
        if (parentId == null) {
            throw new IllegalArgumentException("Parent ID cannot be null.");
        }

        // Find the subcategory to delete
        Integer subcategoryId = categoriesRepository.findCategoryIdByAccountIdAndCategoryName(accountId, subcategoryName);
        if (subcategoryId == null) {
            throw new IllegalArgumentException("Subcategory with name '" + subcategoryName + "' does not exist.");
        }

        // Delete the subcategory
        categoriesRepository.deleteById(subcategoryId);

        // Reorder the remaining subcategories
        List<Category> remainingSubcategories = categoriesRepository.findSubcategoriesByParentId(accountId, parentId);
        int priority = 1;
        for (Category subcategory : remainingSubcategories) {
            categoriesRepository.updateSubcategoryPriority(subcategory.getCategoryId(), priority);
            priority++;
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