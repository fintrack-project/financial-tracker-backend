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

    @PostMapping("/subcategories/update")
    public ResponseEntity<String> updateSubcategoriesByCategoryName(
      @RequestBody Map<String, Object> subcategoryData,
      @RequestParam UUID accountId) {
        categoriesService.updateSubcategoriesByCategoryName(accountId, subcategoryData);
        return ResponseEntity.ok("Subcategories updated successfully.");
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