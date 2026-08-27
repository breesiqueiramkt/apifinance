package com.financeapp.controller;

import com.financeapp.dto.ReportDtos.*;
import com.financeapp.security.UserPrincipal;
import com.financeapp.service.ReportService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
@Tag(name = "Relatórios")
public class ReportController {

    private final ReportService reportService;

    @GetMapping("/cashflow")
    public CashflowReport cashflow(@AuthenticationPrincipal UserPrincipal user,
                                    @RequestParam(defaultValue = "6") int months) {
        return reportService.cashflow(user.getId(), months);
    }

    @GetMapping("/net-worth")
    public NetWorthReport netWorth(@AuthenticationPrincipal UserPrincipal user,
                                    @RequestParam(defaultValue = "6") int months) {
        return reportService.netWorthEvolution(user.getId(), months);
    }

    @GetMapping("/expenses-by-category")
    public CategoryReport expensesByCategory(
            @AuthenticationPrincipal UserPrincipal user,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end) {
        return reportService.expensesByCategory(user.getId(), start, end);
    }
}
