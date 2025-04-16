package com.fintrack.controller;

import com.fintrack.service.HoldingsCategoriesService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/categories")
public class HoldingsCategoriesController {

    private final HoldingsCategoriesService holdingsCategoriesService;

    public HoldingsCategoriesController(HoldingsCategoriesService holdingsCategoriesService) {
        this.holdingsCategoriesService = holdingsCategoriesService;
    }

    @PostMapping("/holdings/update")
    public ResponseEntity<String> updateHoldingsCategories(
        @RequestParam UUID accountId,
        @RequestBody Map<String, Map<String, String>> holdingsCategories
    ) {
        // Pass the data to the service layer
        holdingsCategoriesService.updateHoldingsCategories(accountId, holdingsCategories);
    
        return ResponseEntity.ok("Holdings categories updated successfully.");
    }

    @PostMapping("/holdings/add")
    public ResponseEntity<String> addHoldingsCategories(
        @RequestParam(name = "accountId") UUID accountId,
        @RequestBody Map<String, Map<String, String>> holdingsCategories
    ) {
        // Pass the data to the service layer
        holdingsCategoriesService.addHoldingsCategories(accountId, holdingsCategories);
    
        return ResponseEntity.ok("Holdings categories added successfully.");
    }

    @PostMapping("/holdings/remove")
    public ResponseEntity<String> removeHoldingsCategory(
        @RequestParam(name = "accountId") UUID accountId,
        @RequestParam(name = "category") String category
    ) {
        // Pass the data to the service layer
        holdingsCategoriesService.removeHoldingsCategory(accountId, category);
    
        return ResponseEntity.ok("Holdings category removed successfully.");
    }

    @GetMapping("/holdings/fetch")
    public ResponseEntity<Map<String, Map<String, String>>> fetchHoldingsCategories(@RequestParam UUID accountId) {
        Map<String, Map<String, String>> holdings = holdingsCategoriesService.fetchHoldingsCategories(accountId);
        return ResponseEntity.ok(holdings);
    }
}