package com.inventory.vehicle.dashboard.presentation;

import com.inventory.vehicle.auth.application.SessionService;
import com.inventory.vehicle.common.exception.BusinessException;
import com.inventory.vehicle.common.util.MoneyFormat;
import com.inventory.vehicle.common.util.ReceiptPrinter;
import com.inventory.vehicle.navigation.SceneManager;
import com.inventory.vehicle.navigation.SidebarController;
import com.inventory.vehicle.product.application.ProductQueryService;
import com.inventory.vehicle.product.presentation.ProductTableRow;
import com.inventory.vehicle.sales.application.CartSaleItemCommand;
import com.inventory.vehicle.sales.application.RecordCartSaleCommand;
import com.inventory.vehicle.sales.application.RecordSaleService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.TilePane;
import javafx.scene.layout.VBox;
import org.springframework.stereotype.Controller;

@Controller
public class EmployeeDashboardController extends SidebarController {

    private final ProductQueryService productQueryService;
    private final RecordSaleService recordSaleService;
    private List<ProductTableRow> allProducts = List.of();
    private final List<EmployeeCartItemRow> cartItems = new ArrayList<>();

    @FXML
    private TextField searchField;

    @FXML
    private TilePane productCardPane;

    @FXML
    private TextField cartQuantityField;

    @FXML
    private TableView<EmployeeCartItemRow> cartTable;

    @FXML
    private TableColumn<EmployeeCartItemRow, String> cartProductColumn;

    @FXML
    private TableColumn<EmployeeCartItemRow, String> cartModelCodeColumn;

    @FXML
    private TableColumn<EmployeeCartItemRow, String> cartStockNumberColumn;

    @FXML
    private TableColumn<EmployeeCartItemRow, Integer> cartQuantityColumn;

    @FXML
    private TableColumn<EmployeeCartItemRow, BigDecimal> cartOriginalPriceColumn;

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
            RecordSaleService recordSaleService) {
        super(sceneManager, sessionService);
        this.productQueryService = productQueryService;
        this.recordSaleService = recordSaleService;
    }

    @FXML
    private void initialize() {
        cartProductColumn.setCellValueFactory(new PropertyValueFactory<>("productName"));
        cartModelCodeColumn.setCellValueFactory(new PropertyValueFactory<>("modelCode"));
        cartStockNumberColumn.setCellValueFactory(new PropertyValueFactory<>("stockNumber"));
        cartQuantityColumn.setCellValueFactory(new PropertyValueFactory<>("quantity"));
        cartOriginalPriceColumn.setCellValueFactory(new PropertyValueFactory<>("originalPrice"));
        cartPriceColumn.setCellValueFactory(new PropertyValueFactory<>("priceSold"));
        cartTotalColumn.setCellValueFactory(new PropertyValueFactory<>("totalAmount"));
        cartOriginalPriceColumn.setCellFactory(column -> moneyCell());
        cartPriceColumn.setCellFactory(column -> editablePriceCell());
        cartTotalColumn.setCellFactory(column -> moneyCell());
        cartTable.setEditable(true);
        cartTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        cartTable.setRowFactory(tv -> {
            TableRow<EmployeeCartItemRow> row = new TableRow<>() {
                @Override
                protected void updateItem(EmployeeCartItemRow item, boolean empty) {
                    super.updateItem(item, empty);
                }
            };
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !row.isEmpty()) {
                    cartTable.edit(row.getIndex(), cartPriceColumn);
                }
            });
            return row;
        });
        cartTable.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, selectedItem) -> {
            if (selectedItem == null) {
                cartQuantityField.clear();
            } else {
                cartQuantityField.setText(String.valueOf(selectedItem.getQuantity()));
            }
        });

        searchField.textProperty().addListener((observable, oldValue, newValue) -> applySearch());

        refreshProducts();
        refreshCart();
    }

    @FXML
    private void searchProduct() {
        List<ProductTableRow> products = filterProducts();
        renderProductCards(products);
        if (products.isEmpty()) {
            messageLabel.setText("No matching product found.");
            return;
        }
        messageLabel.setText("Showing " + products.size() + " matching product(s).");
    }

    @FXML
    private void clearSearch() {
        searchField.clear();
        applySearch();
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
        cartQuantityField.clear();
        messageLabel.setText("Removed from cart.");
    }

    @FXML
    private void updateCartItemQuantity() {
        EmployeeCartItemRow selectedItem = cartTable.getSelectionModel().getSelectedItem();
        if (selectedItem == null) {
            messageLabel.setText("Select a cart item to update.");
            return;
        }

        try {
            int quantity = parseCardQuantity(cartQuantityField.getText());
            ProductTableRow product = findProduct(selectedItem.getProductId());
            if (product != null && quantity > product.getCurrentStock()) {
                messageLabel.setText("Insufficient stock. Available stock: " + product.getCurrentStock() + ".");
                return;
            }
            selectedItem.setQuantity(quantity);
            refreshCart();
            cartTable.getSelectionModel().select(selectedItem);
            cartQuantityField.setText(String.valueOf(quantity));
            messageLabel.setText(selectedItem.getProductName() + " quantity updated.");
        } catch (NumberFormatException exception) {
            messageLabel.setText(exception.getMessage());
        }
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
                    "Delivery Receipt",
                    cartItems.stream()
                            .map(item -> new CartSaleItemCommand(item.getProductId(), item.getQuantity(),
                                    item.getOriginalPrice(), item.getPriceSold()))
                            .toList()));
            ReceiptPrinter.printReceipt(
                    sessionService.getCurrentDisplayName(),
                    LocalDate.now(),
                    new ArrayList<>(cartItems),
                    saleId);
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

    private ProductTableRow findProduct(Long productId) {
        return allProducts.stream()
                .filter(product -> product.getId().equals(productId))
                .findFirst()
                .orElse(null);
    }

    private void applySearch() {
        renderProductCards(filterProducts());
    }

    private List<ProductTableRow> filterProducts() {
        String searchText = searchField.getText();
        if (searchText == null || searchText.isBlank()) {
            return allProducts;
        }

        String normalizedSearch = searchText.trim().toLowerCase();
        return allProducts.stream()
                .filter(product -> contains(product.getProductName(), normalizedSearch)
                        || contains(product.getBrandName(), normalizedSearch)
                        || contains(product.getVehicleTypeName(), normalizedSearch)
                        || contains(product.getModelCode(), normalizedSearch))
                .toList();
    }

    private void renderProductCards(List<ProductTableRow> products) {
        productCardPane.getChildren().setAll(products.stream()
                .map(this::createProductCard)
                .toList());
    }

    private VBox createProductCard(ProductTableRow product) {
        ImageView productImage = new ImageView(product.getImage());
        productImage.setFitHeight(96);
        productImage.setFitWidth(140);
        productImage.setPreserveRatio(true);
        productImage.setPickOnBounds(true);
        productImage.getStyleClass().add("product-card-image");
        productImage.setOnMouseClicked(event -> {
            openProductDetails(product);
            event.consume();
        });

        Label photoPlaceholder = new Label(product.getImage() == null ? "No Photo" : "");
        photoPlaceholder.getStyleClass().add("product-card-placeholder");

        VBox imageBox = new VBox(productImage, photoPlaceholder);
        imageBox.setAlignment(Pos.CENTER);
        imageBox.getStyleClass().add("product-card-image-box");

        Label nameLabel = new Label(product.getProductName());
        nameLabel.setWrapText(true);
        nameLabel.getStyleClass().add("product-card-title");

        Label metaLabel = new Label(product.getBrandName() + " | " + product.getModelCode());
        metaLabel.setWrapText(true);
        metaLabel.getStyleClass().add("subtitle");

        Label stockLabel = new Label("Stock: " + product.getCurrentStock());
        stockLabel.getStyleClass().add("field-label");

        Label drLabel = new Label(product.getLastDrNumber().isEmpty() ? "" : "DR: " + product.getLastDrNumber());
        drLabel.getStyleClass().add("field-label");

        Label priceLabel = new Label(MoneyFormat.peso(product.getUnitPrice()));
        priceLabel.getStyleClass().add("summary-value-small");

        TextField quantityField = new TextField("1");
        quantityField.setPromptText("Qty");
        quantityField.setMaxWidth(72);
        quantityField.setOnMouseClicked(event -> event.consume());

        Button addButton = new Button("Add to Cart");
        addButton.setMaxWidth(Double.MAX_VALUE);
        addButton.setOnMouseClicked(event -> event.consume());
        addButton.setOnAction(event -> {
            try {
                addProductToCart(product, quantityField.getText());
                quantityField.setText("1");
            } catch (NumberFormatException exception) {
                messageLabel.setText(exception.getMessage());
            }
            event.consume();
        });

        HBox cartControls = new HBox(8, quantityField, addButton);
        cartControls.setAlignment(Pos.CENTER_LEFT);
        cartControls.setOnMouseClicked(event -> event.consume());

        VBox card = new VBox(8, imageBox, nameLabel, metaLabel, new HBox(10, stockLabel, drLabel), new HBox(10, priceLabel), cartControls);
        card.setPadding(new Insets(12));
        card.setPrefWidth(210);
        card.setMinHeight(260);
        card.getStyleClass().add("product-card");
        card.setOnMouseClicked(event -> openProductDetails(product));
        return card;
    }

    private void addProductToCart(ProductTableRow product, String quantityText) {
        int quantity = parseCardQuantity(quantityText);
        EmployeeCartItemRow existingItem = findCartItem(product.getId());
        int existingQuantity = existingItem == null ? 0 : existingItem.getQuantity();
        int totalRequestedQuantity = existingQuantity + quantity;
        if (totalRequestedQuantity > product.getCurrentStock()) {
            messageLabel.setText("Insufficient stock. Available stock: " + product.getCurrentStock() + ".");
            return;
        }

        if (existingItem == null) {
            cartItems.add(new EmployeeCartItemRow(product, quantity, product.getUnitPrice()));
            messageLabel.setText(product.getProductName() + " added to cart.");
        } else {
            existingItem.addQuantity(quantity);
            messageLabel.setText(product.getProductName() + " quantity updated.");
        }
        refreshCart();
    }

    private int parseCardQuantity(String quantityText) {
        if (quantityText == null || quantityText.isBlank()) {
            throw new NumberFormatException("Quantity is required.");
        }
        try {
            int quantity = Integer.parseInt(quantityText.trim());
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

    private boolean contains(String value, String searchText) {
        return value != null && value.toLowerCase().contains(searchText);
    }

    private void openProductDetails(ProductTableRow product) {
        Image image = product.getImage();
        ImageView imageView = new ImageView(image);
        imageView.setFitWidth(240);
        imageView.setFitHeight(180);
        imageView.setPreserveRatio(true);
        imageView.setPickOnBounds(true);
        imageView.getStyleClass().add("product-image-preview");
        imageView.setOnMouseClicked(event -> {
            openProductImage(product);
            event.consume();
        });

        Label noPhotoLabel = new Label(image == null ? "No Photo" : "Click photo to enlarge");
        noPhotoLabel.getStyleClass().add("subtitle");

        VBox photoBox = new VBox(8, imageView, noPhotoLabel);
        photoBox.setAlignment(Pos.CENTER);
        photoBox.getStyleClass().add("product-card-image-box");

        GridPane details = new GridPane();
        details.setHgap(18);
        details.setVgap(10);
        addDetail(details, 0, 0, "Product", product.getProductName());
        addDetail(details, 1, 0, "Brand", product.getBrandName());
        addDetail(details, 0, 1, "Type", product.getVehicleTypeName());
        addDetail(details, 1, 1, "Model", product.getModelCode());
        addDetail(details, 0, 2, "Stock", String.valueOf(product.getCurrentStock()));
        addDetail(details, 1, 2, "DR Number", product.getLastDrNumber().isEmpty() ? "-" : product.getLastDrNumber());
        addDetail(details, 0, 3, "Default Price", MoneyFormat.peso(product.getUnitPrice()));

        VBox content = new VBox(14, photoBox, details);
        content.setPrefWidth(560);

        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Product Details");
        dialog.setHeaderText(product.getProductName());
        dialog.getDialogPane().getButtonTypes().add(new ButtonType("Close", ButtonBar.ButtonData.CANCEL_CLOSE));
        dialog.getDialogPane().setContent(content);
        dialog.showAndWait();
    }

    private void addDetail(GridPane details, int column, int row, String label, String value) {
        VBox field = new VBox(3);
        Label labelNode = new Label(label);
        labelNode.getStyleClass().add("field-label");
        Label valueNode = new Label(value == null || value.isBlank() ? "-" : value);
        valueNode.setWrapText(true);
        field.getChildren().addAll(labelNode, valueNode);
        details.add(field, column, row);
    }

    private void openProductImage(ProductTableRow product) {
        Image image = product.getImage();
        if (image == null) {
            messageLabel.setText("No product photo available.");
            return;
        }
        openProductImage(product.getProductName(), image);
    }

    private void openProductImage(String title, Image image) {
        ImageView imageView = new ImageView(image);
        imageView.setPreserveRatio(true);
        imageView.setFitWidth(760);
        imageView.setFitHeight(520);

        ScrollPane imageScroll = new ScrollPane(imageView);
        imageScroll.setFitToWidth(true);
        imageScroll.setFitToHeight(true);
        imageScroll.setPannable(true);

        VBox content = new VBox(12, new Label(title), imageScroll);
        content.setPrefSize(820, 600);

        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Product Photo");
        dialog.setHeaderText(title);
        dialog.getDialogPane().getButtonTypes().add(new ButtonType("Close", ButtonBar.ButtonData.CANCEL_CLOSE));
        dialog.getDialogPane().setContent(content);
        dialog.showAndWait();
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

    private TableCell<EmployeeCartItemRow, BigDecimal> editablePriceCell() {
        return new TableCell<>() {
            private TextField priceField;

            @Override
            protected void updateItem(BigDecimal amount, boolean empty) {
                super.updateItem(amount, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setGraphic(null);
                    setText(null);
                    return;
                }

                if (isEditing()) {
                    if (priceField == null) {
                        priceField = new TextField();
                        priceField.setPrefWidth(80);
                        priceField.setOnAction(event -> commitEdit(parsePrice(priceField.getText())));
                        priceField.focusedProperty().addListener((obs, oldVal, newVal) -> {
                            if (!newVal && priceField.getText() != null && !priceField.getText().isEmpty()) {
                                try {
                                    commitEdit(parsePrice(priceField.getText()));
                                } catch (NumberFormatException e) {
                                    cancelEdit();
                                }
                            }
                        });
                    }
                    priceField.setText(amount == null ? "" : amount.toString());
                    setGraphic(priceField);
                    setText(null);
                    priceField.requestFocus();
                    priceField.selectAll();
                } else {
                    setGraphic(null);
                    setText(MoneyFormat.peso(amount));
                }
            }

            @Override
            public void startEdit() {
                if (!isEmpty()) {
                    super.startEdit();
                    updateItem(getItem(), false);
                }
            }

            @Override
            public void commitEdit(BigDecimal newValue) {
                super.commitEdit(newValue);
                EmployeeCartItemRow cartItem = getTableRow().getItem();
                if (cartItem != null) {
                    cartItem.setPriceSold(newValue);
                    refreshCart();
                }
            }

            @Override
            public void cancelEdit() {
                super.cancelEdit();
                updateItem(getItem(), false);
            }

            private BigDecimal parsePrice(String text) {
                if (text == null || text.isBlank()) {
                    throw new NumberFormatException("Price is required.");
                }
                try {
                    BigDecimal price = new BigDecimal(text.trim());
                    if (price.compareTo(BigDecimal.ZERO) <= 0) {
                        throw new NumberFormatException("Price must be greater than zero.");
                    }
                    return price;
                } catch (NumberFormatException e) {
                    messageLabel.setText("Invalid price format. Enter a valid number.");
                    throw e;
                }
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

}
