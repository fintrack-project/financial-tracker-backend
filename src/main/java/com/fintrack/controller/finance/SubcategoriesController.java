package com.fintrack.controller.finance;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.MediaType;
import com.fintrack.common.ApiResponse;
import com.fintrack.common.ResponseWrapper;
import com.fintrack.service.finance.SubcategoriesService;
import com.fintrack.constants.Color;

import java.util.*;

@RestController
@RequestMapping(value = "/api/categories/subcategories", produces = MediaType.APPLICATION_JSON_VALUE)
public class SubcategoriesController {

    private final SubcategoriesService subcategoriesService;

    public SubcategoriesController(SubcategoriesService subcategoriesService) {
        this.subcategoriesService = subcategoriesService;
    }

    @PostMapping("/add")
    public ResponseEntity<ApiResponse<Void>> addSubcategory(@RequestBody Map<String, Object> subcategoryData) {
        try {
            UUID accountId = UUID.fromString((String) subcategoryData.get("accountId"));
            String categoryName = (String) subcategoryData.get("category_name");
            String subcategoryName = (String) subcategoryData.get("subcategory_name");
        
            subcategoriesService.addSubcategory(accountId, categoryName, subcategoryName);
            return ResponseWrapper.ok(null, "Subcategory added successfully.");
        } catch (Exception e) {
            return ResponseWrapper.badRequest(e.getMessage());
        }
    }

    @PostMapping("/name/update")
    public ResponseEntity<ApiResponse<Void>> updateSubcategoryName(@RequestBody Map<String, Object> subcategoryData) {
        try {
            UUID accountId = UUID.fromString((String) subcategoryData.get("accountId"));
            String categoryName = (String) subcategoryData.get("category_name");
            String oldSubcategoryName = (String) subcategoryData.get("old_subcategory_name");
            String newSubcategoryName = (String) subcategoryData.get("new_subcategory_name");
        
            subcategoriesService.updateSubcategoryName(accountId, categoryName, oldSubcategoryName, newSubcategoryName);
            return ResponseWrapper.ok(null, "Subcategory name updated successfully.");
        } catch (Exception e) {
            return ResponseWrapper.badRequest(e.getMessage());
        }
    }
    
    @DeleteMapping("/remove")
    public ResponseEntity<ApiResponse<Void>> removeSubcategory(
            @RequestParam(name = "accountId") UUID accountId,
            @RequestParam(name = "category") String category,
            @RequestParam(name = "subcategory") String subcategory) {
        try {
            subcategoriesService.removeSubcategory(accountId, category, subcategory);
            return ResponseWrapper.ok(null, "Subcategory removed successfully.");
        } catch (Exception e) {
            return ResponseWrapper.badRequest(e.getMessage());
        }
    }

    @PostMapping("/color/update")
    public ResponseEntity<ApiResponse<Void>> updateSubcategoryColor(@RequestBody Map<String, Object> subcategoryData) {
        try {
            UUID accountId = UUID.fromString((String) subcategoryData.get("accountId"));
            String categoryName = (String) subcategoryData.get("category_name");
            String subcategoryName = (String) subcategoryData.get("subcategory_name");
            String hexCode = (String) subcategoryData.get("color");
            
            subcategoriesService.updateSubcategoryColor(accountId, categoryName, subcategoryName, hexCode);
            return ResponseWrapper.ok(null, "Subcategory color updated successfully.");
        } catch (IllegalArgumentException e) {
            return ResponseWrapper.badRequest(e.getMessage());
        } catch (Exception e) {
            return ResponseWrapper.badRequest(e.getMessage());
        }
    }

    @GetMapping("/fetch/color-map")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getSubcategoryColorMap(
        @RequestParam(name = "accountId") UUID accountId,
        @RequestParam(name = "categoryName") String categoryName) {
        try {
            Map<String, Object> response = subcategoriesService.getSubcategoryColorMap(accountId, categoryName);
            return ResponseWrapper.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseWrapper.badRequest(e.getMessage());
        } catch (Exception e) {
            return ResponseWrapper.badRequest(e.getMessage());
        }
    }
}