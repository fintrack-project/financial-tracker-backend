package com.fintrack.controller.finance;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.MediaType;
import com.fintrack.common.ApiResponse;
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
            return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(ApiResponse.success(portfolioData));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .contentType(MediaType.APPLICATION_JSON)
                .body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/piechart-data")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getPortfolioPieChartData(@RequestBody Map<String, Object> requestData) {
        try {
            UUID accountId = UUID.fromString((String) requestData.get("accountId"));
            String category = (String) requestData.get("category");
            String baseCurrency = (String) requestData.get("baseCurrency");

            List<Map<String, Object>> pieChartData = portfolioService.calculatePortfolioPieChartData(accountId, category, baseCurrency);
            return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(ApiResponse.success(pieChartData));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .contentType(MediaType.APPLICATION_JSON)
                .body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/barchart-data")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getPortfolioBarChartsData(@RequestBody Map<String, Object> requestData) {
        try {
            UUID accountId = UUID.fromString((String) requestData.get("accountId"));
            String category = (String) requestData.get("category");
            String baseCurrency = (String) requestData.get("baseCurrency");

            List<Map<String, Object>> barChartData = portfolioService.calculatePortfolioBarChartsData(accountId, category, baseCurrency);
            return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(ApiResponse.success(barChartData));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .contentType(MediaType.APPLICATION_JSON)
                .body(ApiResponse.error(e.getMessage()));
        }
    }
}