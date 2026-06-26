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
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Dialog;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import org.springframework.stereotype.Controller;

@Controller
public class SalesHistoryController extends SidebarController {

    private final SalesQueryService salesQueryService;
    private List<SaleTableRow> currentSales = List.of();

    @FXML
    private DatePicker soldDatePicker;

    @FXML
    private ComboBox<String> salesFilterComboBox;

    @FXML
    private TextField searchField;

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

    public SalesHistoryController(SceneManager sceneManager, SessionService sessionService,
            SalesQueryService salesQueryService) {
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
        searchField.textProperty().addListener((observable, oldValue, newValue) -> applySearch());
        salesFilterComboBox.getItems().setAll("All", "Today", "Week", "Month", "Date");
        salesFilterComboBox.setValue("All");
        salesFilterComboBox.valueProperty().addListener((observable, oldValue, newValue) -> applySalesFilter());
        soldDatePicker.setOnAction(event -> {
            salesFilterComboBox.setValue("Date");
            applySalesFilter();
        });
        applySalesFilter();
    }

    private void applySalesFilter() {
        String filter = salesFilterComboBox.getValue();
        if ("Today".equals(filter)) {
            loadTodaySales();
        } else if ("Week".equals(filter)) {
            loadWeeklySales();
        } else if ("Month".equals(filter)) {
            loadMonthlySales();
        } else if ("Date".equals(filter)) {
            loadSalesByDate();
        } else {
            loadAllSales();
        }
    }

    @FXML
    private void loadSalesByDate() {
        if (soldDatePicker.getValue() == null) {
            loadAllSales();
            return;
        }

        setSales(salesQueryService.findSalesByDate(soldDatePicker.getValue())
                .stream()
                .map(SaleTableRow::new)
                .toList());
    }

    @FXML
    private void loadAllSales() {
        soldDatePicker.setValue(null);
        setSales(salesQueryService.findAllSales()
                .stream()
                .map(SaleTableRow::new)
                .toList());
    }

    @FXML
    private void loadTodaySales() {
        LocalDate today = LocalDate.now();
        soldDatePicker.setValue(today);
        setSales(salesQueryService.findSalesByDate(today)
                .stream()
                .map(SaleTableRow::new)
                .toList());
    }

    @FXML
    private void loadWeeklySales() {
        LocalDate today = LocalDate.now();
        soldDatePicker.setValue(null);
        setSales(salesQueryService.findSalesBetween(today.minusDays(6), today)
                .stream()
                .map(SaleTableRow::new)
                .toList());
    }

    @FXML
    private void loadMonthlySales() {
        LocalDate today = LocalDate.now();
        soldDatePicker.setValue(null);
        setSales(salesQueryService.findSalesBetween(today.withDayOfMonth(1), today)
                .stream()
                .map(SaleTableRow::new)
                .toList());
    }

    @FXML
    private void searchSales() {
        applySearch();
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

        TableColumn<EmployeeSaleDetailRow, Image> imageColumn = new TableColumn<>("Photo");
        imageColumn.setCellValueFactory(new PropertyValueFactory<>("image"));
        imageColumn.setPrefWidth(80);
        imageColumn.setCellFactory(column -> imageCell());

        TableColumn<EmployeeSaleDetailRow, String> soldAtColumn = new TableColumn<>("Time");
        soldAtColumn.setCellValueFactory(new PropertyValueFactory<>("soldAt"));
        soldAtColumn.setPrefWidth(140);

        TableColumn<EmployeeSaleDetailRow, String> productColumn = new TableColumn<>("Product");
        productColumn.setCellValueFactory(new PropertyValueFactory<>("productName"));
        productColumn.setPrefWidth(210);

        TableColumn<EmployeeSaleDetailRow, String> brandColumn = new TableColumn<>("Brand");
        brandColumn.setCellValueFactory(new PropertyValueFactory<>("brandName"));
        brandColumn.setPrefWidth(120);

        TableColumn<EmployeeSaleDetailRow, String> vehicleTypeColumn = new TableColumn<>("Vehicle");
        vehicleTypeColumn.setCellValueFactory(new PropertyValueFactory<>("vehicleTypeName"));
        vehicleTypeColumn.setPrefWidth(130);

        TableColumn<EmployeeSaleDetailRow, String> modelCodeColumn = new TableColumn<>("Model");
        modelCodeColumn.setCellValueFactory(new PropertyValueFactory<>("modelCode"));
        modelCodeColumn.setPrefWidth(110);

        TableColumn<EmployeeSaleDetailRow, String> stockNumberColumn = new TableColumn<>("Stock#");
        stockNumberColumn.setCellValueFactory(new PropertyValueFactory<>("stockNumber"));
        stockNumberColumn.setPrefWidth(85);

        TableColumn<EmployeeSaleDetailRow, Integer> quantityColumn = new TableColumn<>("Qty");
        quantityColumn.setCellValueFactory(new PropertyValueFactory<>("quantitySold"));
        quantityColumn.setPrefWidth(50);

        TableColumn<EmployeeSaleDetailRow, BigDecimal> defaultPriceColumn = new TableColumn<>("Default Price");
        defaultPriceColumn.setCellValueFactory(new PropertyValueFactory<>("originalPrice"));
        defaultPriceColumn.setPrefWidth(100);
        defaultPriceColumn.setCellFactory(column -> moneyCell());

        TableColumn<EmployeeSaleDetailRow, BigDecimal> priceColumn = new TableColumn<>("Sold Price");
        priceColumn.setCellValueFactory(new PropertyValueFactory<>("priceSold"));
        priceColumn.setPrefWidth(90);
        priceColumn.setCellFactory(column -> moneyCell());

        TableColumn<EmployeeSaleDetailRow, BigDecimal> totalColumn = new TableColumn<>("Total");
        totalColumn.setCellValueFactory(new PropertyValueFactory<>("totalAmount"));
        totalColumn.setPrefWidth(100);
        totalColumn.setCellFactory(column -> moneyCell());

        detailTable.getColumns().clear();
        detailTable.getColumns().add(imageColumn);
        detailTable.getColumns().add(soldAtColumn);
        detailTable.getColumns().add(productColumn);
        detailTable.getColumns().add(brandColumn);
        detailTable.getColumns().add(vehicleTypeColumn);
        detailTable.getColumns().add(modelCodeColumn);
        detailTable.getColumns().add(stockNumberColumn);
        detailTable.getColumns().add(quantityColumn);
        detailTable.getColumns().add(defaultPriceColumn);
        detailTable.getColumns().add(priceColumn);
        detailTable.getColumns().add(totalColumn);

        ComboBox<String> employeeFilterComboBox = new ComboBox<>();
        employeeFilterComboBox.getItems().setAll("Today", "Week", "Month", "All");
        employeeFilterComboBox.setValue("Today");
        employeeFilterComboBox.setPrefWidth(150);
        employeeFilterComboBox.valueProperty().addListener((observable, oldValue, filter) ->
                applyEmployeeSalesFilter(filter, sellerName, detailTable, summaryLabel));

        HBox filters = new HBox(10, new Label("Filter"), employeeFilterComboBox);
        VBox content = new VBox(12, filters, summaryLabel, detailTable);
        dialog.getDialogPane().setContent(content);

        applyEmployeeSalesFilter("Today", sellerName, detailTable, summaryLabel);
        dialog.showAndWait();
    }

    private void applyEmployeeSalesFilter(
            String filter,
            String sellerName,
            TableView<EmployeeSaleDetailRow> detailTable,
            Label summaryLabel) {
        if ("Week".equals(filter)) {
            loadEmployeeSalesForDateRange(sellerName, LocalDate.now().minusDays(6), LocalDate.now(), detailTable, summaryLabel);
        } else if ("Month".equals(filter)) {
            loadEmployeeSalesForDateRange(sellerName, LocalDate.now().withDayOfMonth(1), LocalDate.now(), detailTable, summaryLabel);
        } else if ("All".equals(filter)) {
            loadEmployeeSales(sellerName, salesQueryService.findAllSaleLinesForSeller(sellerName), detailTable, summaryLabel);
        } else {
            loadEmployeeSalesForDateRange(sellerName, LocalDate.now(), LocalDate.now(), detailTable, summaryLabel);
        }
    }

    private void setSales(List<SaleTableRow> sales) {
        currentSales = sales;
        applySearch();
    }

    private void applySearch() {
        String searchText = searchField.getText();
        if (searchText == null || searchText.isBlank()) {
            salesTable.getItems().setAll(currentSales);
            return;
        }

        String normalizedSearch = searchText.trim().toLowerCase();
        salesTable.getItems().setAll(currentSales.stream()
                .filter(sale -> contains(String.valueOf(sale.getId()), normalizedSearch)
                        || contains(sale.getSellerName(), normalizedSearch)
                        || contains(sale.getEncodedBy(), normalizedSearch)
                        || contains(sale.getSoldDate().toString(), normalizedSearch))
                .toList());
    }

    private boolean contains(String value, String searchText) {
        return value != null && value.toLowerCase().contains(searchText);
    }

    private void loadEmployeeSalesForDateRange(
            String sellerName,
            LocalDate startDate,
            LocalDate endDate,
            TableView<EmployeeSaleDetailRow> detailTable,
            Label summaryLabel) {
        loadEmployeeSales(
                sellerName,
                salesQueryService.findSaleLinesForSellerBetween(sellerName, startDate, endDate),
                detailTable,
                summaryLabel);
    }

    private void loadEmployeeSales(
            String sellerName,
            List<SaleLineResult> saleLines,
            TableView<EmployeeSaleDetailRow> detailTable,
            Label summaryLabel) {
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

    private <S> TableCell<S, Image> imageCell() {
        return new TableCell<>() {
            private final ImageView imageView = new ImageView();

            {
                imageView.setFitWidth(52);
                imageView.setFitHeight(42);
                imageView.setPreserveRatio(true);
            }

            @Override
            protected void updateItem(Image image, boolean empty) {
                super.updateItem(image, empty);
                if (empty || image == null) {
                    setGraphic(null);
                    return;
                }
                imageView.setImage(image);
                setGraphic(imageView);
            }
        };
    }

    @FXML
    private void backToDashboard() {
        showDashboard();
    }
}
