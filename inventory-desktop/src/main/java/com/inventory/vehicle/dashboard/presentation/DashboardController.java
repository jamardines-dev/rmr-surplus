package com.inventory.vehicle.dashboard.presentation;

import com.inventory.vehicle.auth.application.SessionService;
import com.inventory.vehicle.navigation.SceneManager;
import com.inventory.vehicle.navigation.View;
import com.inventory.vehicle.product.application.ProductQueryService;
import com.inventory.vehicle.product.application.ProductResult;
import com.inventory.vehicle.product.presentation.ProductTableRow;
import com.inventory.vehicle.sales.application.DailySalesService;
import com.inventory.vehicle.sales.application.SalesQueryService;
import com.inventory.vehicle.sales.presentation.SaleTableRow;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import org.springframework.stereotype.Controller;

@Controller
public class DashboardController {

    private final SceneManager sceneManager;
    private final SessionService sessionService;
    private final ProductQueryService productQueryService;
    private final SalesQueryService salesQueryService;
    private final DailySalesService dailySalesService;

    @FXML
    private Label productCountLabel;

    @FXML
    private Label totalStockLabel;

    @FXML
    private Label todaySalesLabel;

    @FXML
    private TableView<ProductTableRow> inventoryTable;

    @FXML
    private TableColumn<ProductTableRow, String> productNameColumn;

    @FXML
    private TableColumn<ProductTableRow, String> brandColumn;

    @FXML
    private TableColumn<ProductTableRow, Integer> stockColumn;

    @FXML
    private TableView<SaleTableRow> recentSalesTable;

    @FXML
    private TableColumn<SaleTableRow, Long> saleIdColumn;

    @FXML
    private TableColumn<SaleTableRow, String> sellerColumn;

    @FXML
    private TableColumn<SaleTableRow, BigDecimal> saleTotalColumn;

    public DashboardController(
            SceneManager sceneManager,
            SessionService sessionService,
            ProductQueryService productQueryService,
            SalesQueryService salesQueryService,
            DailySalesService dailySalesService
    ) {
        this.sceneManager = sceneManager;
        this.sessionService = sessionService;
        this.productQueryService = productQueryService;
        this.salesQueryService = salesQueryService;
        this.dailySalesService = dailySalesService;
    }

    @FXML
    private void initialize() {
        productNameColumn.setCellValueFactory(new PropertyValueFactory<>("productName"));
        brandColumn.setCellValueFactory(new PropertyValueFactory<>("brandName"));
        stockColumn.setCellValueFactory(new PropertyValueFactory<>("currentStock"));
        saleIdColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        sellerColumn.setCellValueFactory(new PropertyValueFactory<>("sellerName"));
        saleTotalColumn.setCellValueFactory(new PropertyValueFactory<>("totalAmount"));
        refreshDashboard();
    }

    @FXML
    private void refreshDashboard() {
        List<ProductResult> products = productQueryService.findActiveProducts();
        int totalStock = products.stream()
                .mapToInt(ProductResult::currentStock)
                .sum();
        BigDecimal todaySales = dailySalesService.calculateTotalForDate(LocalDate.now());

        productCountLabel.setText(String.valueOf(products.size()));
        totalStockLabel.setText(String.valueOf(totalStock));
        todaySalesLabel.setText(todaySales.toPlainString());

        inventoryTable.getItems().setAll(products.stream()
                .map(ProductTableRow::new)
                .toList());
        recentSalesTable.getItems().setAll(salesQueryService.findAllSales()
                .stream()
                .limit(8)
                .map(SaleTableRow::new)
                .toList());
    }

    @FXML
    private void showProducts() {
        sceneManager.show(View.PRODUCT_LIST);
    }

    @FXML
    private void showInventory() {
        sceneManager.show(View.INVENTORY);
    }

    @FXML
    private void showSalesHistory() {
        sceneManager.show(View.SALES_HISTORY);
    }

    @FXML
    private void showReports() {
        sceneManager.show(View.REPORTS);
    }

    @FXML
    private void logout() {
        sessionService.clearSession();
        sceneManager.show(View.LOGIN);
    }
}
