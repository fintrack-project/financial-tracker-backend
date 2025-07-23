package com.fintrack.controller.finance;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.MediaType;
import com.fintrack.common.ApiResponse;
import com.fintrack.common.ResponseWrapper;
import com.fintrack.service.finance.CategoriesService;
import com.fintrack.service.finance.HoldingsCategoriesService;
import com.fintrack.constants.Color;

import java.util.*;

@RestController
@RequestMapping(value = "/api/categories", produces = MediaType.APPLICATION_JSON_VALUE)
public class CategoriesController {

    private final CategoriesService categoriesService;
    private final HoldingsCategoriesService holdingsCategoriesService;

    public CategoriesController(CategoriesService categoriesService, HoldingsCategoriesService holdingsCategoriesService) {
        this.categoriesService = categoriesService;
        this.holdingsCategoriesService = holdingsCategoriesService;
    }

    @PostMapping("/add")
    public ResponseEntity<ApiResponse<Void>> addCategory(
        @RequestBody Map<String, Object> categoryData) {
        try {
            UUID accountId = UUID.fromString((String) categoryData.get("accountId"));
            String categoryName = (String) categoryData.get("category_name");
            String hexCode = (String) categoryData.get("color");
        
            categoriesService.addCategory(accountId, categoryName, hexCode);
            return ResponseWrapper.ok(null, "Category added successfully.");
        } catch (Exception e) {
            return ResponseWrapper.badRequest(e.getMessage());
        }
    }

    @PostMapping("/name/update")
    public ResponseEntity<ApiResponse<Void>> updateCategoryName(@RequestBody Map<String, Object> categoryData) {
        try {
            UUID accountId = UUID.fromString((String) categoryData.get("accountId"));
            String oldCategoryName = (String) categoryData.get("old_category_name");
            String newCategoryName = (String) categoryData.get("new_category_name");
        
            categoriesService.updateCategoryName(accountId, oldCategoryName, newCategoryName);
            return ResponseWrapper.ok(null, "Category name updated successfully.");
        } catch (Exception e) {
            return ResponseWrapper.badRequest(e.getMessage());
        }
    }

    @DeleteMapping("/remove")
    public ResponseEntity<ApiResponse<Void>> removeCategory(
            @RequestParam(name = "accountId") UUID accountId,
            @RequestParam(name = "category") String category) {
        try {
            categoriesService.removeCategory(accountId, category);
            return ResponseWrapper.ok(null, "Category and its subcategories removed successfully.");
        } catch (Exception e) {
            return ResponseWrapper.badRequest(e.getMessage());
        }
    }

    @GetMapping("/fetch/color-map")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getCategoryColorMap(
        @RequestParam(name = "accountId") UUID accountId) {
        try {
            Map<String, Object> response = categoriesService.getCategoryColorMap(accountId);
            return ResponseWrapper.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseWrapper.badRequest(e.getMessage());
        } catch (Exception e) {
            return ResponseWrapper.badRequest(e.getMessage());
        }
    }

    @GetMapping("/fetch/names-map")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getCategoriesAndSubcategoriesNamesMap(
        @RequestParam(name = "accountId") UUID accountId) {
        try {
            Map<String, Object> response = categoriesService.getCategoriesAndSubcategoriesNamesMap(accountId);
            return ResponseWrapper.ok(response);
        } catch (Exception e) {
            return ResponseWrapper.badRequest(e.getMessage());
        }
    }

    @GetMapping("/fetch/category-names")
    public ResponseEntity<ApiResponse<List<String>>> getCategoryNames(
        @RequestParam(name = "accountId") UUID accountId) {
        try {
            List<String> categoryNames = categoriesService.getCategoryNames(accountId);
            return ResponseWrapper.ok(categoryNames);
        } catch (Exception e) {
            return ResponseWrapper.badRequest(e.getMessage());
        }
    }

    @PostMapping("/color/update")
    public ResponseEntity<ApiResponse<Void>> updateCategoryColor(@RequestBody Map<String, Object> categoryData) {
        try {
            UUID accountId = UUID.fromString((String) categoryData.get("accountId"));
            String categoryName = (String) categoryData.get("category_name");
            String hexCode = (String) categoryData.get("color");
            
            categoriesService.updateCategoryColor(accountId, categoryName, hexCode);
            return ResponseWrapper.ok(null, "Category color updated successfully.");
        } catch (IllegalArgumentException e) {
            return ResponseWrapper.badRequest(e.getMessage());
        } catch (Exception e) {
            return ResponseWrapper.badRequest(e.getMessage());
        }
    }

    @PostMapping("/cleanup/orphaned")
    public ResponseEntity<ApiResponse<Void>> cleanupOrphanedHoldingsCategories(@RequestBody Map<String, Object> request) {
        try {
            UUID accountId = UUID.fromString((String) request.get("accountId"));
            
            holdingsCategoriesService.cleanupOrphanedHoldingsCategories(accountId);
            return ResponseWrapper.ok(null, "Orphaned holdings categories cleaned up successfully.");
        } catch (Exception e) {
            return ResponseWrapper.badRequest(e.getMessage());
        }
    }

    @PostMapping("/cleanup/orphaned/assets")
    public ResponseEntity<ApiResponse<Void>> cleanupOrphanedHoldingsCategoriesForAssets(@RequestBody Map<String, Object> request) {
        try {
            UUID accountId = UUID.fromString((String) request.get("accountId"));
            @SuppressWarnings("unchecked")
            List<String> assetNames = (List<String>) request.get("assetNames");
            
            holdingsCategoriesService.cleanupOrphanedHoldingsCategoriesForAssets(accountId, assetNames);
            return ResponseWrapper.ok(null, "Orphaned holdings categories for specified assets cleaned up successfully.");
        } catch (Exception e) {
            return ResponseWrapper.badRequest(e.getMessage());
        }
    }
}