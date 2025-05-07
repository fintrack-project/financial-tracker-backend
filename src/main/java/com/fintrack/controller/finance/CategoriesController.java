package com.fintrack.controller.finance;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.MediaType;
import com.fintrack.common.ApiResponse;
import com.fintrack.service.finance.CategoriesService;

import java.util.*;

@RestController
@RequestMapping(value = "/api/categories", produces = MediaType.APPLICATION_JSON_VALUE)
public class CategoriesController {

    private final CategoriesService categoriesService;

    public CategoriesController(CategoriesService categoriesService) {
        this.categoriesService = categoriesService;
    }

    @PostMapping("/add")
    public ResponseEntity<ApiResponse<Void>> addCategory(
        @RequestBody Map<String, Object> categoryData) {
        try {
            UUID accountId = UUID.fromString((String) categoryData.get("accountId"));
            String categoryName = (String) categoryData.get("category_name");
        
            categoriesService.addCategory(accountId, categoryName);
            return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(ApiResponse.success(null, "Category added successfully."));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .contentType(MediaType.APPLICATION_JSON)
                .body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/name/update")
    public ResponseEntity<ApiResponse<Void>> updateCategoryName(@RequestBody Map<String, Object> categoryData) {
        try {
            UUID accountId = UUID.fromString((String) categoryData.get("accountId"));
            String oldCategoryName = (String) categoryData.get("old_category_name");
            String newCategoryName = (String) categoryData.get("new_category_name");
        
            categoriesService.updateCategoryName(accountId, oldCategoryName, newCategoryName);
            return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(ApiResponse.success(null, "Category name updated successfully."));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .contentType(MediaType.APPLICATION_JSON)
                .body(ApiResponse.error(e.getMessage()));
        }
    }

    @DeleteMapping("/remove")
    public ResponseEntity<ApiResponse<Void>> removeCategory(
            @RequestParam(name = "accountId") UUID accountId,
            @RequestParam(name = "category") String category) {
        try {
            categoriesService.removeCategory(accountId, category);
            return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(ApiResponse.success(null, "Category and its subcategories removed successfully."));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .contentType(MediaType.APPLICATION_JSON)
                .body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/fetch")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getCategoriesAndSubcategories(
        @RequestParam(name = "accountId") UUID accountId) {
        try {
            Map<String, Object> response = categoriesService.getCategoriesAndSubcategories(accountId);
            return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(ApiResponse.success(response));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .contentType(MediaType.APPLICATION_JSON)
                .body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/fetch/names")
    public ResponseEntity<ApiResponse<List<String>>> getCategoryNames(
        @RequestParam(name = "accountId") UUID accountId) {
        try {
            List<String> categoryNames = categoriesService.getCategoryNames(accountId);
            return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(ApiResponse.success(categoryNames));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .contentType(MediaType.APPLICATION_JSON)
                .body(ApiResponse.error(e.getMessage()));
        }
    }
}