package com.fintrack.controller;

import com.fintrack.dto.ApiResponse;
import com.fintrack.dto.MonthComparisonResponse;
import com.fintrack.dto.MonthlySummaryResponse;
import com.fintrack.security.CustomUserDetails;
import com.fintrack.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/summary")
    public ResponseEntity<ApiResponse> getMonthlySummary(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        MonthlySummaryResponse summary = dashboardService.getMonthlySummary(userDetails.getId());
        return ResponseEntity.ok(new ApiResponse(true, "Monthly summary retrieved", summary));
    }

    @GetMapping("/comparison")
    public ResponseEntity<ApiResponse> getMonthComparison(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        MonthComparisonResponse comparison = dashboardService.getMonthComparison(userDetails.getId());
        return ResponseEntity.ok(new ApiResponse(true, "Month comparison retrieved", comparison));
    }

    @GetMapping("/stats")
    public ResponseEntity<ApiResponse> getDashboardStats(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        Map<String, Object> stats = dashboardService.getDashboardStats(userDetails.getId());
        return ResponseEntity.ok(new ApiResponse(true, "Dashboard stats retrieved", stats));
    }
}
