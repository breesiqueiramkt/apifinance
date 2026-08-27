package com.financeapp.dto;

import java.math.BigDecimal;
import java.util.List;

public class ReportDtos {

    public record CashflowPoint(String month, BigDecimal income, BigDecimal expenses) {}

    public record NetWorthPoint(String month, BigDecimal netWorth) {}

    public record CategorySlice(Long categoryId, String categoryName, String icon, BigDecimal total) {}

    public record CashflowReport(List<CashflowPoint> points) {}

    public record NetWorthReport(List<NetWorthPoint> points) {}

    public record CategoryReport(List<CategorySlice> slices, BigDecimal total) {}
}
