package com.fintrack.controller.finance;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.MediaType;
import com.fintrack.common.ApiResponse;
import com.fintrack.service.finance.SubcategoriesService;

import java.util.List;
import java.util.Map;
import java.util.UUID;

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
            return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(ApiResponse.success(null, "Subcategory added successfully."));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .contentType(MediaType.APPLICATION_JSON)
                .body(ApiResponse.error(e.getMessage()));
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
            return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(ApiResponse.success(null, "Subcategory name updated successfully."));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .contentType(MediaType.APPLICATION_JSON)
                .body(ApiResponse.error(e.getMessage()));
        }
    }
    
    @DeleteMapping("/remove")
    public ResponseEntity<ApiResponse<Void>> removeSubcategory(
            @RequestParam(name = "accountId") UUID accountId,
            @RequestParam(name = "category") String category,
            @RequestParam(name = "subcategory") String subcategory) {
        try {
            subcategoriesService.removeSubcategory(accountId, category, subcategory);
            return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(ApiResponse.success(null, "Subcategory removed successfully."));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .contentType(MediaType.APPLICATION_JSON)
                .body(ApiResponse.error(e.getMessage()));
        }
    }
}