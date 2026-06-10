package com.inventory.vehicle.sales.application;

import java.math.BigDecimal;

public record EmployeeSalesSummary(
        String employeeName,
        long transactionsToday,
        int itemsSoldToday,
        BigDecimal totalSalesToday
) {
}
