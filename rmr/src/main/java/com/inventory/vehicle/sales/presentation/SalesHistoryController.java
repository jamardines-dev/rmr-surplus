package com.inventory.vehicle.sales.presentation;

import com.inventory.vehicle.auth.application.SessionService;
import com.inventory.vehicle.common.util.MoneyFormat;
import com.inventory.vehicle.navigation.SceneManager;
import com.inventory.vehicle.navigation.SidebarController;
import com.inventory.vehicle.sales.application.SaleLineResult;
import com.inventory.vehicle.sales.application.SalesQueryService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Dialog;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import org.springframework.stereotype.Controller;

@Controller
public class SalesHistoryController extends SidebarController {

    private final SalesQueryService salesQueryService;

    @FXML
    private DatePicker soldDatePicker;

    @FXML
    private TableView<SaleTableRow> salesTable;

    @FXML
    private TableColumn<SaleTableRow, Long> idColumn;

    @FXML
    private TableColumn<SaleTableRow, String> sellerColumn;

    @FXML
    private TableColumn<SaleTableRow, LocalDate> soldDateColumn;

    @FXML
    private TableColumn<SaleTableRow, BigDecimal> totalColumn;

    @FXML
    private TableColumn<SaleTableRow, String> encodedByColumn;

    @FXML
    private TableColumn<SaleTableRow, String> createdAtColumn;

    public SalesHistoryController(SceneManager sceneManager, SessionService sessionService, SalesQueryService salesQueryService) {
        super(sceneManager, sessionService);
        this.salesQueryService = salesQueryService;
    }

    @FXML
    private void initialize() {
        idColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        sellerColumn.setCellValueFactory(new PropertyValueFactory<>("sellerName"));
        soldDateColumn.setCellValueFactory(new PropertyValueFactory<>("soldDate"));
        totalColumn.setCellValueFactory(new PropertyValueFactory<>("totalAmount"));
        totalColumn.setCellFactory(column -> moneyCell());
        encodedByColumn.setCellValueFactory(new PropertyValueFactory<>("encodedBy"));
        createdAtColumn.setCellValueFactory(new PropertyValueFactory<>("createdAtText"));
        salesTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        salesTable.setRowFactory(table -> {
            var row = new javafx.scene.control.TableRow<SaleTableRow>();
            row.setOnMouseClicked(event -> {
                if (!row.isEmpty() && event.getButton() == MouseButton.PRIMARY && event.getClickCount() == 2) {
                    showEmployeeSales(row.getItem().getSellerName());
                }
            });
            return row;
        });
        loadAllSales();
    }

    @FXML
    private void loadSalesByDate() {
        if (soldDatePicker.getValue() == null) {
            loadAllSales();
            return;
        }

        salesTable.getItems().setAll(salesQueryService.findSalesByDate(soldDatePicker.getValue())
                .stream()
                .map(SaleTableRow::new)
                .toList());
    }

    @FXML
    private void loadAllSales() {
        soldDatePicker.setValue(null);
        salesTable.getItems().setAll(salesQueryService.findAllSales()
                .stream()
                .map(SaleTableRow::new)
                .toList());
    }

    @FXML
    private void openSelectedEmployeeSales() {
        SaleTableRow selectedSale = salesTable.getSelectionModel().getSelectedItem();
        if (selectedSale != null) {
            showEmployeeSales(selectedSale.getSellerName());
        }
    }

    private void showEmployeeSales(String sellerName) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Employee Sales");
        dialog.setHeaderText(sellerName);
        dialog.getDialogPane().getButtonTypes().add(new ButtonType("Close", ButtonBar.ButtonData.CANCEL_CLOSE));

        Label summaryLabel = new Label();
        summaryLabel.setStyle("-fx-font-weight: bold;");

        TableView<EmployeeSaleDetailRow> detailTable = new TableView<>();
        detailTable.setPrefSize(920, 420);
        detailTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);

        TableColumn<EmployeeSaleDetailRow, String> soldAtColumn = new TableColumn<>("Time");
        soldAtColumn.setCellValueFactory(new PropertyValueFactory<>("soldAt"));
        soldAtColumn.setPrefWidth(140);

        TableColumn<EmployeeSaleDetailRow, String> productColumn = new TableColumn<>("Product");
        productColumn.setCellValueFactory(new PropertyValueFactory<>("productName"));
        productColumn.setPrefWidth(210);

        TableColumn<EmployeeSaleDetailRow, String> brandColumn = new TableColumn<>("Brand");
        brandColumn.setCellValueFactory(new PropertyValueFactory<>("brandName"));
        brandColumn.setPrefWidth(120);

        TableColumn<EmployeeSaleDetailRow, String> vehicleTypeColumn = new TableColumn<>("Vehicle Type");
        vehicleTypeColumn.setCellValueFactory(new PropertyValueFactory<>("vehicleTypeName"));
        vehicleTypeColumn.setPrefWidth(130);

        TableColumn<EmployeeSaleDetailRow, String> modelCodeColumn = new TableColumn<>("Model Code");
        modelCodeColumn.setCellValueFactory(new PropertyValueFactory<>("modelCode"));
        modelCodeColumn.setPrefWidth(110);

        TableColumn<EmployeeSaleDetailRow, Integer> quantityColumn = new TableColumn<>("Qty");
        quantityColumn.setCellValueFactory(new PropertyValueFactory<>("quantitySold"));
        quantityColumn.setPrefWidth(70);

        TableColumn<EmployeeSaleDetailRow, BigDecimal> priceColumn = new TableColumn<>("Price");
        priceColumn.setCellValueFactory(new PropertyValueFactory<>("priceSold"));
        priceColumn.setPrefWidth(100);
        priceColumn.setCellFactory(column -> moneyCell());

        TableColumn<EmployeeSaleDetailRow, BigDecimal> totalColumn = new TableColumn<>("Total");
        totalColumn.setCellValueFactory(new PropertyValueFactory<>("totalAmount"));
        totalColumn.setPrefWidth(110);
        totalColumn.setCellFactory(column -> moneyCell());

        detailTable.getColumns().clear();
        detailTable.getColumns().add(soldAtColumn);
        detailTable.getColumns().add(productColumn);
        detailTable.getColumns().add(brandColumn);
        detailTable.getColumns().add(vehicleTypeColumn);
        detailTable.getColumns().add(modelCodeColumn);
        detailTable.getColumns().add(quantityColumn);
        detailTable.getColumns().add(priceColumn);
        detailTable.getColumns().add(totalColumn);

        Button todayButton = new Button("Today");
        Button weekButton = new Button("Past 7 Days");
        Button monthButton = new Button("This Month");
        Button allButton = new Button("All");

        todayButton.setOnAction(event -> loadEmployeeSalesForDateRange(sellerName, LocalDate.now(), LocalDate.now(), detailTable, summaryLabel));
        weekButton.setOnAction(event -> loadEmployeeSalesForDateRange(sellerName, LocalDate.now().minusDays(6), LocalDate.now(), detailTable, summaryLabel));
        monthButton.setOnAction(event -> loadEmployeeSalesForDateRange(sellerName, LocalDate.now().withDayOfMonth(1), LocalDate.now(), detailTable, summaryLabel));
        allButton.setOnAction(event -> loadEmployeeSales(sellerName, salesQueryService.findAllSaleLinesForSeller(sellerName), detailTable, summaryLabel));

        HBox filters = new HBox(10, todayButton, weekButton, monthButton, allButton);
        VBox content = new VBox(12, filters, summaryLabel, detailTable);
        dialog.getDialogPane().setContent(content);

        loadEmployeeSalesForDateRange(sellerName, LocalDate.now(), LocalDate.now(), detailTable, summaryLabel);
        dialog.showAndWait();
    }

    private void loadEmployeeSalesForDateRange(
            String sellerName,
            LocalDate startDate,
            LocalDate endDate,
            TableView<EmployeeSaleDetailRow> detailTable,
            Label summaryLabel
    ) {
        loadEmployeeSales(
                sellerName,
                salesQueryService.findSaleLinesForSellerBetween(sellerName, startDate, endDate),
                detailTable,
                summaryLabel
        );
    }

    private void loadEmployeeSales(
            String sellerName,
            List<SaleLineResult> saleLines,
            TableView<EmployeeSaleDetailRow> detailTable,
            Label summaryLabel
    ) {
        detailTable.setItems(FXCollections.observableArrayList(saleLines.stream()
                .map(EmployeeSaleDetailRow::new)
                .toList()));
        int itemsSold = saleLines.stream()
                .mapToInt(SaleLineResult::quantitySold)
                .sum();
        BigDecimal total = saleLines.stream()
                .map(SaleLineResult::totalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        long transactions = saleLines.stream()
                .map(SaleLineResult::saleId)
                .distinct()
                .count();
        summaryLabel.setText(sellerName
                + " | Transactions: " + transactions
                + " | Items: " + itemsSold
                + " | Sales: " + MoneyFormat.peso(total));
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
