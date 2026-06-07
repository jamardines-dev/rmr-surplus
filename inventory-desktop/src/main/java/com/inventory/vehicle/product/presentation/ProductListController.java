package com.inventory.vehicle.product.presentation;

import com.inventory.vehicle.common.exception.BusinessException;
import com.inventory.vehicle.auth.application.SessionService;
import com.inventory.vehicle.navigation.SceneManager;
import com.inventory.vehicle.navigation.SidebarController;
import com.inventory.vehicle.product.application.CreateProductCommand;
import com.inventory.vehicle.product.application.ProductQueryService;
import com.inventory.vehicle.product.application.ProductResult;
import com.inventory.vehicle.product.application.ProductService;
import com.inventory.vehicle.product.application.UpdateProductCommand;
import com.inventory.vehicle.sales.application.CartSaleItemCommand;
import com.inventory.vehicle.sales.application.RecordCartSaleCommand;
import com.inventory.vehicle.sales.application.RecordSaleService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import org.springframework.stereotype.Controller;

@Controller
public class ProductListController extends SidebarController {

    private final ProductQueryService productQueryService;
    private final ProductService productService;
    private final RecordSaleService recordSaleService;
    private List<ProductTableRow> allProducts = List.of();
    private final List<CartItemRow> cartItems = new ArrayList<>();

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
    private TextField restockQuantityField;

    @FXML
    private TextField priceField;

    @FXML
    private TextField sellerNameField;

    @FXML
    private TextField sellQuantityField;

    @FXML
    private Label selectedSellProductLabel;

    @FXML
    private TableView<CartItemRow> cartTable;

    @FXML
    private TableColumn<CartItemRow, String> cartProductColumn;

    @FXML
    private TableColumn<CartItemRow, Integer> cartQuantityColumn;

    @FXML
    private TableColumn<CartItemRow, BigDecimal> cartPriceColumn;

    @FXML
    private TableColumn<CartItemRow, BigDecimal> cartTotalColumn;

    @FXML
    private Label cartTotalLabel;

    @FXML
    private Label messageLabel;

    public ProductListController(
            SceneManager sceneManager,
            SessionService sessionService,
            ProductQueryService productQueryService,
            ProductService productService,
            RecordSaleService recordSaleService
    ) {
        super(sceneManager, sessionService);
        this.productQueryService = productQueryService;
        this.productService = productService;
        this.recordSaleService = recordSaleService;
    }

    @FXML
    private void initialize() {
        idColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        productNameColumn.setCellValueFactory(new PropertyValueFactory<>("productName"));
        brandColumn.setCellValueFactory(new PropertyValueFactory<>("brandName"));
        vehicleTypeColumn.setCellValueFactory(new PropertyValueFactory<>("vehicleTypeName"));
        stockColumn.setCellValueFactory(new PropertyValueFactory<>("currentStock"));
        priceColumn.setCellValueFactory(new PropertyValueFactory<>("unitPrice"));
        cartProductColumn.setCellValueFactory(new PropertyValueFactory<>("productName"));
        cartQuantityColumn.setCellValueFactory(new PropertyValueFactory<>("quantity"));
        cartPriceColumn.setCellValueFactory(new PropertyValueFactory<>("unitPrice"));
        cartTotalColumn.setCellValueFactory(new PropertyValueFactory<>("totalAmount"));

        productTable.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, selectedProduct) -> {
            if (selectedProduct != null) {
                fillForm(selectedProduct);
                selectedSellProductLabel.setText("Selected product: " + selectedProduct.getProductName()
                        + " | Stock: " + selectedProduct.getCurrentStock()
                        + " | Price: " + selectedProduct.getUnitPrice().toPlainString());
            }
        });
        searchField.textProperty().addListener((observable, oldValue, newValue) -> applySearch());

        refreshProducts();
        refreshCart();
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
        if (!confirm("Clear fields", "Clear the current product and sell fields?")) {
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
        restockQuantityField.clear();
        priceField.clear();
        sellQuantityField.clear();
        selectedSellProductLabel.setText("Selected product: none");
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
            selectedSellProductLabel.setText("Selected product: none");
            messageLabel.setText("No product found.");
            return;
        }

        productTable.getSelectionModel().selectFirst();
        productTable.scrollTo(0);
        messageLabel.setText("Product selected. Enter seller name and quantity to sell.");
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
                        || contains(product.getVehicleTypeName(), normalizedSearch))
                .toList());
    }

    private boolean contains(String value, String searchText) {
        return value != null && value.toLowerCase().contains(searchText);
    }

    @FXML
    private void addSelectedProductToCart() {
        ProductTableRow selectedProduct = productTable.getSelectionModel().getSelectedItem();
        if (selectedProduct == null) {
            messageLabel.setText("Search and select a product to add to cart.");
            return;
        }

        try {
            int quantity = parseInteger(sellQuantityField.getText(), "Cart quantity");
            if (quantity > selectedProduct.getCurrentStock()) {
                messageLabel.setText("Quantity is greater than available stock.");
                return;
            }

            cartItems.add(new CartItemRow(selectedProduct, quantity));
            sellQuantityField.clear();
            refreshCart();
            messageLabel.setText("Added to cart.");
        } catch (NumberFormatException exception) {
            messageLabel.setText(exception.getMessage());
        }
    }

    @FXML
    private void removeSelectedCartItem() {
        CartItemRow selectedItem = cartTable.getSelectionModel().getSelectedItem();
        if (selectedItem == null) {
            messageLabel.setText("Select a cart item to remove.");
            return;
        }

        cartItems.remove(selectedItem);
        refreshCart();
        messageLabel.setText("Removed from cart.");
    }

    @FXML
    private void clearCart() {
        cartItems.clear();
        refreshCart();
        messageLabel.setText("Cart cleared.");
    }

    @FXML
    private void sellCart() {
        try {
            Long saleId = recordSaleService.recordCartSale(new RecordCartSaleCommand(
                    sellerNameField.getText(),
                    LocalDate.now(),
                    cartItems.stream()
                            .map(item -> new CartSaleItemCommand(item.getProductId(), item.getQuantity(), item.getUnitPrice()))
                            .toList()
            ));
            cartItems.clear();
            refreshCart();
            refreshProducts();
            messageLabel.setText("Cart sold. Sale ID: " + saleId);
        } catch (BusinessException exception) {
            messageLabel.setText(exception.getMessage());
        }
    }

    @FXML
    private void undoLastSale() {
        try {
            Long saleId = recordSaleService.undoLatestSaleForSeller(sellerNameField.getText());
            messageLabel.setText("Sale undone for seller " + sellerNameField.getText().trim() + ". ID: " + saleId);
            refreshProducts();
        } catch (BusinessException exception) {
            messageLabel.setText(exception.getMessage());
        }
    }

    private void fillForm(ProductTableRow product) {
        productNameField.setText(product.getProductName());
        brandField.setText(product.getBrandName());
        vehicleTypeField.setText(product.getVehicleTypeName());
        priceField.setText(product.getUnitPrice().toPlainString());
    }

    private void refreshCart() {
        cartTable.getItems().setAll(cartItems);
        BigDecimal total = cartItems.stream()
                .map(CartItemRow::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        cartTotalLabel.setText("Cart total: " + total.toPlainString());
    }

    private CreateProductCommand toCreateCommand() {
        return new CreateProductCommand(
                productNameField.getText(),
                brandField.getText(),
                vehicleTypeField.getText(),
                0,
                parsePrice()
        );
    }

    private UpdateProductCommand toUpdateCommand(Long productId) {
        ProductTableRow selectedProduct = productTable.getSelectionModel().getSelectedItem();
        return new UpdateProductCommand(
                productId,
                productNameField.getText(),
                brandField.getText(),
                vehicleTypeField.getText(),
                selectedProduct.getCurrentStock(),
                parsePrice()
        );
    }

    @FXML
    private void restockProduct() {
        ProductTableRow selectedProduct = productTable.getSelectionModel().getSelectedItem();
        if (selectedProduct == null) {
            messageLabel.setText("Select a product to restock.");
            return;
        }

        try {
            productService.restockProduct(selectedProduct.getId(), parseInteger(restockQuantityField.getText(), "Restock quantity"));
            restockQuantityField.clear();
            refreshProducts();
            messageLabel.setText("Product restocked.");
        } catch (BusinessException | NumberFormatException exception) {
            messageLabel.setText(exception.getMessage());
        }
    }

    private int parseInteger(String value, String fieldName) {
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException exception) {
            throw new NumberFormatException(fieldName + " must be a whole number.");
        }
    }

    private BigDecimal parsePrice() {
        try {
            return new BigDecimal(priceField.getText().trim());
        } catch (NumberFormatException exception) {
            throw new NumberFormatException("Price must be a valid number.");
        }
    }

    @FXML
    private void backToDashboard() {
        showDashboard();
    }
}
