package com.inventory.vehicle;

import com.inventory.vehicle.navigation.SceneManager;
import com.inventory.vehicle.navigation.View;
import javafx.application.Application;
import javafx.stage.Stage;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

public class JavaFxApplication extends Application {

    private ConfigurableApplicationContext applicationContext;

    @Override
    public void init() {
        applicationContext = new SpringApplicationBuilder(InventoryApplication.class)
                .web(WebApplicationType.NONE)
                .run(getParameters().getRaw().toArray(String[]::new));
    }

    @Override
    public void start(Stage primaryStage) {
        SceneManager sceneManager = applicationContext.getBean(SceneManager.class);
        sceneManager.setStage(primaryStage);
        sceneManager.show(View.LOGIN);
    }

    @Override
    public void stop() {
        applicationContext.close();
    }
}
