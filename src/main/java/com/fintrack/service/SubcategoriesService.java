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
public class SubcategoriesService {

    private final CategoriesRepository categoriesRepository;

    public SubcategoriesService(CategoriesRepository categoriesRepository, HoldingsCategoriesRepository holdingsCategoriesRepository) {
        this.categoriesRepository = categoriesRepository;
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
}