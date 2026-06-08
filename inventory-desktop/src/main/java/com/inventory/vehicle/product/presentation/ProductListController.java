package com.inventory.vehicle.product.presentation;

import com.inventory.vehicle.common.exception.BusinessException;
import com.inventory.vehicle.auth.application.SessionService;
import com.inventory.vehicle.common.util.MoneyFormat;
import com.inventory.vehicle.navigation.SceneManager;
import com.inventory.vehicle.navigation.SidebarController;
import com.inventory.vehicle.product.application.CreateProductCommand;
import com.inventory.vehicle.product.application.ProductQueryService;
import com.inventory.vehicle.product.application.ProductResult;
import com.inventory.vehicle.product.application.ProductService;
import com.inventory.vehicle.product.application.UpdateProductCommand;
import java.math.BigDecimal;
import java.util.List;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import org.springframework.stereotype.Controller;

@Controller
public class ProductListController extends SidebarController {

    private final ProductQueryService productQueryService;
    private final ProductService productService;
    private List<ProductTableRow> allProducts = List.of();

    @FXML
    private TableView<ProductTableRow> productTable;

    @FXML
    private TableColumn<ProductTableRow, Long> idColumn;

    @FXML
    private TableColumn<ProductTableRow, String> productNameColumn;

    @FXML
    private TableColumn<ProductTableRow, String> brandColumn;

    @FXML
    private TableColumn<ProductTableRow, String> vehicleTypeColumn;

    @FXML
    private TableColumn<ProductTableRow, String> modelCodeColumn;

    @FXML
    private TableColumn<ProductTableRow, Integer> stockColumn;

    @FXML
    private TableColumn<ProductTableRow, BigDecimal> priceColumn;

    @FXML
    private TextField searchField;

    @FXML
    private TextField productNameField;

    @FXML
    private TextField brandField;

    @FXML
    private TextField vehicleTypeField;

    @FXML
    private TextField modelCodeField;

    @FXML
    private TextField stockQuantityField;

    @FXML
    private TextField priceField;

    @FXML
    private Label messageLabel;

    public ProductListController(
            SceneManager sceneManager,
            SessionService sessionService,
            ProductQueryService productQueryService,
            ProductService productService) {
        super(sceneManager, sessionService);
        this.productQueryService = productQueryService;
        this.productService = productService;
    }

    @FXML
    private void initialize() {
        idColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        productNameColumn.setCellValueFactory(new PropertyValueFactory<>("productName"));
        brandColumn.setCellValueFactory(new PropertyValueFactory<>("brandName"));
        vehicleTypeColumn.setCellValueFactory(new PropertyValueFactory<>("vehicleTypeName"));
        modelCodeColumn.setCellValueFactory(new PropertyValueFactory<>("modelCode"));
        stockColumn.setCellValueFactory(new PropertyValueFactory<>("currentStock"));
        priceColumn.setCellValueFactory(new PropertyValueFactory<>("unitPrice"));
        priceColumn.setCellFactory(column -> moneyCell());
        productTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);

        productTable.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, selectedProduct) -> {
            if (selectedProduct != null) {
                fillForm(selectedProduct);
                messageLabel.setText("Selected " + selectedProduct.getProductName()
                        + " for product management and stock updates.");
            }
        });
        searchField.textProperty().addListener((observable, oldValue, newValue) -> applySearch());

        refreshProducts();
    }

    @FXML
    private void createProduct() {
        try {
            productService.createProduct(toCreateCommand());
            resetForm();
            refreshProducts();
            messageLabel.setText("Product created.");
        } catch (BusinessException | NumberFormatException exception) {
            messageLabel.setText(exception.getMessage());
        }
    }

    @FXML
    private void updateProduct() {
        ProductTableRow selectedProduct = productTable.getSelectionModel().getSelectedItem();
        if (selectedProduct == null) {
            messageLabel.setText("Select a product to update.");
            return;
        }

        try {
            productService.updateProduct(toUpdateCommand(selectedProduct.getId()));
            resetForm();
            refreshProducts();
            messageLabel.setText("Product updated.");
        } catch (BusinessException | NumberFormatException exception) {
            messageLabel.setText(exception.getMessage());
        }
    }

    @FXML
    private void deleteProduct() {
        ProductTableRow selectedProduct = productTable.getSelectionModel().getSelectedItem();
        if (selectedProduct == null) {
            messageLabel.setText("Select a product to delete.");
            return;
        }

        if (!confirm("Delete product", "Delete " + selectedProduct.getProductName() + "?")) {
            return;
        }

        try {
            productService.deleteProduct(selectedProduct.getId());
            resetForm();
            refreshProducts();
            messageLabel.setText("Product deleted.");
        } catch (BusinessException exception) {
            messageLabel.setText(exception.getMessage());
        }
    }

    @FXML
    private void clearForm() {
        if (!confirm("Clear fields", "Clear the current product fields?")) {
            return;
        }
        resetForm();
        messageLabel.setText("Fields cleared.");
    }

    private void resetForm() {
        productTable.getSelectionModel().clearSelection();
        productNameField.clear();
        brandField.clear();
        vehicleTypeField.clear();
        modelCodeField.clear();
        stockQuantityField.clear();
        priceField.clear();
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

    private void refreshProducts() {
        allProducts = productQueryService.findActiveProducts()
                .stream()
                .map(ProductTableRow::new)
                .toList();
        applySearch();
    }

    @FXML
    private void clearSearch() {
        searchField.clear();
        applySearch();
    }

    @FXML
    private void searchProduct() {
        applySearch();
        if (productTable.getItems().isEmpty()) {
            messageLabel.setText("No product found.");
            return;
        }

        productTable.getSelectionModel().selectFirst();
        productTable.scrollTo(0);
        messageLabel.setText("Product selected for admin management.");
    }

    private void applySearch() {
        String searchText = searchField.getText();
        if (searchText == null || searchText.isBlank()) {
            productTable.getItems().setAll(allProducts);
            return;
        }

        String normalizedSearch = searchText.trim().toLowerCase();
        productTable.getItems().setAll(allProducts.stream()
                .filter(product -> contains(product.getProductName(), normalizedSearch)
                        || contains(product.getBrandName(), normalizedSearch)
                        || contains(product.getVehicleTypeName(), normalizedSearch)
                        || contains(product.getModelCode(), normalizedSearch))
                .toList());
    }

    private boolean contains(String value, String searchText) {
        return value != null && value.toLowerCase().contains(searchText);
    }

    private void fillForm(ProductTableRow product) {
        productNameField.setText(product.getProductName());
        brandField.setText(product.getBrandName());
        vehicleTypeField.setText(product.getVehicleTypeName());
        modelCodeField.setText(product.getModelCode());
        stockQuantityField.setText(String.valueOf(product.getCurrentStock()));
        priceField.setText(product.getUnitPrice().toPlainString());
    }

    private CreateProductCommand toCreateCommand() {
        return new CreateProductCommand(
                productNameField.getText(),
                brandField.getText(),
                vehicleTypeField.getText(),
                modelCodeField.getText(),
                parseInteger(stockQuantityField.getText(), "Stock quantity"),
                parsePrice());
    }

    private UpdateProductCommand toUpdateCommand(Long productId) {
        ProductTableRow selectedProduct = productTable.getSelectionModel().getSelectedItem();
        return new UpdateProductCommand(
                productId,
                productNameField.getText(),
                brandField.getText(),
                vehicleTypeField.getText(),
                modelCodeField.getText(),
                parseInteger(stockQuantityField.getText(), "Stock quantity"),
                parsePrice());
    }

    private int parseInteger(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new NumberFormatException(fieldName + " is required.");
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException exception) {
            if (exception.getMessage() != null && exception.getMessage().endsWith("is required.")) {
                throw exception;
            }
            throw new NumberFormatException(fieldName + " must be a whole number.");
        }
    }

    private BigDecimal parsePrice() {
        if (priceField.getText() == null || priceField.getText().isBlank()) {
            throw new NumberFormatException("Price is required.");
        }
        try {
            return new BigDecimal(priceField.getText().trim());
        } catch (NumberFormatException exception) {
            if (exception.getMessage() != null && exception.getMessage().equals("Price is required.")) {
                throw exception;
            }
            throw new NumberFormatException("Price must be a valid number.");
        }
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
