package com.fintrack.controller.finance;

import com.fintrack.model.finance.Holdings;
import com.fintrack.service.finance.HoldingsService;
import com.fintrack.common.ApiResponse;
import com.fintrack.common.ResponseWrapper;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.MediaType;

import java.util.*;

@RestController
@RequestMapping(value = "/api/accounts", produces = MediaType.APPLICATION_JSON_VALUE)
public class HoldingsController {

    private HoldingsService holdingsService;

    public HoldingsController(HoldingsService holdingsService) {
        this.holdingsService = holdingsService;
    }

    @GetMapping("/{accountId}/holdings")
    public ResponseEntity<ApiResponse<List<Holdings>>> getHoldings(@PathVariable UUID accountId) {
        try {
            List<Holdings> holdings = holdingsService.getHoldingsByAccount(accountId);
            return ResponseWrapper.ok(holdings);
        } catch (Exception e) {
            return ResponseWrapper.badRequest(e.getMessage());
        }
    }
}