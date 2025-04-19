package com.fintrack.controller;

import com.fintrack.service.CategoriesService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

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

    @DeleteMapping("/remove")
    public ResponseEntity<String> removeCategory(
            @RequestParam UUID accountId,
            @RequestParam String category) {
        categoriesService.removeCategory(accountId, category);
        return ResponseEntity.ok("Category and its subcategories removed successfully.");
    }

    @GetMapping("/fetch")
    public ResponseEntity<Map<String, Object>> getCategoriesAndSubcategories(@RequestParam UUID accountId) {
        Map<String, Object> response = categoriesService.getCategoriesAndSubcategories(accountId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/fetch/names")
    public ResponseEntity<List<String>> getCategoryNames(@RequestParam UUID accountId) {
        List<String> categoryNames = categoriesService.getCategoryNames(accountId);
        return ResponseEntity.ok(categoryNames);
    }
}