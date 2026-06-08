package com.inventory.vehicle.report.presentation;

import com.inventory.vehicle.auth.application.SessionService;
import com.inventory.vehicle.common.util.MoneyFormat;
import com.inventory.vehicle.navigation.SceneManager;
import com.inventory.vehicle.navigation.SidebarController;
import com.inventory.vehicle.product.presentation.ProductTableRow;
import com.inventory.vehicle.report.application.ReportService;
import com.inventory.vehicle.sales.application.DailySalesService;
import java.math.BigDecimal;
import java.time.LocalDate;
import javafx.fxml.FXML;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import org.springframework.stereotype.Controller;

@Controller
public class ReportController extends SidebarController {

    private final ReportService reportService;
    private final DailySalesService dailySalesService;

    @FXML
    private TableView<ProductTableRow> productTable;

    @FXML
    private TableColumn<ProductTableRow, String> productNameColumn;

    @FXML
    private TableColumn<ProductTableRow, String> brandColumn;

    @FXML
    private TableColumn<ProductTableRow, Integer> stockColumn;

    @FXML
    private TableColumn<ProductTableRow, BigDecimal> priceColumn;

    @FXML
    private DatePicker salesDatePicker;

    @FXML
    private Label totalSalesLabel;

    @FXML
    private Label activeProductsLabel;

    @FXML
    private Label totalStockLabel;

    @FXML
    private Label inventoryValueLabel;

    public ReportController(
            SceneManager sceneManager,
            SessionService sessionService,
            ReportService reportService,
            DailySalesService dailySalesService
    ) {
        super(sceneManager, sessionService);
        this.reportService = reportService;
        this.dailySalesService = dailySalesService;
    }

    @FXML
    private void initialize() {
        productNameColumn.setCellValueFactory(new PropertyValueFactory<>("productName"));
        brandColumn.setCellValueFactory(new PropertyValueFactory<>("brandName"));
        stockColumn.setCellValueFactory(new PropertyValueFactory<>("currentStock"));
        priceColumn.setCellValueFactory(new PropertyValueFactory<>("unitPrice"));
        priceColumn.setCellFactory(column -> moneyCell());
        productTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        salesDatePicker.setValue(LocalDate.now());
        refreshReports();
    }

    @FXML
    private void refreshReports() {
        var products = reportService.activeProducts()
                .stream()
                .map(ProductTableRow::new)
                .toList();
        productTable.getItems().setAll(products);

        BigDecimal total = dailySalesService.calculateTotalForDate(salesDatePicker.getValue());
        int totalStock = products.stream()
                .mapToInt(ProductTableRow::getCurrentStock)
                .sum();
        BigDecimal inventoryValue = products.stream()
                .map(product -> product.getUnitPrice().multiply(BigDecimal.valueOf(product.getCurrentStock())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        totalSalesLabel.setText(MoneyFormat.peso(total));
        activeProductsLabel.setText(String.valueOf(products.size()));
        totalStockLabel.setText(String.valueOf(totalStock));
        inventoryValueLabel.setText(MoneyFormat.peso(inventoryValue));
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
