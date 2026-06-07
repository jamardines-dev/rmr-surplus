package com.inventory.vehicle.navigation;

public enum View {
    LOGIN("/fxml/auth/login-view.fxml", "Login"),
    DASHBOARD("/fxml/dashboard/dashboard-view.fxml", "Dashboard"),
    PRODUCT_LIST("/fxml/product/product-list-view.fxml", "Products"),
    PRODUCT_FORM("/fxml/product/product-form-view.fxml", "Product Form"),
    INVENTORY("/fxml/inventory/inventory-view.fxml", "Inventory"),
    SALES_HISTORY("/fxml/sales/sales-history-view.fxml", "Sales History"),
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
}
