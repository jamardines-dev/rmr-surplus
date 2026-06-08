package com.inventory.vehicle.navigation;

import com.inventory.vehicle.auth.domain.Role;
import com.inventory.vehicle.auth.application.SessionService;
import javafx.fxml.FXML;

public abstract class SidebarController {

    protected final SceneManager sceneManager;
    protected final SessionService sessionService;

    protected SidebarController(SceneManager sceneManager, SessionService sessionService) {
        this.sceneManager = sceneManager;
        this.sessionService = sessionService;
    }

    @FXML
    protected void showDashboard() {
        sceneManager.show(sessionService.getCurrentRole() == Role.ADMIN
                ? View.DASHBOARD
                : View.EMPLOYEE_DASHBOARD);
    }

    @FXML
    protected void showProducts() {
        sceneManager.show(View.PRODUCT_LIST);
    }

    @FXML
    protected void showInventory() {
        sceneManager.show(View.INVENTORY);
    }

    @FXML
    protected void showSalesHistory() {
        sceneManager.show(View.SALES_HISTORY);
    }

    @FXML
    protected void showEmployeeSales() {
        sceneManager.show(View.EMPLOYEE_SALES);
    }

    @FXML
    protected void showReports() {
        sceneManager.show(View.REPORTS);
    }

    @FXML
    protected void showUsers() {
        sceneManager.show(View.USERS);
    }

    @FXML
    protected void logout() {
        sessionService.clearSession();
        sceneManager.show(View.LOGIN);
    }
}
