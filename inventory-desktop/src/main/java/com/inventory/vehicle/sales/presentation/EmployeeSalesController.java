package com.inventory.vehicle.sales.presentation;

import com.inventory.vehicle.auth.application.SessionService;
import com.inventory.vehicle.common.exception.BusinessException;
import com.inventory.vehicle.common.util.MoneyFormat;
import com.inventory.vehicle.navigation.SceneManager;
import com.inventory.vehicle.navigation.SidebarController;
import com.inventory.vehicle.sales.application.RecordSaleService;
import com.inventory.vehicle.sales.application.SalesQueryService;
import com.inventory.vehicle.dashboard.presentation.EmployeeSaleTableRow;
import java.math.BigDecimal;
import java.time.LocalDate;
import javafx.fxml.FXML;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import org.springframework.stereotype.Controller;

@Controller
public class EmployeeSalesController extends SidebarController {

    private final SalesQueryService salesQueryService;
    private final RecordSaleService recordSaleService;

    @FXML
    private Label summaryLabel;

    @FXML
    private TableView<EmployeeSaleTableRow> mySalesTable;

    @FXML
    private TableColumn<EmployeeSaleTableRow, String> saleTimeColumn;

    @FXML
    private TableColumn<EmployeeSaleTableRow, String> saleProductColumn;

    @FXML
    private TableColumn<EmployeeSaleTableRow, String> saleBrandColumn;

    @FXML
    private TableColumn<EmployeeSaleTableRow, String> saleVehicleTypeColumn;

    @FXML
    private TableColumn<EmployeeSaleTableRow, String> saleModelCodeColumn;

    @FXML
    private TableColumn<EmployeeSaleTableRow, Integer> saleQuantityColumn;

    @FXML
    private TableColumn<EmployeeSaleTableRow, BigDecimal> salePriceColumn;

    @FXML
    private TableColumn<EmployeeSaleTableRow, BigDecimal> saleTotalColumn;

    @FXML
    private Label messageLabel;

    public EmployeeSalesController(
            SceneManager sceneManager,
            SessionService sessionService,
            SalesQueryService salesQueryService,
            RecordSaleService recordSaleService
    ) {
        super(sceneManager, sessionService);
        this.salesQueryService = salesQueryService;
        this.recordSaleService = recordSaleService;
    }

    @FXML
    private void initialize() {
        saleTimeColumn.setCellValueFactory(new PropertyValueFactory<>("time"));
        saleProductColumn.setCellValueFactory(new PropertyValueFactory<>("productName"));
        saleBrandColumn.setCellValueFactory(new PropertyValueFactory<>("brandName"));
        saleVehicleTypeColumn.setCellValueFactory(new PropertyValueFactory<>("vehicleTypeName"));
        saleModelCodeColumn.setCellValueFactory(new PropertyValueFactory<>("modelCode"));
        saleQuantityColumn.setCellValueFactory(new PropertyValueFactory<>("quantitySold"));
        salePriceColumn.setCellValueFactory(new PropertyValueFactory<>("priceSold"));
        saleTotalColumn.setCellValueFactory(new PropertyValueFactory<>("totalAmount"));
        salePriceColumn.setCellFactory(column -> moneyCell());
        saleTotalColumn.setCellFactory(column -> moneyCell());
        mySalesTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        mySalesTable.setRowFactory(tableView -> {
            TableRow<EmployeeSaleTableRow> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !row.isEmpty()) {
                    showSaleDetails(row.getItem());
                }
            });
            return row;
        });
        refreshSales();
    }

    @FXML
    private void refreshSales() {
        var sales = salesQueryService.findSaleLinesForSellerByDate(
                sessionService.getCurrentDisplayName(),
                LocalDate.now()
        );
        mySalesTable.getItems().setAll(sales.stream()
                .map(EmployeeSaleTableRow::new)
                .toList());
        int items = sales.stream()
                .mapToInt(sale -> sale.quantitySold())
                .sum();
        BigDecimal total = sales.stream()
                .map(sale -> sale.totalAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        summaryLabel.setText("Transactions today: " + sales.stream().map(sale -> sale.saleId()).distinct().count()
                + " | Items sold: " + items
                + " | Total sales: " + MoneyFormat.peso(total));
    }

    private void showSaleDetails(EmployeeSaleTableRow sale) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Sold Product Details");
        ButtonType undoButton = new ButtonType("Undo This Product");
        dialog.getDialogPane().getButtonTypes().setAll(undoButton, ButtonType.CANCEL);
        dialog.getDialogPane().setContent(createSaleDetailsContent(sale));
        dialog.getDialogPane().getStylesheets().add(getClass().getResource("/css/global.css").toExternalForm());
        dialog.getDialogPane().lookupButton(undoButton).getStyleClass().add("danger");

        if (dialog.showAndWait().filter(buttonType -> buttonType == undoButton).isEmpty()) {
            return;
        }

        undoSaleItem(sale);
    }

    private VBox createSaleDetailsContent(EmployeeSaleTableRow sale) {
        VBox content = new VBox(16);
        content.setMinWidth(460);

        Label title = new Label(sale.getProductName());
        title.getStyleClass().add("title");
        title.setWrapText(true);

        Label subtitle = new Label(sale.getBrandName() + " | " + sale.getModelCode() + " | " + sale.getTime());
        subtitle.getStyleClass().add("subtitle");

        VBox header = new VBox(4);
        header.getChildren().addAll(title, subtitle);

        GridPane details = new GridPane();
        details.setHgap(24);
        details.setVgap(12);
        addDetailRow(details, 0, 0, "Brand", sale.getBrandName());
        addDetailRow(details, 1, 0, "Vehicle Type", sale.getVehicleTypeName());
        addDetailRow(details, 0, 1, "Model Code", sale.getModelCode());
        addDetailRow(details, 1, 1, "Time", sale.getTime());
        addDetailRow(details, 0, 2, "Quantity", String.valueOf(sale.getQuantitySold()));
        addDetailRow(details, 1, 2, "Price", MoneyFormat.peso(sale.getPriceSold()));

        VBox totalCard = new VBox(4);
        totalCard.getStyleClass().add("summary-card");
        Label totalLabel = new Label("Total Amount");
        totalLabel.getStyleClass().add("summary-label");
        Label totalValue = new Label(MoneyFormat.peso(sale.getTotalAmount()));
        totalValue.getStyleClass().add("summary-value");
        totalCard.getChildren().addAll(totalLabel, totalValue);

        HBox receipt = new HBox(14);
        receipt.getChildren().addAll(details, totalCard);

        VBox note = new VBox(4);
        note.getStyleClass().add("section");
        Label noteTitle = new Label("Undo action");
        noteTitle.getStyleClass().add("field-label");
        Label warning = new Label("This restores stock and removes only this sold product from the sale.");
        warning.setWrapText(true);
        warning.getStyleClass().add("subtitle");
        note.getChildren().addAll(noteTitle, warning);

        content.getChildren().addAll(header, receipt, note);
        return content;
    }

    private void addDetailRow(GridPane gridPane, int column, int row, String label, String value) {
        VBox field = new VBox(3);
        field.setMinWidth(140);
        Label labelNode = new Label(label);
        labelNode.getStyleClass().add("field-label");
        Label valueNode = new Label(value == null || value.isBlank() ? "-" : value);
        valueNode.setWrapText(true);
        field.getChildren().addAll(labelNode, valueNode);
        gridPane.add(field, column, row);
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

    private void undoSaleItem(EmployeeSaleTableRow sale) {
        try {
            Long saleId = recordSaleService.undoSaleItemForSeller(
                    sale.getSaleItemId(),
                    sessionService.getCurrentDisplayName()
            );
            refreshSales();
            messageLabel.setText("Selected product undone. Transaction ID: " + saleId);
        } catch (BusinessException exception) {
            messageLabel.setText(exception.getMessage());
        }
    }

}
