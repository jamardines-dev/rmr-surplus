package com.inventory.vehicle.dashboard.presentation;

import com.inventory.vehicle.auth.application.SessionService;
import com.inventory.vehicle.common.util.MoneyFormat;
import com.inventory.vehicle.navigation.SceneManager;
import com.inventory.vehicle.navigation.View;
import com.inventory.vehicle.product.application.ProductQueryService;
import com.inventory.vehicle.product.application.ProductResult;
import com.inventory.vehicle.product.presentation.ProductTableRow;
import com.inventory.vehicle.sales.application.DailySalesService;
import com.inventory.vehicle.sales.application.SalesQueryService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
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
    private Label itemsSoldTodayLabel;

    @FXML
    private Label todaySalesLabel;

    @FXML
    private Label transactionsTodayLabel;

    @FXML
    private Label outOfStockLabel;

    @FXML
    private TableView<RecentSaleActivityRow> recentSalesTable;

    @FXML
    private TableColumn<RecentSaleActivityRow, String> sellerColumn;

    @FXML
    private TableColumn<RecentSaleActivityRow, String> activityColumn;

    @FXML
    private TableColumn<RecentSaleActivityRow, BigDecimal> saleTotalColumn;

    @FXML
    private TableColumn<RecentSaleActivityRow, String> soldAtColumn;

    @FXML
    private TableView<EmployeeSummaryTableRow> employeeSummaryTable;

    @FXML
    private TableColumn<EmployeeSummaryTableRow, String> employeeColumn;

    @FXML
    private TableColumn<EmployeeSummaryTableRow, Long> employeeTransactionsColumn;

    @FXML
    private TableColumn<EmployeeSummaryTableRow, Integer> employeeItemsColumn;

    @FXML
    private TableColumn<EmployeeSummaryTableRow, BigDecimal> employeeSalesColumn;

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
        sellerColumn.setCellValueFactory(new PropertyValueFactory<>("sellerName"));
        activityColumn.setCellValueFactory(new PropertyValueFactory<>("activity"));
        saleTotalColumn.setCellValueFactory(new PropertyValueFactory<>("totalAmount"));
        saleTotalColumn.setCellFactory(column -> moneyCell());
        soldAtColumn.setCellValueFactory(new PropertyValueFactory<>("soldAtText"));
        employeeColumn.setCellValueFactory(new PropertyValueFactory<>("employeeName"));
        employeeTransactionsColumn.setCellValueFactory(new PropertyValueFactory<>("transactionsToday"));
        employeeItemsColumn.setCellValueFactory(new PropertyValueFactory<>("itemsSoldToday"));
        employeeSalesColumn.setCellValueFactory(new PropertyValueFactory<>("totalSalesToday"));
        employeeSalesColumn.setCellFactory(column -> moneyCell());
        refreshDashboard();
    }

    @FXML
    private void refreshDashboard() {
        List<ProductResult> products = productQueryService.findActiveProducts();
        LocalDate today = LocalDate.now();
        BigDecimal todaySales = dailySalesService.calculateTotalForDate(today);
        List<RecentSaleActivityRow> todaySaleLines = salesQueryService.findSaleLinesByDate(today)
                .stream()
                .map(RecentSaleActivityRow::new)
                .toList();

        productCountLabel.setText(String.valueOf(products.size()));
        itemsSoldTodayLabel.setText(String.valueOf(todaySaleLines.stream()
                .mapToInt(RecentSaleActivityRow::getQuantitySold)
                .sum()));
        todaySalesLabel.setText(MoneyFormat.peso(todaySales));
        transactionsTodayLabel.setText(String.valueOf(salesQueryService.findSalesByDate(today).size()));
        outOfStockLabel.setText(String.valueOf(productQueryService.findOutOfStockProducts().size()));

        recentSalesTable.getItems().setAll(todaySaleLines.stream().limit(10).toList());
        employeeSummaryTable.getItems().setAll(salesQueryService.summarizeSalesPerEmployee(today)
                .stream()
                .map(EmployeeSummaryTableRow::new)
                .toList());
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
