package com.inventory.vehicle.navigation;

import com.inventory.vehicle.auth.application.SessionService;
import com.inventory.vehicle.auth.domain.Role;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Screen;
import javafx.stage.Stage;
import org.springframework.stereotype.Component;

@Component
public class SceneManager {

    private final ViewLoader viewLoader;
    private final SessionService sessionService;
    private Stage stage;

    public SceneManager(ViewLoader viewLoader, SessionService sessionService) {
        this.viewLoader = viewLoader;
        this.sessionService = sessionService;
    }

    public void setStage(Stage stage) {
        this.stage = stage;
    }

    public void show(View view) {
        view = authorize(view);
        Parent root = viewLoader.load(view);
        Scene scene = new Scene(root);
        scene.getStylesheets().add(getClass().getResource("/css/global.css").toExternalForm());

        stage.setTitle("RMR SURPLUS - " + view.getTitle());
        stage.setScene(scene);
        stage.setMinWidth(1024);
        stage.setMinHeight(700);
        stage.setWidth(Screen.getPrimary().getVisualBounds().getWidth());
        stage.setHeight(Screen.getPrimary().getVisualBounds().getHeight());
        stage.setMaximized(true);
        stage.show();
    }

    private View authorize(View view) {
        if (!sessionService.isLoggedIn()) {
            return view.isPublicView() ? view : View.LOGIN;
        }

        Role role = sessionService.getCurrentRole();
        if (role == Role.EMPLOYEE && view.isAdminView()) {
            return View.EMPLOYEE_DASHBOARD;
        }
        if (role == Role.ADMIN && view.isEmployeeView()) {
            return View.DASHBOARD;
        }
        return view;
    }
}
