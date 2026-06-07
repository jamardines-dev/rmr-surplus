package com.inventory.vehicle.navigation;

import java.io.IOException;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

@Component
public class ViewLoader {

    private final ApplicationContext applicationContext;

    public ViewLoader(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    public Parent load(View view) {
        FXMLLoader loader = new FXMLLoader(getClass().getResource(view.getFxmlPath()));
        loader.setControllerFactory(applicationContext::getBean);

        try {
            return loader.load();
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to load view: " + view.getFxmlPath(), exception);
        }
    }
}
