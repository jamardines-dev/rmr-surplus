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
import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.time.LocalDate;
import java.util.List;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;
import org.springframework.stereotype.Controller;

@Controller
public class ProductListController extends SidebarController {

    private final ProductQueryService productQueryService;
    private final ProductService productService;
    private List<ProductTableRow> allProducts = List.of();
    private byte[] selectedImage;
    private String selectedImageType;

    @FXML
    private TableView<ProductTableRow> productTable;

    @FXML
    private TableColumn<ProductTableRow, Image> imageColumn;

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
    private TableColumn<ProductTableRow, String> restockedColumn;

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
    private DatePicker lastRestockedDatePicker;

    @FXML
    private ImageView productImageView;

    @FXML
    private Label imageNameLabel;

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
        imageColumn.setCellValueFactory(new PropertyValueFactory<>("image"));
        imageColumn.setCellFactory(column -> imageCell());
        idColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        productNameColumn.setCellValueFactory(new PropertyValueFactory<>("productName"));
        brandColumn.setCellValueFactory(new PropertyValueFactory<>("brandName"));
        vehicleTypeColumn.setCellValueFactory(new PropertyValueFactory<>("vehicleTypeName"));
        modelCodeColumn.setCellValueFactory(new PropertyValueFactory<>("modelCode"));
        stockColumn.setCellValueFactory(new PropertyValueFactory<>("currentStock"));
        priceColumn.setCellValueFactory(new PropertyValueFactory<>("unitPrice"));
        priceColumn.setCellFactory(column -> moneyCell());
        restockedColumn.setCellValueFactory(new PropertyValueFactory<>("lastRestockedDateText"));
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
        lastRestockedDatePicker.setValue(null);
        selectedImage = null;
        selectedImageType = null;
        productImageView.setImage(null);
        imageNameLabel.setText("No photo selected");
    }

    @FXML
    private void chooseProductImage() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Choose Product Photo");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter(
                "Images",
                "*.png",
                "*.jpg",
                "*.jpeg",
                "*.gif"
        ));
        File file = fileChooser.showOpenDialog(productTable.getScene().getWindow());
        if (file == null) {
            return;
        }

        try {
            selectedImage = Files.readAllBytes(file.toPath());
            selectedImageType = Files.probeContentType(file.toPath());
            productImageView.setImage(new Image(file.toURI().toString()));
            imageNameLabel.setText(file.getName());
            messageLabel.setText("Photo selected.");
        } catch (IOException exception) {
            messageLabel.setText("Could not load product photo.");
        }
    }

    @FXML
    private void removeProductImage() {
        selectedImage = null;
        selectedImageType = null;
        productImageView.setImage(null);
        imageNameLabel.setText("No photo selected");
        messageLabel.setText("Photo removed.");
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
        lastRestockedDatePicker.setValue(product.getLastRestockedDate());
        selectedImage = product.getProductImage();
        selectedImageType = product.getProductImageType();
        productImageView.setImage(product.getImage());
        imageNameLabel.setText(product.getImage() == null ? "No photo selected" : "Saved product photo");
    }

    private CreateProductCommand toCreateCommand() {
        return new CreateProductCommand(
                productNameField.getText(),
                brandField.getText(),
                vehicleTypeField.getText(),
                modelCodeField.getText(),
                parseInteger(stockQuantityField.getText(), "Stock quantity"),
                parsePrice(),
                selectedImage,
                selectedImageType,
                lastRestockedDatePicker.getValue());
    }

    private UpdateProductCommand toUpdateCommand(Long productId) {
        return new UpdateProductCommand(
                productId,
                productNameField.getText(),
                brandField.getText(),
                vehicleTypeField.getText(),
                modelCodeField.getText(),
                parseInteger(stockQuantityField.getText(), "Stock quantity"),
                parsePrice(),
                selectedImage,
                selectedImageType,
                lastRestockedDatePicker.getValue());
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

    private TableCell<ProductTableRow, Image> imageCell() {
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
