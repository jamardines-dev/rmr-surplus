package com.inventory.vehicle.navigation;

import com.inventory.vehicle.auth.application.SessionService;
import com.inventory.vehicle.auth.domain.Role;
import javafx.geometry.Rectangle2D;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.BorderPane;
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
        makeContentScrollable(root);
        Scene scene = new Scene(root);
        scene.getStylesheets().add(getClass().getResource("/css/global.css").toExternalForm());

        stage.setTitle("RMR SURPLUS - " + view.getTitle());
        stage.setScene(scene);
        Rectangle2D bounds = getCurrentScreenBounds();
        stage.setMinWidth(Math.min(1024, bounds.getWidth()));
        stage.setMinHeight(Math.min(700, bounds.getHeight()));
        stage.setWidth(Math.min(1280, bounds.getWidth()));
        stage.setHeight(Math.min(820, bounds.getHeight()));
        stage.setMaximized(true);
        stage.show();
    }

    private void makeContentScrollable(Parent root) {
        if (!(root instanceof BorderPane borderPane) || borderPane.getCenter() == null) {
            return;
        }

        Node content = borderPane.getCenter();
        if (content instanceof ScrollPane) {
            return;
        }

        ScrollPane scrollPane = new ScrollPane(content);
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(false);
        scrollPane.setPannable(true);
        scrollPane.getStyleClass().add("content-scroll");
        borderPane.setCenter(scrollPane);
    }

    private Rectangle2D getCurrentScreenBounds() {
        return Screen.getScreensForRectangle(stage.getX(), stage.getY(), 1, 1)
                .stream()
                .findFirst()
                .orElse(Screen.getPrimary())
                .getVisualBounds();
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
