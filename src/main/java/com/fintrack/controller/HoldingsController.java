package com.fintrack.controller;

import com.fintrack.model.HoldingDto;
import com.fintrack.service.HoldingsService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.*;

@RestController
@RequestMapping("/api/accounts")
public class HoldingsController {

    private HoldingsService holdingsService;

    public HoldingsController(HoldingsService holdingsService) {
        this.holdingsService = holdingsService;
    }

    @GetMapping("/{accountId}/holdings")
    public ResponseEntity<List<HoldingDto>> getHoldings(@PathVariable UUID accountId) {
        List<HoldingDto> holdings = holdingsService.getLatestHoldingsByAccount(accountId);
        return ResponseEntity.ok(holdings);
    }
}