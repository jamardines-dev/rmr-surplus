package com.inventory.vehicle.navigation.presentation;

import com.inventory.vehicle.auth.domain.Role;
import com.inventory.vehicle.auth.application.SessionService;
import com.inventory.vehicle.navigation.SceneManager;
import com.inventory.vehicle.navigation.SidebarController;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import org.springframework.stereotype.Controller;

@Controller
public class SidebarComponentController extends SidebarController {

    @FXML
    private Label welcomeLabel;

    @FXML
    private Button productsButton;

    @FXML
    private Button inventoryButton;

    @FXML
    private Button salesHistoryButton;

    @FXML
    private Button employeeSalesButton;

    @FXML
    private Button reportsButton;

    @FXML
    private Button usersButton;

    public SidebarComponentController(SceneManager sceneManager, SessionService sessionService) {
        super(sceneManager, sessionService);
    }

    @FXML
    private void initialize() {
        welcomeLabel.setText("Signed in as " + sessionService.getCurrentUsername());
        boolean admin = sessionService.getCurrentRole() == Role.ADMIN;
        setVisibleForAdmin(productsButton, admin);
        setVisibleForAdmin(inventoryButton, admin);
        setVisibleForAdmin(salesHistoryButton, admin);
        setVisibleForAdmin(reportsButton, admin);
        setVisibleForAdmin(usersButton, admin);
        employeeSalesButton.setVisible(!admin);
        employeeSalesButton.setManaged(!admin);
    }

    private void setVisibleForAdmin(Button button, boolean admin) {
        button.setVisible(admin);
        button.setManaged(admin);
    }
}
