package com.inventory.vehicle.inventory.presentation;

import com.inventory.vehicle.auth.application.SessionService;
import com.inventory.vehicle.common.exception.BusinessException;
import com.inventory.vehicle.common.util.MoneyFormat;
import com.inventory.vehicle.inventory.application.InventoryQueryService;
import com.inventory.vehicle.inventory.domain.StockMovement;
import com.inventory.vehicle.navigation.SceneManager;
import com.inventory.vehicle.navigation.SidebarController;
import com.inventory.vehicle.product.application.ProductService;
import com.inventory.vehicle.product.application.UpdateDrRestockLineCommand;
import com.inventory.vehicle.product.presentation.ProductTableRow;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import org.springframework.stereotype.Controller;

@Controller
public class InventoryController extends SidebarController {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd hh:mm a");

    private final InventoryQueryService inventoryQueryService;
    private final ProductService productService;

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
    private TableView<DrRestockGroupRow> drRestockTable;

    @FXML
    private TableColumn<DrRestockGroupRow, String> drNumberColumn;

    @FXML
    private TableColumn<DrRestockGroupRow, Integer> drItemCountColumn;

    @FXML
    private TableColumn<DrRestockGroupRow, Integer> drTotalQuantityColumn;

    @FXML
    private TableColumn<DrRestockGroupRow, String> drDateColumn;

    @FXML
    private TableColumn<DrRestockGroupRow, String> drUserColumn;

    @FXML
    private Label activeProductsLabel;

    @FXML
    private Label totalStockLabel;

    @FXML
    private Label inventoryValueLabel;

    @FXML
    private Label outOfStockLabel;

    public InventoryController(
            SceneManager sceneManager,
            SessionService sessionService,
            InventoryQueryService inventoryQueryService,
            ProductService productService) {
        super(sceneManager, sessionService);
        this.inventoryQueryService = inventoryQueryService;
        this.productService = productService;
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
        drNumberColumn.setCellValueFactory(new PropertyValueFactory<>("drNumber"));
        drItemCountColumn.setCellValueFactory(new PropertyValueFactory<>("itemCount"));
        drTotalQuantityColumn.setCellValueFactory(new PropertyValueFactory<>("totalQuantity"));
        drDateColumn.setCellValueFactory(new PropertyValueFactory<>("createdAt"));
        drUserColumn.setCellValueFactory(new PropertyValueFactory<>("createdBy"));
        drRestockTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        drRestockTable.setRowFactory(table -> {
            var row = new javafx.scene.control.TableRow<DrRestockGroupRow>();
            row.setOnMouseClicked(event -> {
                if (!row.isEmpty() && event.getButton() == MouseButton.PRIMARY && event.getClickCount() == 2) {
                    openDrRestockDetails(row.getItem());
                }
            });
            return row;
        });
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

        drRestockTable.getItems().setAll(groupDrRestocks(inventoryQueryService.findDrRestockMovements()));
    }

    private List<DrRestockGroupRow> groupDrRestocks(List<StockMovement> movements) {
        Map<String, List<StockMovement>> movementsByDr = new LinkedHashMap<>();
        for (StockMovement movement : movements) {
            movementsByDr.computeIfAbsent(movement.getReferenceId(), key -> new ArrayList<>()).add(movement);
        }
        return movementsByDr.entrySet().stream()
                .map(entry -> new DrRestockGroupRow(entry.getKey(), entry.getValue()))
                .toList();
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

    private void openDrRestockDetails(DrRestockGroupRow groupRow) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("DR " + groupRow.getDrNumber());
        dialog.setHeaderText("Products under DR " + groupRow.getDrNumber());
        dialog.initOwner(drRestockTable.getScene().getWindow());
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);

        TableView<DrRestockLineRow> detailsTable = createDrDetailsTable();
        detailsTable.getItems().setAll(groupRow.getLines());

        Button editButton = new Button("Edit Selected");
        Button deleteButton = new Button("Delete Selected");
        deleteButton.getStyleClass().add("danger");
        Label actionMessage = new Label();
        actionMessage.getStyleClass().add("message");

        editButton.setOnAction(event -> {
            DrRestockLineRow selectedLine = detailsTable.getSelectionModel().getSelectedItem();
            if (selectedLine == null) {
                actionMessage.setText("Select a product to edit.");
                return;
            }
            if (openEditDrLineModal(selectedLine)) {
                reloadDrDetails(groupRow.getDrNumber(), detailsTable);
                refreshInventory();
                actionMessage.setText("DR product updated.");
            }
        });
        deleteButton.setOnAction(event -> {
            DrRestockLineRow selectedLine = detailsTable.getSelectionModel().getSelectedItem();
            if (selectedLine == null) {
                actionMessage.setText("Select a product to delete.");
                return;
            }
            if (!confirm("Delete DR product", "Delete " + selectedLine.getProductName() + " from DR " + groupRow.getDrNumber() + "?")) {
                return;
            }
            try {
                productService.deleteDrRestockLine(selectedLine.getMovementId());
                boolean hasRows = reloadDrDetails(groupRow.getDrNumber(), detailsTable);
                refreshInventory();
                actionMessage.setText("DR product deleted.");
                if (!hasRows) {
                    dialog.close();
                }
            } catch (BusinessException exception) {
                actionMessage.setText(exception.getMessage());
            }
        });

        HBox actions = new HBox(10, editButton, deleteButton);
        actions.getStyleClass().add("modal-actions");
        VBox content = new VBox(12);
        content.setPadding(new Insets(8, 0, 0, 0));
        content.getStyleClass().add("modal-content");
        content.getChildren().addAll(actions, detailsTable, actionMessage);
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().setPrefWidth(940);
        dialog.getDialogPane().setPrefHeight(540);
        dialog.showAndWait();
    }

    private boolean reloadDrDetails(String drNumber, TableView<DrRestockLineRow> detailsTable) {
        DrRestockGroupRow refreshedGroup = groupDrRestocks(inventoryQueryService.findDrRestockMovements())
                .stream()
                .filter(group -> group.getDrNumber().equals(drNumber))
                .findFirst()
                .orElse(null);
        if (refreshedGroup == null) {
            detailsTable.getItems().clear();
            return false;
        }
        detailsTable.getItems().setAll(refreshedGroup.getLines());
        return !refreshedGroup.getLines().isEmpty();
    }

    private boolean openEditDrLineModal(DrRestockLineRow line) {
        TextField productField = new TextField(line.getProductName());
        TextField brandField = new TextField(line.getBrandName());
        TextField vehicleTypeField = new TextField(line.getVehicleTypeName());
        TextField modelCodeField = new TextField(line.getModelCode());
        TextField quantityField = new TextField(String.valueOf(line.getQuantity()));
        TextField priceField = new TextField(line.getUnitPrice().toPlainString());
        productField.setDisable(true);
        brandField.setDisable(true);
        vehicleTypeField.setDisable(true);
        modelCodeField.setDisable(true);
        priceField.setDisable(true);
        Label errorLabel = new Label();
        errorLabel.getStyleClass().add("message");

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Edit Restock Quantity");
        dialog.setHeaderText("Adjust the quantity recorded for this DR line.");
        dialog.initOwner(drRestockTable.getScene().getWindow());
        ButtonType saveButtonType = new ButtonType("Save Changes", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

        HBox quantityPriceRow = new HBox(10);
        quantityPriceRow.getChildren().addAll(
                labeledField("Restock Quantity", quantityField),
                labeledField("Default Price", priceField));

        VBox content = new VBox(12);
        content.setPadding(new Insets(8, 0, 0, 0));
        content.getStyleClass().add("modal-content");
        content.getChildren().addAll(
                labeledField("Product", productField),
                labeledField("Brand", brandField),
                labeledField("Vehicle", vehicleTypeField),
                labeledField("Model", modelCodeField),
                quantityPriceRow,
                errorLabel);
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().setPrefWidth(540);

        boolean[] saved = {false};
        Button saveButton = (Button) dialog.getDialogPane().lookupButton(saveButtonType);
        saveButton.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
            try {
                productService.updateDrRestockLine(new UpdateDrRestockLineCommand(
                        line.getMovementId(),
                        productField.getText(),
                        brandField.getText(),
                        vehicleTypeField.getText(),
                        modelCodeField.getText(),
                        parseWholeNumber(quantityField.getText(), "Restock quantity"),
                        line.getUnitPrice()));
                saved[0] = true;
            } catch (BusinessException | NumberFormatException exception) {
                event.consume();
                errorLabel.setText(exception.getMessage());
            }
        });

        dialog.showAndWait();
        return saved[0];
    }

    private VBox labeledField(String labelText, javafx.scene.Node field) {
        VBox wrapper = new VBox(6);
        Label label = new Label(labelText);
        label.getStyleClass().add("field-label");
        wrapper.getChildren().addAll(label, field);
        return wrapper;
    }

    private boolean confirm(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        return alert.showAndWait()
                .filter(response -> response == ButtonType.OK)
                .isPresent();
    }

    private int parseWholeNumber(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new NumberFormatException(fieldName + " is required.");
        }
        try {
            int parsedValue = Integer.parseInt(value.trim());
            if (parsedValue <= 0) {
                throw new NumberFormatException();
            }
            return parsedValue;
        } catch (NumberFormatException exception) {
            throw new NumberFormatException(fieldName + " must be a whole number greater than 0.");
        }
    }

    private TableView<DrRestockLineRow> createDrDetailsTable() {
        TableView<DrRestockLineRow> table = new TableView<>();
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        table.setPrefHeight(420);

        TableColumn<DrRestockLineRow, String> productColumn = new TableColumn<>("Product");
        productColumn.setCellValueFactory(new PropertyValueFactory<>("productName"));
        productColumn.setPrefWidth(220);

        TableColumn<DrRestockLineRow, String> brandColumn = new TableColumn<>("Brand");
        brandColumn.setCellValueFactory(new PropertyValueFactory<>("brandName"));
        brandColumn.setPrefWidth(130);

        TableColumn<DrRestockLineRow, String> modelColumn = new TableColumn<>("Model");
        modelColumn.setCellValueFactory(new PropertyValueFactory<>("modelCode"));
        modelColumn.setPrefWidth(130);

        TableColumn<DrRestockLineRow, Integer> quantityColumn = new TableColumn<>("Qty");
        quantityColumn.setCellValueFactory(new PropertyValueFactory<>("quantity"));
        quantityColumn.setPrefWidth(90);

        TableColumn<DrRestockLineRow, Integer> newStockColumn = new TableColumn<>("New Stock");
        newStockColumn.setCellValueFactory(new PropertyValueFactory<>("newStock"));
        newStockColumn.setPrefWidth(110);

        TableColumn<DrRestockLineRow, BigDecimal> priceColumn = new TableColumn<>("Price");
        priceColumn.setCellValueFactory(new PropertyValueFactory<>("unitPrice"));
        priceColumn.setCellFactory(column -> moneyCell());
        priceColumn.setPrefWidth(120);

        TableColumn<DrRestockLineRow, String> dateColumn = new TableColumn<>("Date");
        dateColumn.setCellValueFactory(new PropertyValueFactory<>("createdAt"));
        dateColumn.setPrefWidth(170);

        TableColumn<DrRestockLineRow, String> userColumn = new TableColumn<>("User");
        userColumn.setCellValueFactory(new PropertyValueFactory<>("createdBy"));
        userColumn.setPrefWidth(130);

        table.getColumns().add(productColumn);
        table.getColumns().add(brandColumn);
        table.getColumns().add(modelColumn);
        table.getColumns().add(quantityColumn);
        table.getColumns().add(newStockColumn);
        table.getColumns().add(priceColumn);
        table.getColumns().add(dateColumn);
        table.getColumns().add(userColumn);
        return table;
    }

    public static class DrRestockGroupRow {

        private final String drNumber;
        private final int itemCount;
        private final int totalQuantity;
        private final String createdAt;
        private final String createdBy;
        private final List<DrRestockLineRow> lines;

        public DrRestockGroupRow(String drNumber, List<StockMovement> movements) {
            this.drNumber = drNumber;
            this.lines = movements.stream()
                    .map(DrRestockLineRow::new)
                    .toList();
            this.itemCount = lines.size();
            this.totalQuantity = lines.stream()
                    .mapToInt(DrRestockLineRow::getQuantity)
                    .sum();
            DrRestockLineRow firstLine = lines.isEmpty() ? null : lines.getFirst();
            this.createdAt = firstLine == null ? "" : firstLine.getCreatedAt();
            this.createdBy = firstLine == null ? "" : firstLine.getCreatedBy();
        }

        public String getDrNumber() {
            return drNumber;
        }

        public int getItemCount() {
            return itemCount;
        }

        public int getTotalQuantity() {
            return totalQuantity;
        }

        public String getCreatedAt() {
            return createdAt;
        }

        public String getCreatedBy() {
            return createdBy;
        }

        public List<DrRestockLineRow> getLines() {
            return lines;
        }
    }

    public static class DrRestockLineRow {

        private final Long movementId;
        private final String productName;
        private final String brandName;
        private final String vehicleTypeName;
        private final String modelCode;
        private final int quantity;
        private final int previousStock;
        private final int newStock;
        private final BigDecimal unitPrice;
        private final String createdAt;
        private final String createdBy;

        public DrRestockLineRow(StockMovement movement) {
            this.movementId = movement.getId();
            this.productName = movement.getProduct().getProductName();
            this.brandName = movement.getProduct().getBrand().getName();
            this.vehicleTypeName = movement.getProduct().getVehicleType().getName();
            this.modelCode = movement.getProduct().getModelCode();
            this.quantity = movement.getQuantity();
            this.previousStock = movement.getPreviousStock();
            this.newStock = movement.getNewStock();
            this.unitPrice = movement.getProduct().getUnitPrice();
            this.createdAt = movement.getCreatedAt() == null ? "" : movement.getCreatedAt().format(DATE_TIME_FORMATTER);
            this.createdBy = movement.getCreatedBy();
        }

        public Long getMovementId() {
            return movementId;
        }

        public String getProductName() {
            return productName;
        }

        public String getBrandName() {
            return brandName;
        }

        public String getVehicleTypeName() {
            return vehicleTypeName;
        }

        public String getModelCode() {
            return modelCode;
        }

        public int getQuantity() {
            return quantity;
        }

        public int getPreviousStock() {
            return previousStock;
        }

        public int getNewStock() {
            return newStock;
        }

        public BigDecimal getUnitPrice() {
            return unitPrice;
        }

        public String getCreatedAt() {
            return createdAt;
        }

        public String getCreatedBy() {
            return createdBy;
        }
    }
}
