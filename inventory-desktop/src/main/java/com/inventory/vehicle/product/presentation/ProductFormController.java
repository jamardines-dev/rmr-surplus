package com.inventory.vehicle.product.presentation;

import com.inventory.vehicle.auth.application.SessionService;
import com.inventory.vehicle.navigation.SceneManager;
import com.inventory.vehicle.navigation.SidebarController;
import javafx.fxml.FXML;
import org.springframework.stereotype.Controller;

@Controller
public class ProductFormController extends SidebarController {

    public ProductFormController(SceneManager sceneManager, SessionService sessionService) {
        super(sceneManager, sessionService);
    }

    @FXML
    private void backToProducts() {
        showProducts();
    }
}
