package com.inventory.vehicle.auth.presentation;

import com.inventory.vehicle.auth.application.LoginService;
import com.inventory.vehicle.auth.application.SessionService;
import com.inventory.vehicle.auth.domain.Role;
import com.inventory.vehicle.navigation.SceneManager;
import com.inventory.vehicle.navigation.View;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import org.springframework.stereotype.Controller;

@Controller
public class LoginController {

    private final LoginService loginService;
    private final SessionService sessionService;
    private final SceneManager sceneManager;

    @FXML
    private TextField usernameField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private Label messageLabel;

    public LoginController(LoginService loginService, SessionService sessionService, SceneManager sceneManager) {
        this.loginService = loginService;
        this.sessionService = sessionService;
        this.sceneManager = sceneManager;
    }

    @FXML
    private void handleLogin() {
        String username = usernameField.getText();
        String password = passwordField.getText();

        if (username == null || username.isBlank() || password == null || password.isBlank()) {
            messageLabel.setText("Enter your username and password.");
            return;
        }

        if (loginService.login(username, password)) {
            sceneManager.show(sessionService.getCurrentRole() == Role.ADMIN
                    ? View.DASHBOARD
                    : View.EMPLOYEE_DASHBOARD);
        } else {
            messageLabel.setText("Invalid username or password.");
        }
    }
}
