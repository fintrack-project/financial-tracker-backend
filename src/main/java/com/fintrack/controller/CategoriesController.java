package com.fintrack.controller;

import com.fintrack.service.CategoriesService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/categories")
public class CategoriesController {

    private final CategoriesService categoriesService;

    public CategoriesController(CategoriesService categoriesService) {
        this.categoriesService = categoriesService;
    }

    @PostMapping("/add")
    public ResponseEntity<String> addCategory(
        @RequestBody Map<String, Object> categoryData) {
        UUID accountId = UUID.fromString((String) categoryData.get("accountId"));
        String categoryName = (String) categoryData.get("category_name");
    
        categoriesService.addCategory(accountId, categoryName);
        return ResponseEntity.ok("Category added successfully.");
    }

    @PostMapping("/name/update")
    public ResponseEntity<String> updateCategoryName(@RequestBody Map<String, Object> categoryData) {
        UUID accountId = UUID.fromString((String) categoryData.get("accountId"));
        String oldCategoryName = (String) categoryData.get("old_category_name");
        String newCategoryName = (String) categoryData.get("new_category_name");
    
        categoriesService.updateCategoryName(accountId, oldCategoryName, newCategoryName);
        return ResponseEntity.ok("Category name updated successfully.");
    }

    @PostMapping("/subcategories/add")
    public ResponseEntity<String> addSubcategory(@RequestBody Map<String, Object> subcategoryData) {
        UUID accountId = UUID.fromString((String) subcategoryData.get("accountId"));
        String categoryName = (String) subcategoryData.get("category_name");
        String subcategoryName = (String) subcategoryData.get("subcategory_name");
    
        categoriesService.addSubcategory(accountId, categoryName, subcategoryName);
        return ResponseEntity.ok("Subcategory added successfully.");
    }

    @PostMapping("/subcategories/name/update")
    public ResponseEntity<String> updateSubcategoryName(@RequestBody Map<String, Object> subcategoryData) {
        UUID accountId = UUID.fromString((String) subcategoryData.get("accountId"));
        String categoryName = (String) subcategoryData.get("category_name");
        String oldSubcategoryName = (String) subcategoryData.get("old_subcategory_name");
        String newSubcategoryName = (String) subcategoryData.get("new_subcategory_name");
    
        categoriesService.updateSubcategoryName(accountId, categoryName, oldSubcategoryName, newSubcategoryName);
        return ResponseEntity.ok("Subcategory name updated successfully.");
    }

    @DeleteMapping("/remove")
    public ResponseEntity<String> removeCategory(
            @RequestParam UUID accountId,
            @RequestParam String category) {
        categoriesService.removeCategory(accountId, category);
        return ResponseEntity.ok("Category and its subcategories removed successfully.");
    }
    
    @DeleteMapping("/subcategories/remove")
    public ResponseEntity<String> removeSubcategory(
            @RequestParam UUID accountId,
            @RequestParam String category,
            @RequestParam String subcategory) {
        categoriesService.removeSubcategory(accountId, category, subcategory);
        return ResponseEntity.ok("Subcategory removed successfully.");
    }

    @GetMapping("/fetch")
    public ResponseEntity<Map<String, Object>> getCategoriesAndSubcategories(@RequestParam UUID accountId) {
        Map<String, Object> response = categoriesService.getCategoriesAndSubcategories(accountId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/holdings/update")
    public ResponseEntity<String> updateHoldingsCategories(
        @RequestParam UUID accountId,
        @RequestBody List<Map<String, Object>> holdingsData
    ) {
        categoriesService.updateHoldingsCategories(accountId, holdingsData);
        return ResponseEntity.ok("Holdings categories updated successfully.");
    }
}