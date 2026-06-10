package com.inventory.vehicle.dashboard.presentation;

import com.inventory.vehicle.auth.application.SessionService;
import com.inventory.vehicle.common.exception.BusinessException;
import com.inventory.vehicle.common.util.MoneyFormat;
import com.inventory.vehicle.navigation.SceneManager;
import com.inventory.vehicle.navigation.SidebarController;
import com.inventory.vehicle.product.application.ProductQueryService;
import com.inventory.vehicle.product.presentation.ProductTableRow;
import com.inventory.vehicle.sales.application.CartSaleItemCommand;
import com.inventory.vehicle.sales.application.RecordCartSaleCommand;
import com.inventory.vehicle.sales.application.RecordSaleService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
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
public class EmployeeDashboardController extends SidebarController {

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("h:mm a");

    private final ProductQueryService productQueryService;
    private final RecordSaleService recordSaleService;
    private List<ProductTableRow> allProducts = List.of();
    private final List<EmployeeCartItemRow> cartItems = new ArrayList<>();

    @FXML
    private TextField searchField;

    @FXML
    private TableView<ProductTableRow> productTable;

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
    private Label selectedProductLabel;

    @FXML
    private Label selectedDetailsLabel;

    @FXML
    private TextField quantityField;

    @FXML
    private TextField priceField;

    @FXML
    private Label employeeNameLabel;

    @FXML
    private Label dateLabel;

    @FXML
    private Label timeLabel;

    @FXML
    private Label totalAmountLabel;

    @FXML
    private TableView<EmployeeCartItemRow> cartTable;

    @FXML
    private TableColumn<EmployeeCartItemRow, String> cartProductColumn;

    @FXML
    private TableColumn<EmployeeCartItemRow, String> cartModelCodeColumn;

    @FXML
    private TableColumn<EmployeeCartItemRow, Integer> cartQuantityColumn;

    @FXML
    private TableColumn<EmployeeCartItemRow, BigDecimal> cartPriceColumn;

    @FXML
    private TableColumn<EmployeeCartItemRow, BigDecimal> cartTotalColumn;

    @FXML
    private Label cartTotalLabel;

    @FXML
    private Label messageLabel;

    public EmployeeDashboardController(
            SceneManager sceneManager,
            SessionService sessionService,
            ProductQueryService productQueryService,
            RecordSaleService recordSaleService
    ) {
        super(sceneManager, sessionService);
        this.productQueryService = productQueryService;
        this.recordSaleService = recordSaleService;
    }

    @FXML
    private void initialize() {
        productNameColumn.setCellValueFactory(new PropertyValueFactory<>("productName"));
        brandColumn.setCellValueFactory(new PropertyValueFactory<>("brandName"));
        vehicleTypeColumn.setCellValueFactory(new PropertyValueFactory<>("vehicleTypeName"));
        modelCodeColumn.setCellValueFactory(new PropertyValueFactory<>("modelCode"));
        stockColumn.setCellValueFactory(new PropertyValueFactory<>("currentStock"));
        priceColumn.setCellValueFactory(new PropertyValueFactory<>("unitPrice"));
        priceColumn.setCellFactory(column -> moneyCell());
        productTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);

        cartProductColumn.setCellValueFactory(new PropertyValueFactory<>("productName"));
        cartModelCodeColumn.setCellValueFactory(new PropertyValueFactory<>("modelCode"));
        cartQuantityColumn.setCellValueFactory(new PropertyValueFactory<>("quantity"));
        cartPriceColumn.setCellValueFactory(new PropertyValueFactory<>("priceSold"));
        cartTotalColumn.setCellValueFactory(new PropertyValueFactory<>("totalAmount"));
        cartPriceColumn.setCellFactory(column -> moneyCell());
        cartTotalColumn.setCellFactory(column -> moneyCell());
        cartTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);

        productTable.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, selectedProduct) -> {
            if (selectedProduct != null) {
                showSelectedProduct(selectedProduct);
            }
        });
        searchField.textProperty().addListener((observable, oldValue, newValue) -> applySearch());
        quantityField.textProperty().addListener((observable, oldValue, newValue) -> updateSalePreview());
        priceField.setEditable(false);
        priceField.setFocusTraversable(false);

        employeeNameLabel.setText(sessionService.getCurrentDisplayName());
        dateLabel.setText(LocalDate.now().toString());
        timeLabel.setText(LocalTime.now().format(TIME_FORMATTER));
        refreshProducts();
        refreshCart();
        updateSalePreview();
    }

    @FXML
    private void searchProduct() {
        applySearch();
        if (productTable.getItems().isEmpty()) {
            messageLabel.setText("No matching product found.");
            return;
        }
        productTable.getSelectionModel().selectFirst();
        productTable.scrollTo(0);
        messageLabel.setText("Product selected.");
    }

    @FXML
    private void clearSearch() {
        searchField.clear();
        applySearch();
    }

    @FXML
    private void addToCart() {
        ProductTableRow selectedProduct = productTable.getSelectionModel().getSelectedItem();
        if (selectedProduct == null) {
            messageLabel.setText("Select a product first.");
            return;
        }

        try {
            int quantity = parseQuantity();
            BigDecimal price = parsePrice();
            EmployeeCartItemRow existingItem = findCartItem(selectedProduct.getId());
            int existingQuantity = existingItem == null ? 0 : existingItem.getQuantity();
            int totalRequestedQuantity = existingQuantity + quantity;
            if (totalRequestedQuantity > selectedProduct.getCurrentStock()) {
                messageLabel.setText("Insufficient stock. Available stock: " + selectedProduct.getCurrentStock() + ".");
                return;
            }

            if (existingItem == null) {
                cartItems.add(new EmployeeCartItemRow(selectedProduct, quantity, price));
                messageLabel.setText("Added to cart.");
            } else {
                existingItem.addQuantity(quantity);
                messageLabel.setText("Cart quantity updated.");
            }
            quantityField.setText("1");
            priceField.setText(selectedProduct.getUnitPrice().toPlainString());
            refreshCart();
        } catch (NumberFormatException exception) {
            messageLabel.setText(exception.getMessage());
        }
    }

    @FXML
    private void removeCartItem() {
        EmployeeCartItemRow selectedItem = cartTable.getSelectionModel().getSelectedItem();
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
    private void confirmSale() {
        if (cartItems.isEmpty()) {
            messageLabel.setText("Add at least one product to the cart.");
            return;
        }
        BigDecimal subtotal = calculateCartSubtotal();
        if (!confirm("Confirm sale", "Save this sale for " + MoneyFormat.peso(subtotal) + "?")) {
            return;
        }

        try {
            Long saleId = recordSaleService.recordCartSale(new RecordCartSaleCommand(
                    sessionService.getCurrentDisplayName(),
                    LocalDate.now(),
                    cartItems.stream()
                            .map(item -> new CartSaleItemCommand(item.getProductId(), item.getQuantity(), item.getPriceSold()))
                            .toList()
            ));
            cartItems.clear();
            refreshCart();
            refreshProducts();
            messageLabel.setText("Cart sale saved. Transaction ID: " + saleId);
        } catch (BusinessException exception) {
            messageLabel.setText(exception.getMessage());
        }
    }

    private void refreshProducts() {
        allProducts = productQueryService.findActiveProducts()
                .stream()
                .map(ProductTableRow::new)
                .toList();
        applySearch();
    }

    private void refreshCart() {
        cartTable.getItems().setAll(cartItems);
        cartTotalLabel.setText(MoneyFormat.peso(calculateCartSubtotal()));
    }

    private BigDecimal calculateCartSubtotal() {
        return cartItems.stream()
                .map(EmployeeCartItemRow::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private EmployeeCartItemRow findCartItem(Long productId) {
        return cartItems.stream()
                .filter(item -> item.getProductId().equals(productId))
                .findFirst()
                .orElse(null);
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

    private void showSelectedProduct(ProductTableRow product) {
        selectedProductLabel.setText(product.getProductName());
        selectedDetailsLabel.setText(product.getBrandName()
                + " | " + product.getVehicleTypeName()
                + " | " + product.getModelCode()
                + " | Stock: " + product.getCurrentStock()
                + " | Default price: " + MoneyFormat.peso(product.getUnitPrice()));
        quantityField.setText("1");
        priceField.setText(product.getUnitPrice().toPlainString());
        updateSalePreview();
    }

    private void updateSalePreview() {
        employeeNameLabel.setText(sessionService.getCurrentDisplayName());
        dateLabel.setText(LocalDate.now().toString());
        timeLabel.setText(LocalTime.now().format(TIME_FORMATTER));
        try {
            BigDecimal total = parsePrice().multiply(BigDecimal.valueOf(parseQuantity()));
            totalAmountLabel.setText(MoneyFormat.peso(total));
        } catch (NumberFormatException exception) {
            totalAmountLabel.setText(MoneyFormat.peso(BigDecimal.ZERO));
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

    private boolean confirm(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(title);
        alert.setHeaderText(title);
        alert.setContentText(message);
        return alert.showAndWait()
                .filter(buttonType -> buttonType == ButtonType.OK)
                .isPresent();
    }

    private int parseQuantity() {
        if (quantityField.getText() == null || quantityField.getText().isBlank()) {
            throw new NumberFormatException("Quantity is required.");
        }
        try {
            int quantity = Integer.parseInt(quantityField.getText().trim());
            if (quantity <= 0) {
                throw new NumberFormatException("Quantity must be greater than zero.");
            }
            return quantity;
        } catch (NumberFormatException exception) {
            if (exception.getMessage() != null && exception.getMessage().startsWith("Quantity")) {
                throw exception;
            }
            throw new NumberFormatException("Quantity must be a whole number.");
        }
    }

    private BigDecimal parsePrice() {
        if (priceField.getText() == null || priceField.getText().isBlank()) {
            throw new NumberFormatException("Price is required.");
        }
        try {
            BigDecimal price = new BigDecimal(priceField.getText().trim());
            if (price.compareTo(BigDecimal.ZERO) <= 0) {
                throw new NumberFormatException("Price must be greater than zero.");
            }
            return price;
        } catch (NumberFormatException exception) {
            if (exception.getMessage() != null && exception.getMessage().startsWith("Price")) {
                throw exception;
            }
            throw new NumberFormatException("Price must be a valid number.");
        }
    }
}
