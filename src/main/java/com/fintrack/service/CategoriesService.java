package com.fintrack.service;

import com.fintrack.repository.CategoriesRepository;
import com.fintrack.repository.HoldingsCategoriesRepository;

import jakarta.transaction.Transactional;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class CategoriesService {

    private final CategoriesRepository categoriesRepository;
    private final HoldingsCategoriesRepository holdingsCategoriesRepository;

    public CategoriesService(CategoriesRepository categoriesRepository, HoldingsCategoriesRepository holdingsCategoriesRepository) {
        this.categoriesRepository = categoriesRepository;
        this.holdingsCategoriesRepository = holdingsCategoriesRepository;
    }

    @Transactional
    public void updateCategories(UUID accountId, List<Map<String, Object>> categories) {
        // Delete existing categories for the account
        categoriesRepository.deleteByAccountId(accountId);

        // Insert new categories recursively
        insertCategories(accountId, categories, null, 1);
    }

    private void insertCategories(UUID accountId, List<Map<String, Object>> categories, Integer parentId, int level) {
        int priority = 1; // Start priority from 1
        for (Map<String, Object> category : categories) {
            String categoryName = (String) category.get("category_name");
            List<Map<String, Object>> subcategories = (List<Map<String, Object>>) category.get("subcategories");

            // Insert the category
            Integer categoryId = categoriesRepository.insertCategory(accountId, categoryName, parentId, level, priority);

            // Recursively insert subcategories
            if (subcategories != null && !subcategories.isEmpty()) {
                insertCategories(accountId, subcategories, categoryId, level + 1);
            }

            priority++; // Increment priority for the next category
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
}