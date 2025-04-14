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

    @PostMapping("/update")
    public ResponseEntity<String> updateCategoriesWithSubcategories(
      @RequestBody List<Map<String, Object>> categories, 
      @RequestParam UUID accountId) {
        categoriesService.updateCategoriesWithSubcategories(accountId, categories);
        return ResponseEntity.ok("Categories updated successfully.");
    }

    @PostMapping("/holdings/update")
    public ResponseEntity<String> updateHoldingsCategories(
      @RequestBody List<Map<String, Object>> holdingsCategories, 
      @RequestParam UUID accountId) {
        categoriesService.updateHoldingsCategories(accountId, holdingsCategories);
        return ResponseEntity.ok("Holdings categories updated successfully.");
    }

    @PostMapping("/subcategories/update")
    public ResponseEntity<String> updateSubcategoriesByCategoryName(
      @RequestBody Map<String, Object> subcategoryData,
      @RequestParam UUID accountId) {
        categoriesService.updateSubcategoriesByCategoryName(accountId, subcategoryData);
        return ResponseEntity.ok("Subcategories updated successfully.");
    }
}