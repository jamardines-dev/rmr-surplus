package com.inventory.vehicle.navigation;

public enum View {
    LOGIN("/fxml/auth/login-view.fxml", "Login"),
    DASHBOARD("/fxml/dashboard/dashboard-view.fxml", "Dashboard"),
    EMPLOYEE_DASHBOARD("/fxml/dashboard/employee-dashboard-view.fxml", "Employee Dashboard"),
    EMPLOYEE_SALES("/fxml/sales/employee-sales-view.fxml", "My Sales Today"),
    PRODUCT_LIST("/fxml/product/product-list-view.fxml", "Products"),
    INVENTORY("/fxml/inventory/inventory-view.fxml", "Inventory"),
    SALES_HISTORY("/fxml/sales/sales-history-view.fxml", "Sales History"),
    USERS("/fxml/auth/user-management-view.fxml", "Users"),
    REPORTS("/fxml/report/reports-view.fxml", "Reports");

    private final String fxmlPath;
    private final String title;

    View(String fxmlPath, String title) {
        this.fxmlPath = fxmlPath;
        this.title = title;
    }

    public String getFxmlPath() {
        return fxmlPath;
    }

    public String getTitle() {
        return title;
    }

    public boolean isPublicView() {
        return this == LOGIN;
    }

    public boolean isEmployeeView() {
        return this == EMPLOYEE_DASHBOARD || this == EMPLOYEE_SALES;
    }

    public boolean isAdminView() {
        return this == DASHBOARD
                || this == PRODUCT_LIST
                || this == INVENTORY
                || this == SALES_HISTORY
                || this == USERS
                || this == REPORTS;
    }
}
