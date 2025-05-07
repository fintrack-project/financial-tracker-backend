package com.fintrack.controller.finance;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.MediaType;
import com.fintrack.common.ApiResponse;
import com.fintrack.service.finance.HoldingsCategoriesService;

import java.util.*;

@RestController
@RequestMapping(value = "/api/categories", produces = MediaType.APPLICATION_JSON_VALUE)
public class HoldingsCategoriesController {

    private final HoldingsCategoriesService holdingsCategoriesService;

    public HoldingsCategoriesController(HoldingsCategoriesService holdingsCategoriesService) {
        this.holdingsCategoriesService = holdingsCategoriesService;
    }

    @PostMapping("/holdings/update")
    public ResponseEntity<ApiResponse<Void>> updateHoldingsCategories(
        @RequestParam(name = "accountId") UUID accountId,
        @RequestBody Map<String, Map<String, String>> holdingsCategories
    ) {
        try {
            holdingsCategoriesService.updateHoldingsCategories(accountId, holdingsCategories);
            return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(ApiResponse.success(null, "Holdings categories updated successfully."));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .contentType(MediaType.APPLICATION_JSON)
                .body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/holdings/add")
    public ResponseEntity<ApiResponse<Void>> addHoldingsCategories(
        @RequestParam(name = "accountId") UUID accountId,
        @RequestBody Map<String, Map<String, String>> holdingsCategories
    ) {
        try {
            holdingsCategoriesService.addHoldingsCategories(accountId, holdingsCategories);
            return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(ApiResponse.success(null, "Holdings categories added successfully."));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .contentType(MediaType.APPLICATION_JSON)
                .body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/holdings/remove")
    public ResponseEntity<ApiResponse<Void>> removeHoldingsCategory(
        @RequestParam(name = "accountId") UUID accountId,
        @RequestParam(name = "category") String category
    ) {
        try {
            holdingsCategoriesService.removeHoldingsCategory(accountId, category);
            return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(ApiResponse.success(null, "Holdings category removed successfully."));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .contentType(MediaType.APPLICATION_JSON)
                .body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/holdings/fetch")
    public ResponseEntity<ApiResponse<Map<String, Map<String, String>>>> fetchHoldingsCategories(@RequestParam(name = "accountId") UUID accountId) {
        try {
            Map<String, Map<String, String>> holdings = holdingsCategoriesService.fetchHoldingsCategories(accountId);
            return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(ApiResponse.success(holdings));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .contentType(MediaType.APPLICATION_JSON)
                .body(ApiResponse.error(e.getMessage()));
        }
    }
}