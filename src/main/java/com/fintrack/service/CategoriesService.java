package com.fintrack.service;

import com.fintrack.repository.CategoriesRepository;
import com.fintrack.repository.SubcategoriesRepository;
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
    private final SubcategoriesRepository subcategoriesRepository;
    private final HoldingsCategoriesRepository holdingsCategoriesRepository;

    public CategoriesService(
        CategoriesRepository categoriesRepository, 
        SubcategoriesRepository subcategoriesRepository,
        HoldingsCategoriesRepository holdingsCategoriesRepository) {
        this.categoriesRepository = categoriesRepository;
        this.subcategoriesRepository = subcategoriesRepository;
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

    @Transactional(readOnly = true)
    public Map<String, Object> getCategoriesAndSubcategories(UUID accountId) {
        // Fetch all top-level categories (parent_id is NULL)
        List<Category> categories = categoriesRepository.findCategoriesByAccountId(accountId);
    
        // Prepare the response
        Map<String, List<String>> subcategoriesMap = new LinkedHashMap<>();
    
        for (Category category : categories) {
            // Fetch subcategories for each category
            List<Category> subcategories = subcategoriesRepository.findSubcategoriesByParentId(accountId, category.getCategoryId());
    
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
        Map<String, Object> response = new LinkedHashMap<>();
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
        // Remove all related entries in the holdings_categories table
        holdingsCategoriesRepository.deleteByAccountIdAndCategoryId(accountId, categoryId);

        // Delete all subcategories for the category
        subcategoriesRepository.deleteByParentId(accountId, categoryId);
    
        // Delete the category itself
        categoriesRepository.deleteByCategoryId(categoryId);

        // Fetch remaining categories and reassign their priorities
        List<Category> remainingCategories = categoriesRepository.findCategoriesByAccountIdOrderedByPriority(accountId);
        for (int priority = 1; priority <= remainingCategories.size(); priority++) {
            Category category = remainingCategories.get(priority - 1);
            categoriesRepository.updateCategoryPriority(category.getCategoryId(), priority);
        }
    }
}