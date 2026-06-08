package com.inventory.vehicle.inventory.presentation;

import com.inventory.vehicle.auth.application.SessionService;
import com.inventory.vehicle.common.util.MoneyFormat;
import com.inventory.vehicle.inventory.application.InventoryQueryService;
import com.inventory.vehicle.navigation.SceneManager;
import com.inventory.vehicle.navigation.SidebarController;
import com.inventory.vehicle.product.presentation.ProductTableRow;
import java.math.BigDecimal;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
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

    @FXML
    private Label activeProductsLabel;

    @FXML
    private Label totalStockLabel;

    @FXML
    private Label inventoryValueLabel;

    @FXML
    private Label outOfStockLabel;

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
        priceColumn.setCellFactory(column -> moneyCell());
        inventoryTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        refreshInventory();
    }

    @FXML
    private void refreshInventory() {
        var products = inventoryQueryService.listInventory()
                .stream()
                .map(ProductTableRow::new)
                .toList();
        inventoryTable.getItems().setAll(products);

        int totalStock = products.stream()
                .mapToInt(ProductTableRow::getCurrentStock)
                .sum();
        BigDecimal inventoryValue = products.stream()
                .map(product -> product.getUnitPrice().multiply(BigDecimal.valueOf(product.getCurrentStock())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        long outOfStock = products.stream()
                .filter(product -> product.getCurrentStock() == 0)
                .count();

        activeProductsLabel.setText(String.valueOf(products.size()));
        totalStockLabel.setText(String.valueOf(totalStock));
        inventoryValueLabel.setText(MoneyFormat.peso(inventoryValue));
        outOfStockLabel.setText(String.valueOf(outOfStock));
    }

    private <S> TableCell<S, BigDecimal> moneyCell() {
        return new TableCell<>() {
            @Override
            protected void updateItem(BigDecimal amount, boolean empty) {
                super.updateItem(amount, empty);
                setText(empty ? null : MoneyFormat.peso(amount));
            }
        };
    }

    @FXML
    private void backToDashboard() {
        showDashboard();
    }
}
