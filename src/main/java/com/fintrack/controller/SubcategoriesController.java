package com.fintrack.controller;

import com.fintrack.service.SubcategoriesService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/categories/subcategories")
public class SubcategoriesController {

    private final SubcategoriesService subcategoriesService;

    public SubcategoriesController(SubcategoriesService subcategoriesService) {
        this.subcategoriesService = subcategoriesService;
    }

    @PostMapping("/add")
    public ResponseEntity<String> addSubcategory(@RequestBody Map<String, Object> subcategoryData) {
        UUID accountId = UUID.fromString((String) subcategoryData.get("accountId"));
        String categoryName = (String) subcategoryData.get("category_name");
        String subcategoryName = (String) subcategoryData.get("subcategory_name");
    
        subcategoriesService.addSubcategory(accountId, categoryName, subcategoryName);
        return ResponseEntity.ok("Subcategory added successfully.");
    }

    @PostMapping("/name/update")
    public ResponseEntity<String> updateSubcategoryName(@RequestBody Map<String, Object> subcategoryData) {
        UUID accountId = UUID.fromString((String) subcategoryData.get("accountId"));
        String categoryName = (String) subcategoryData.get("category_name");
        String oldSubcategoryName = (String) subcategoryData.get("old_subcategory_name");
        String newSubcategoryName = (String) subcategoryData.get("new_subcategory_name");
    
        subcategoriesService.updateSubcategoryName(accountId, categoryName, oldSubcategoryName, newSubcategoryName);
        return ResponseEntity.ok("Subcategory name updated successfully.");
    }
    
    @DeleteMapping("/remove")
    public ResponseEntity<String> removeSubcategory(
            @RequestParam UUID accountId,
            @RequestParam String category,
            @RequestParam String subcategory) {
        subcategoriesService.removeSubcategory(accountId, category, subcategory);
        return ResponseEntity.ok("Subcategory removed successfully.");
    }
}