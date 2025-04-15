package com.fintrack.service;

import com.fintrack.repository.CategoriesRepository;
import com.fintrack.repository.HoldingsCategoriesRepository;
import com.fintrack.model.Category;

import org.springframework.dao.DataIntegrityViolationException;
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
    public void addCategory(UUID accountId, String categoryName) {
        // Validate input
        if (categoryName == null || categoryName.trim().isEmpty()) {
            throw new IllegalArgumentException("Category name cannot be null or empty.");
        }
    
        // Check if the category already exists
        Integer existingCategoryId = categoriesRepository.findCategoryIdByAccountIdAndCategoryName(accountId, categoryName);

        if (existingCategoryId != null) {
            throw new IllegalArgumentException("Category with name '" + categoryName + "' already exists.");
        }

        // Dynamically calculate the priority if not provided
        Integer maxPriority = categoriesRepository.findMaxPriorityByAccountId(accountId);
        Integer priority = (maxPriority != null ? maxPriority : 0) + 1;
    
        // Insert the new category
        try {
            categoriesRepository.insertCategory(accountId, categoryName, null, 1, priority);
        } catch (DataIntegrityViolationException e) {
            throw new IllegalArgumentException("Failed to insert category. Possible data integrity violation.", e);
        }
    }

    @Transactional
    public void addSubcategory(UUID accountId, String categoryName, String subcategoryName) {
        // Validate input
        if (subcategoryName == null || subcategoryName.trim().isEmpty()) {
            throw new IllegalArgumentException("Subcategory name cannot be null or empty.");
        }
    
        // Trim the subcategory name
        String trimmedSubcategoryName = subcategoryName.trim();
    
        // Check if the subcategory already exists
        Integer existingSubcategoryId = categoriesRepository.findSubcategoryIdByAccountIdAndCategoryNameAndSubcategoryName(
            accountId, categoryName, trimmedSubcategoryName
        );
        if (existingSubcategoryId != null) {
            throw new IllegalArgumentException("Subcategory with name '" + trimmedSubcategoryName + "' already exists in category '" + categoryName + "'.");
        }
    
        // Dynamically calculate the priority for the subcategory
        Integer maxPriority = categoriesRepository.findMaxSubcategoryPriorityByAccountIdAndCategoryName(accountId, categoryName);
        Integer priority = (maxPriority != null ? maxPriority : 0) + 1;
    
        // Insert the new subcategory
        categoriesRepository.insertSubcategory(accountId, categoryName, trimmedSubcategoryName, priority);
    }

    @Transactional
    public void updateCategoryName(UUID accountId, String oldCategoryName, String newCategoryName) {
        // Validate input
        if (oldCategoryName == null || oldCategoryName.trim().isEmpty()) {
            throw new IllegalArgumentException("Old category name cannot be null or empty.");
        }
        if (newCategoryName == null || newCategoryName.trim().isEmpty()) {
            throw new IllegalArgumentException("New category name cannot be null or empty.");
        }
    
        // Find the category ID for the old category name
        Integer categoryId = categoriesRepository.findCategoryIdByAccountIdAndCategoryName(accountId, oldCategoryName);
        if (categoryId == null) {
            throw new IllegalArgumentException("Category with name '" + oldCategoryName + "' does not exist.");
        }
    
        // Update the category name
        categoriesRepository.updateCategoryName(accountId, categoryId, newCategoryName);
    }

    @Transactional
    public void updateSubcategoryName(UUID accountId, String categoryName, String oldSubcategoryName, String newSubcategoryName) {
        // Validate input
        if (oldSubcategoryName == null || oldSubcategoryName.trim().isEmpty()) {
            throw new IllegalArgumentException("Old subcategory name cannot be null or empty.");
        }
        if (newSubcategoryName == null || newSubcategoryName.trim().isEmpty()) {
            throw new IllegalArgumentException("New subcategory name cannot be null or empty.");
        }
    
        // Trim the subcategory names
        String trimmedOldSubcategoryName = oldSubcategoryName.trim();
        String trimmedNewSubcategoryName = newSubcategoryName.trim();
    
        // Find the parent category ID
        Integer parentCategoryId = categoriesRepository.findCategoryIdByAccountIdAndCategoryName(accountId, categoryName);
        if (parentCategoryId == null) {
            throw new IllegalArgumentException("Category with name '" + categoryName + "' does not exist.");
        }
    
        // Find the subcategory ID for the old subcategory name
        Integer subcategoryId = categoriesRepository.findSubcategoryIdByAccountIdAndCategoryNameAndSubcategoryName(
            accountId, categoryName, trimmedOldSubcategoryName
        );
        if (subcategoryId == null) {
            throw new IllegalArgumentException("Subcategory with name '" + trimmedOldSubcategoryName + "' does not exist in category '" + categoryName + "'.");
        }
    
        // Check if the new subcategory name already exists
        Integer duplicateSubcategoryId = categoriesRepository.findSubcategoryIdByAccountIdAndCategoryNameAndSubcategoryName(
            accountId, categoryName, trimmedNewSubcategoryName
        );
        if (duplicateSubcategoryId != null) {
            throw new IllegalArgumentException("Subcategory with name '" + trimmedNewSubcategoryName + "' already exists in category '" + categoryName + "'.");
        }
    
        // Update the subcategory name
        categoriesRepository.updateSubcategoryName(accountId, categoryName, trimmedOldSubcategoryName, trimmedNewSubcategoryName);
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

    @Transactional
    public void removeCategory(UUID accountId, String categoryName) {
        // Find the category ID for the given category name
        Integer categoryId = categoriesRepository.findCategoryIdByAccountIdAndCategoryName(accountId, categoryName);
        if (categoryId == null) {
            throw new IllegalArgumentException("Category with name '" + categoryName + "' does not exist.");
        }
    
        // Delete all subcategories for the category
        categoriesRepository.deleteByParentId(accountId, categoryId);
    
        // Delete the category itself
        categoriesRepository.deleteByCategoryId(categoryId);
    }

    @Transactional
    public void removeSubcategory(UUID accountId, String categoryName, String subcategoryName) {
        // Find the category ID for the given category name
        Integer categoryId = categoriesRepository.findCategoryIdByAccountIdAndCategoryName(accountId, categoryName);
        if (categoryId == null) {
            throw new IllegalArgumentException("Category with name '" + categoryName + "' does not exist.");
        }
    
        // Find the subcategory ID for the given subcategory name
        Integer subcategoryId = categoriesRepository.findCategoryIdByAccountIdAndCategoryName(accountId, subcategoryName);
        if (subcategoryId == null) {
            throw new IllegalArgumentException("Subcategory with name '" + subcategoryName + "' does not exist.");
        }
    
        // Delete the subcategory
        categoriesRepository.deleteByCategoryId(subcategoryId);
    
        // Fetch remaining subcategories and update their priorities
        List<Category> remainingSubcategories = categoriesRepository.findSubcategoriesByParentId(accountId, categoryId);
        for (int priority = 1; priority <= remainingSubcategories.size(); priority++) {
            Category subcategory = remainingSubcategories.get(priority - 1);
            categoriesRepository.updateSubcategoryPriority(subcategory.getCategoryId(), priority);
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
}