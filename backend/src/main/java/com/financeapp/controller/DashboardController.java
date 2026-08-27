package com.financeapp.controller;

import com.financeapp.dto.DashboardDtos.DashboardResponse;
import com.financeapp.dto.InsightDtos.InsightResponse;
import com.financeapp.security.UserPrincipal;
import com.financeapp.service.DashboardService;
import com.financeapp.service.InsightService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
@Tag(name = "Dashboard")
public class DashboardController {

    private final DashboardService dashboardService;
    private final InsightService insightService;

    @GetMapping
    public DashboardResponse dashboard(@AuthenticationPrincipal UserPrincipal user) {
        return dashboardService.build(user.getId());
    }

    @GetMapping("/insights")
    public InsightResponse insights(@AuthenticationPrincipal UserPrincipal user) {
        return insightService.build(user.getId());
    }
}

