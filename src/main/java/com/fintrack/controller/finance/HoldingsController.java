package com.fintrack.controller.finance;

import com.fintrack.model.finance.Holdings;
import com.fintrack.service.finance.HoldingsService;

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
    public ResponseEntity<List<Holdings>> getHoldings(@PathVariable UUID accountId) {
        List<Holdings> holdings = holdingsService.getHoldingsByAccount(accountId);
        return ResponseEntity.ok(holdings);
    }
}