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

    @PostMapping("/piechart-data")
    public ResponseEntity<List<Map<String, Object>>> getPortfolioPieChartData(@RequestBody Map<String, Object> requestData) {
        UUID accountId = UUID.fromString((String) requestData.get("accountId"));
        String category = (String) requestData.get("category");

        List<Map<String, Object>> pieChartData = portfolioService.calculatePortfolioPieChartData(accountId, category);
        return ResponseEntity.ok(pieChartData);
    }
}