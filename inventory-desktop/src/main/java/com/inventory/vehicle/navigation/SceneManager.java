package com.inventory.vehicle.navigation;

import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Screen;
import javafx.stage.Stage;
import org.springframework.stereotype.Component;

@Component
public class SceneManager {

    private final ViewLoader viewLoader;
    private Stage stage;

    public SceneManager(ViewLoader viewLoader) {
        this.viewLoader = viewLoader;
    }

    public void setStage(Stage stage) {
        this.stage = stage;
    }

    public void show(View view) {
        Parent root = viewLoader.load(view);
        Scene scene = new Scene(root);
        scene.getStylesheets().add(getClass().getResource("/css/global.css").toExternalForm());

        stage.setTitle("Vehicle Parts Inventory - " + view.getTitle());
        stage.setScene(scene);
        stage.setMinWidth(1024);
        stage.setMinHeight(700);
        stage.setWidth(Screen.getPrimary().getVisualBounds().getWidth());
        stage.setHeight(Screen.getPrimary().getVisualBounds().getHeight());
        stage.setMaximized(true);
        stage.show();
    }
}
