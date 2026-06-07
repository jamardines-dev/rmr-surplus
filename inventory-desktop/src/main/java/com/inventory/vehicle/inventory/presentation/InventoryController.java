package com.inventory.vehicle.inventory.presentation;

import com.inventory.vehicle.auth.application.SessionService;
import com.inventory.vehicle.inventory.application.InventoryQueryService;
import com.inventory.vehicle.navigation.SceneManager;
import com.inventory.vehicle.navigation.SidebarController;
import com.inventory.vehicle.product.presentation.ProductTableRow;
import java.math.BigDecimal;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import org.springframework.stereotype.Controller;

@Controller
public class InventoryController extends SidebarController {

    private final InventoryQueryService inventoryQueryService;

    @FXML
    private TableView<ProductTableRow> inventoryTable;

    @FXML
    private TableColumn<ProductTableRow, Long> idColumn;

    @FXML
    private TableColumn<ProductTableRow, String> productNameColumn;

    @FXML
    private TableColumn<ProductTableRow, String> brandColumn;

    @FXML
    private TableColumn<ProductTableRow, String> vehicleTypeColumn;

    @FXML
    private TableColumn<ProductTableRow, Integer> stockColumn;

    @FXML
    private TableColumn<ProductTableRow, BigDecimal> priceColumn;

    public InventoryController(SceneManager sceneManager, SessionService sessionService, InventoryQueryService inventoryQueryService) {
        super(sceneManager, sessionService);
        this.inventoryQueryService = inventoryQueryService;
    }

    @FXML
    private void initialize() {
        idColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        productNameColumn.setCellValueFactory(new PropertyValueFactory<>("productName"));
        brandColumn.setCellValueFactory(new PropertyValueFactory<>("brandName"));
        vehicleTypeColumn.setCellValueFactory(new PropertyValueFactory<>("vehicleTypeName"));
        stockColumn.setCellValueFactory(new PropertyValueFactory<>("currentStock"));
        priceColumn.setCellValueFactory(new PropertyValueFactory<>("unitPrice"));
        refreshInventory();
    }

    @FXML
    private void refreshInventory() {
        inventoryTable.getItems().setAll(inventoryQueryService.listInventory()
                .stream()
                .map(ProductTableRow::new)
                .toList());
    }

    @FXML
    private void backToDashboard() {
        showDashboard();
    }
}
