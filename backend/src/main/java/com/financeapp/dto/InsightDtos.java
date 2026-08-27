package com.financeapp.dto;

import java.util.List;

public class InsightDtos {
    public record InsightResponse(List<String> insights) {}
}
