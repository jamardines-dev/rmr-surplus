package com.inventory.vehicle.dashboard.presentation;

import com.inventory.vehicle.sales.application.EmployeeSalesSummary;
import java.math.BigDecimal;

public class EmployeeSummaryTableRow {

    private final String employeeName;
    private final long transactionsToday;
    private final int itemsSoldToday;
    private final BigDecimal totalSalesToday;

    public EmployeeSummaryTableRow(EmployeeSalesSummary summary) {
        this.employeeName = summary.employeeName();
        this.transactionsToday = summary.transactionsToday();
        this.itemsSoldToday = summary.itemsSoldToday();
        this.totalSalesToday = summary.totalSalesToday();
    }

    public String getEmployeeName() {
        return employeeName;
    }

    public long getTransactionsToday() {
        return transactionsToday;
    }

    public int getItemsSoldToday() {
        return itemsSoldToday;
    }

    public BigDecimal getTotalSalesToday() {
        return totalSalesToday;
    }
}
