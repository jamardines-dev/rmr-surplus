package com.inventory.vehicle.navigation.presentation;

import com.inventory.vehicle.auth.application.SessionService;
import com.inventory.vehicle.navigation.SceneManager;
import com.inventory.vehicle.navigation.SidebarController;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import org.springframework.stereotype.Controller;

@Controller
public class SidebarComponentController extends SidebarController {

    @FXML
    private Label welcomeLabel;

    public SidebarComponentController(SceneManager sceneManager, SessionService sessionService) {
        super(sceneManager, sessionService);
    }

    @FXML
    private void initialize() {
        welcomeLabel.setText("Signed in as " + sessionService.getCurrentUsername());
    }
}
