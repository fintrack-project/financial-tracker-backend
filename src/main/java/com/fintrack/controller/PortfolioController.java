package com.fintrack.controller;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import com.fintrack.service.PortfolioService;


@RestController
@RequestMapping("/api/portfolio")
public class PortfolioController {

    private final PortfolioService portfolioService;

    public PortfolioController(PortfolioService portfolioService) {
        this.portfolioService = portfolioService;
    }

    @PostMapping("/portfolio-data")
    public ResponseEntity<List<Map<String, Object>>> getPortfolioData(@RequestBody Map<String, Object> requestData) {
        UUID accountId = UUID.fromString((String) requestData.get("accountId"));
        String baseCurrency = (String) requestData.get("baseCurrency");

        List<Map<String, Object>> portfolioData = portfolioService.calculatePortfolioData(accountId, baseCurrency);
        return ResponseEntity.ok(portfolioData);
    }

    @PostMapping("/piechart-data")
    public ResponseEntity<List<Map<String, Object>>> getPortfolioPieChartData(@RequestBody Map<String, Object> requestData) {
        UUID accountId = UUID.fromString((String) requestData.get("accountId"));
        String category = (String) requestData.get("category");

        List<Map<String, Object>> pieChartData = portfolioService.calculatePortfolioPieChartData(accountId, category);
        return ResponseEntity.ok(pieChartData);
    }

    @PostMapping("/barchart-data")
    public ResponseEntity<List<Map<String, Object>>> getPortfolioBarChartsData(@RequestBody Map<String, Object> requestData) {
        UUID accountId = UUID.fromString((String) requestData.get("accountId"));
        String category = (String) requestData.get("category");

        List<Map<String, Object>> barChartData = portfolioService.calculatePortfolioBarChartsData(accountId, category);
        return ResponseEntity.ok(barChartData);
    }
}