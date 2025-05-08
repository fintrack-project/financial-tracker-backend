package com.fintrack.controller.finance;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.MediaType;
import com.fintrack.common.ApiResponse;
import com.fintrack.common.ResponseWrapper;
import com.fintrack.service.finance.PortfolioService;

import java.util.*;

@RestController
@RequestMapping(value = "/api/portfolio", produces = MediaType.APPLICATION_JSON_VALUE)
public class PortfolioController {

    private final PortfolioService portfolioService;

    public PortfolioController(PortfolioService portfolioService) {
        this.portfolioService = portfolioService;
    }

    @PostMapping("/portfolio-data")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getPortfolioData(@RequestBody Map<String, Object> requestData) {
        try {
            UUID accountId = UUID.fromString((String) requestData.get("accountId"));
            String baseCurrency = (String) requestData.get("baseCurrency");

            List<Map<String, Object>> portfolioData = portfolioService.calculatePortfolioData(accountId, baseCurrency);
            return ResponseWrapper.ok(portfolioData);
        } catch (Exception e) {
            return ResponseWrapper.badRequest(e.getMessage());
        }
    }

    @PostMapping("/piechart-data")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getPortfolioPieChartData(@RequestBody Map<String, Object> requestData) {
        try {
            UUID accountId = UUID.fromString((String) requestData.get("accountId"));
            String category = (String) requestData.get("category");
            String baseCurrency = (String) requestData.get("baseCurrency");

            List<Map<String, Object>> pieChartData = portfolioService.calculatePortfolioPieChartData(accountId, category, baseCurrency);
            return ResponseWrapper.ok(pieChartData);
        } catch (Exception e) {
            return ResponseWrapper.badRequest(e.getMessage());
        }
    }

    @PostMapping("/barchart-data")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getPortfolioBarChartsData(@RequestBody Map<String, Object> requestData) {
        try {
            UUID accountId = UUID.fromString((String) requestData.get("accountId"));
            String category = (String) requestData.get("category");
            String baseCurrency = (String) requestData.get("baseCurrency");

            List<Map<String, Object>> barChartData = portfolioService.calculatePortfolioBarChartsData(accountId, category, baseCurrency);
            return ResponseWrapper.ok(barChartData);
        } catch (Exception e) {
            return ResponseWrapper.badRequest(e.getMessage());
        }
    }
}