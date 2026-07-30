package com.inventory.vehicle.product.presentation;

import com.inventory.vehicle.common.exception.BusinessException;
import com.inventory.vehicle.auth.application.SessionService;
import com.inventory.vehicle.common.util.MoneyFormat;
import com.inventory.vehicle.navigation.SceneManager;
import com.inventory.vehicle.navigation.SidebarController;
import com.inventory.vehicle.product.application.CreateProductCommand;
import com.inventory.vehicle.product.application.NewProductImage;
import com.inventory.vehicle.product.application.ProductQueryService;
import com.inventory.vehicle.product.application.ProductResult;
import com.inventory.vehicle.product.application.ProductService;
import com.inventory.vehicle.product.application.RestockNewProductsCommand;
import com.inventory.vehicle.product.application.UpdateProductCommand;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.time.LocalDate;
import java.util.List;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import org.springframework.stereotype.Controller;

@Controller
public class ProductListController extends SidebarController {

    private final ProductQueryService productQueryService;
    private final ProductService productService;
    private List<ProductTableRow> allProducts = List.of();
    private java.util.List<NewProductImage> selectedImages = new java.util.ArrayList<>();

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
    private TableColumn<ProductTableRow, String> drNumberColumn;

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

    private Label drNumberDisplayLabel;

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
        drNumberColumn.setCellValueFactory(new PropertyValueFactory<>("lastDrNumber"));
        productTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);

        productTable.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, selectedProduct) -> {
            if (selectedProduct != null) {
                messageLabel.setText("Selected " + selectedProduct.getProductName() + ".");
            }
        });
        searchField.textProperty().addListener((observable, oldValue, newValue) -> applySearch());

        refreshProducts();
    }

    @FXML
    private void createProduct() {
        openProductDetailsModal(null);
    }

    @FXML
    private void updateProduct() {
        ProductTableRow selectedProduct = productTable.getSelectionModel().getSelectedItem();
        if (selectedProduct == null) {
            messageLabel.setText("Select a product to update.");
            return;
        }
        openProductDetailsModal(selectedProduct);
    }

    private void saveNewProduct() {
        try {
            productService.createProduct(toCreateCommand());
            resetForm();
            refreshProducts();
            messageLabel.setText("Product created.");
        } catch (BusinessException | NumberFormatException exception) {
            messageLabel.setText(exception.getMessage());
        }
    }

    private void saveProductUpdate(Long productId) {
        try {
            productService.updateProduct(toUpdateCommand(productId));
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
        if (productNameField != null) {
            productNameField.clear();
            brandField.clear();
            vehicleTypeField.clear();
            modelCodeField.clear();
            stockQuantityField.clear();
            priceField.clear();
            lastRestockedDatePicker.setValue(null);
        }
        selectedImages.clear();
        if (productImageView != null) {
            productImageView.setImage(null);
        }
        if (imageNameLabel != null) {
            imageNameLabel.setText("No photo selected");
        }
    }

    @FXML
    private void chooseProductImage() {
        if (selectedImages.size() >= 4) {
            messageLabel.setText("Maximum 4 images allowed. Remove an image to add another.");
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Choose Product Photo");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter(
                "Images",
                "*.png",
                "*.jpg",
                "*.jpeg",
                "*.gif"));
        File file = fileChooser.showOpenDialog(productTable.getScene().getWindow());
        if (file == null) {
            return;
        }

        try {
            byte[] imageData = Files.readAllBytes(file.toPath());
            String imageType = Files.probeContentType(file.toPath());
            selectedImages.add(new NewProductImage(imageData, imageType));
            productImageView.setImage(new Image(file.toURI().toString()));
            imageNameLabel.setText("(" + selectedImages.size() + "/4 images) " + file.getName());
            messageLabel.setText(
                    "Photo " + selectedImages.size() + " added. " + (4 - selectedImages.size()) + " more allowed.");
        } catch (IOException exception) {
            messageLabel.setText("Could not load product photo.");
        }
    }

    @FXML
    private void removeProductImage() {
        if (!selectedImages.isEmpty()) {
            selectedImages.remove(selectedImages.size() - 1);
            productImageView.setImage(null);
            imageNameLabel.setText(
                    selectedImages.isEmpty() ? "No photo selected" : "(" + selectedImages.size() + "/4 images)");
            messageLabel.setText(selectedImages.isEmpty() ? "Photo removed."
                    : "Photo removed. " + (4 - selectedImages.size()) + " more allowed.");
        }
    }

    private void openProductDetailsModal(ProductTableRow product) {
        initializeProductFormFields();
        selectedImages.clear();
        if (product != null) {
            fillForm(product);
        }

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle(product == null ? "Add Product" : "Product Details");
        dialog.setHeaderText(product == null ? "Create a new product." : "Edit product details.");
        dialog.initOwner(productTable.getScene().getWindow());

        ButtonType saveButtonType = new ButtonType(product == null ? "Create Product" : "Save Changes",
                ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);
        Label productErrorLabel = new Label();
        productErrorLabel.getStyleClass().add("message");

        VBox content = createProductDetailsContent(productErrorLabel);
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().setPrefWidth(520);
        dialog.getDialogPane().setPrefHeight(700);

        Button saveButton = (Button) dialog.getDialogPane().lookupButton(saveButtonType);
        saveButton.addEventFilter(ActionEvent.ACTION, event -> {
            if (product == null) {
                saveNewProduct();
            } else {
                saveProductUpdate(product.getId());
            }
            if (messageLabel.getText() != null
                    && (messageLabel.getText().equals("Product created.")
                            || messageLabel.getText().equals("Product updated."))) {
                return;
            }
            event.consume();
            productErrorLabel.setText(messageLabel.getText());
        });

        dialog.showAndWait();
    }

    private void initializeProductFormFields() {
        productImageView = new ImageView();
        productImageView.setFitHeight(120);
        productImageView.setFitWidth(190);
        productImageView.setPreserveRatio(true);
        productImageView.getStyleClass().add("product-image-preview");
        imageNameLabel = new Label("No photo selected");
        imageNameLabel.setWrapText(true);
        imageNameLabel.getStyleClass().add("subtitle");
        drNumberDisplayLabel = new Label("Stock Number: —");
        drNumberDisplayLabel.getStyleClass().add("subtitle");
        productNameField = new TextField();
        productNameField.setPromptText("Starter solenoid");
        brandField = new TextField();
        brandField.setPromptText("Mitsubishi");
        vehicleTypeField = new TextField();
        vehicleTypeField.setPromptText("10 Wheeler");
        modelCodeField = new TextField();
        modelCodeField.setPromptText("8M20");
        stockQuantityField = new TextField();
        stockQuantityField.setPromptText("10");
        priceField = new TextField();
        priceField.setPromptText("1200.00");
        lastRestockedDatePicker = new DatePicker();
    }

    private VBox createProductDetailsContent(Label productErrorLabel) {
        VBox content = new VBox(12);
        content.setPadding(new Insets(8, 0, 0, 0));
        content.getStyleClass().add("modal-content");

        HBox imageActions = new HBox(10);
        Button choosePhotoButton = new Button("Choose Photo");
        choosePhotoButton.getStyleClass().add("secondary");
        choosePhotoButton.setMaxWidth(Double.MAX_VALUE);
        choosePhotoButton.setOnAction(event -> chooseProductImage());
        Button removePhotoButton = new Button("Remove");
        removePhotoButton.getStyleClass().add("secondary");
        removePhotoButton.setMaxWidth(Double.MAX_VALUE);
        removePhotoButton.setOnAction(event -> removeProductImage());
        imageActions.getChildren().addAll(choosePhotoButton, removePhotoButton);
        HBox.setHgrow(choosePhotoButton, Priority.ALWAYS);
        HBox.setHgrow(removePhotoButton, Priority.ALWAYS);

        HBox stockPriceRow = new HBox(10);
        stockPriceRow.getChildren().addAll(
                labeledField("Stock Quantity", stockQuantityField),
                labeledField("Default Price", priceField));

        content.getChildren().addAll(
                productImageView,
                imageActions,
                imageNameLabel,
                drNumberDisplayLabel,
                labeledField("Product", productNameField),
                labeledField("Brand", brandField),
                labeledField("Vehicle", vehicleTypeField),
                labeledField("Model", modelCodeField),
                stockPriceRow,
                labeledField("Last Restocked Date", lastRestockedDatePicker),
                productErrorLabel);
        return content;
    }

    @FXML
    private void openRestockModal() {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Restock / Add Stock");
        dialog.initOwner(productTable.getScene().getWindow());

        ButtonType applyButtonType = new ButtonType("Apply Restock", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(applyButtonType, ButtonType.CANCEL);

        DatePicker restockedDatePicker = new DatePicker(LocalDate.now());
        Button addProductButton = new Button("Add New Product");
        Button removeProductButton = new Button("Remove Selected");
        removeProductButton.getStyleClass().add("secondary");
        Label restockErrorLabel = new Label();
        restockErrorLabel.getStyleClass().add("message");

        ObservableList<RestockProductRow> restockRows = FXCollections.observableArrayList();
        TableView<RestockProductRow> restockTable = createRestockTable();
        restockTable.setItems(restockRows);
        addProductButton.setOnAction(event -> openRestockProductModal(restockRows, restockErrorLabel));
        removeProductButton.setOnAction(event -> {
            RestockProductRow selectedRow = restockTable.getSelectionModel().getSelectedItem();
            if (selectedRow != null) {
                restockRows.remove(selectedRow);
                restockErrorLabel.setText("");
            }
        });

        VBox content = new VBox(12);
        content.setPadding(new Insets(8, 0, 0, 0));
        content.getStyleClass().add("modal-content");
        HBox restockMeta = new HBox(10);
        restockMeta.getChildren().addAll(
                labeledField("Restock Date", restockedDatePicker));
        HBox addProductRow = new HBox(10);
        addProductRow.getStyleClass().add("modal-actions");
        addProductRow.getChildren().addAll(addProductButton, removeProductButton);
        content.getChildren().addAll(restockMeta, addProductRow, restockTable, restockErrorLabel);
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().setPrefWidth(860);
        dialog.getDialogPane().setPrefHeight(600);

        Button applyButton = (Button) dialog.getDialogPane().lookupButton(applyButtonType);
        applyButton.addEventFilter(ActionEvent.ACTION, event -> {
            try {
                List<CreateProductCommand> products = restockRows.stream()
                        .map(RestockProductRow::toCreateCommand)
                        .toList();
                productService.restockNewProducts(new RestockNewProductsCommand(
                        restockedDatePicker.getValue(),
                        products));
                refreshProducts();
                messageLabel.setText("Created and restocked " + products.size() + " new product(s).");
            } catch (BusinessException | NumberFormatException exception) {
                event.consume();
                restockErrorLabel.setText(exception.getMessage());
                messageLabel.setText(exception.getMessage());
            }
        });

        dialog.showAndWait();
    }

    private VBox labeledField(String labelText, javafx.scene.Node field) {
        VBox wrapper = new VBox(6);
        Label label = new Label(labelText);
        label.getStyleClass().add("field-label");
        wrapper.getChildren().addAll(label, field);
        HBox.setHgrow(wrapper, Priority.ALWAYS);
        return wrapper;
    }

    private void openRestockProductModal(ObservableList<RestockProductRow> restockRows, Label restockErrorLabel) {
        TextField productField = new TextField();
        productField.setPromptText("Product name");
        TextField brandField = new TextField();
        brandField.setPromptText("Brand");
        TextField vehicleTypeField = new TextField();
        vehicleTypeField.setPromptText("Vehicle");
        TextField modelCodeField = new TextField();
        modelCodeField.setPromptText("Model");
        TextField drNumberField = new TextField();
        drNumberField.setPromptText("Stock Number");
        TextField quantityField = new TextField();
        quantityField.setPromptText("Restock quantity");
        TextField priceField = new TextField();
        priceField.setPromptText("Default price");
        ImageView photoPreview = new ImageView();
        photoPreview.setFitWidth(150);
        photoPreview.setFitHeight(96);
        photoPreview.setPreserveRatio(true);
        photoPreview.getStyleClass().add("product-image-preview");
        Label photoLabel = new Label("No photo selected");
        photoLabel.getStyleClass().add("subtitle");
        photoLabel.setWrapText(true);
        byte[][] productImage = new byte[1][];
        String[] productImageType = new String[1];
        Button choosePhotoButton = new Button("Choose Photo");
        choosePhotoButton.getStyleClass().add("secondary");
        choosePhotoButton.setOnAction(
                event -> chooseRestockProductImage(photoPreview, photoLabel, productImage, productImageType));
        Label productErrorLabel = new Label();
        productErrorLabel.getStyleClass().add("message");

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Add New Product");
        dialog.setHeaderText("Add one new product with its DR number.");
        dialog.initOwner(productTable.getScene().getWindow());

        ButtonType addButtonType = new ButtonType("Save Product", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(addButtonType, ButtonType.CANCEL);

        HBox quantityPriceRow = new HBox(10);
        quantityPriceRow.getChildren().addAll(
                labeledField("Restock Quantity", quantityField),
                labeledField("Default Price", priceField));

        VBox photoText = new VBox(8, choosePhotoButton, photoLabel);
        HBox photoRow = new HBox(12, photoPreview, photoText);
        photoRow.getStyleClass().add("modal-photo-row");

        VBox content = new VBox(12);
        content.setPadding(new Insets(8, 0, 0, 0));
        content.getStyleClass().add("modal-content");
        content.getChildren().addAll(
                photoRow,
                labeledField("Product", productField),
                labeledField("Brand", brandField),
                labeledField("Vehicle", vehicleTypeField),
                labeledField("Model", modelCodeField),
                labeledField("Stock Number", drNumberField),
                quantityPriceRow,
                productErrorLabel);
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().setPrefWidth(540);

        Button addButton = (Button) dialog.getDialogPane().lookupButton(addButtonType);
        addButton.addEventFilter(ActionEvent.ACTION, event -> {
            try {
                RestockProductRow row = RestockProductRow.fromFields(
                        productField.getText(),
                        brandField.getText(),
                        vehicleTypeField.getText(),
                        modelCodeField.getText(),
                        drNumberField.getText(),
                        quantityField.getText(),
                        priceField.getText(),
                        productImage[0],
                        productImageType[0]);
                if (restockRows.stream()
                        .anyMatch(existingRow -> existingRow.getModelCode().equalsIgnoreCase(row.getModelCode()))) {
                    throw new BusinessException("This model is already listed in this restock.");
                }
                restockRows.add(row);
                restockErrorLabel.setText("");
            } catch (BusinessException | NumberFormatException exception) {
                event.consume();
                productErrorLabel.setText(exception.getMessage());
            }
        });

        dialog.showAndWait();
    }

    private void chooseRestockProductImage(
            ImageView photoPreview,
            Label photoLabel,
            byte[][] productImage,
            String[] productImageType) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Choose Product Photo");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter(
                "Images",
                "*.png",
                "*.jpg",
                "*.jpeg",
                "*.gif"));
        File file = fileChooser.showOpenDialog(productTable.getScene().getWindow());
        if (file == null) {
            return;
        }

        try {
            productImage[0] = Files.readAllBytes(file.toPath());
            productImageType[0] = Files.probeContentType(file.toPath());
            photoPreview.setImage(new Image(file.toURI().toString()));
            photoLabel.setText(file.getName());
        } catch (IOException exception) {
            photoLabel.setText("Could not load product photo.");
        }
    }

    private TableView<RestockProductRow> createRestockTable() {
        TableView<RestockProductRow> restockTable = new TableView<>();
        restockTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        restockTable.setPrefHeight(390);

        TableColumn<RestockProductRow, Image> photoColumn = new TableColumn<>("Photo");
        photoColumn.setCellValueFactory(data -> new ReadOnlyObjectWrapper<>(data.getValue().getImage()));
        photoColumn.setCellFactory(column -> restockImageCell());
        photoColumn.setPrefWidth(80);

        TableColumn<RestockProductRow, String> productColumn = new TableColumn<>("Product");
        productColumn.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().getProductName()));
        productColumn.setPrefWidth(220);

        TableColumn<RestockProductRow, String> brandColumn = new TableColumn<>("Brand");
        brandColumn.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().getBrandName()));
        brandColumn.setPrefWidth(150);

        TableColumn<RestockProductRow, String> modelColumn = new TableColumn<>("Model");
        modelColumn.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().getModelCode()));
        modelColumn.setPrefWidth(130);

        TableColumn<RestockProductRow, String> drColumn = new TableColumn<>("Stock Number");
        drColumn.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().getDrNumber()));
        drColumn.setPrefWidth(100);

        TableColumn<RestockProductRow, Integer> quantityColumn = new TableColumn<>("Qty");
        quantityColumn.setCellValueFactory(data -> new ReadOnlyObjectWrapper<>(data.getValue().getQuantity()));
        quantityColumn.setPrefWidth(90);

        TableColumn<RestockProductRow, BigDecimal> priceColumn = new TableColumn<>("Price");
        priceColumn.setCellValueFactory(data -> new ReadOnlyObjectWrapper<>(data.getValue().getUnitPrice()));
        priceColumn.setCellFactory(column -> moneyCell());
        priceColumn.setPrefWidth(120);

        restockTable.getColumns().add(photoColumn);
        restockTable.getColumns().add(productColumn);
        restockTable.getColumns().add(brandColumn);
        restockTable.getColumns().add(modelColumn);
        restockTable.getColumns().add(drColumn);
        restockTable.getColumns().add(quantityColumn);
        restockTable.getColumns().add(priceColumn);
        return restockTable;
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
        selectedImages.clear();
        productImageView.setImage(product.getImage());
        imageNameLabel.setText(product.getImage() == null ? "No photo selected" : "Saved product photo");
        drNumberDisplayLabel.setText("Stock Number: " + product.getLastDrNumber());
    }

    private CreateProductCommand toCreateCommand() {
        return new CreateProductCommand(
                productNameField.getText(),
                brandField.getText(),
                vehicleTypeField.getText(),
                modelCodeField.getText(),
                parseInteger(stockQuantityField.getText(), "Stock quantity"),
                parsePrice(),
                new java.util.ArrayList<>(selectedImages),
                lastRestockedDatePicker.getValue(),
                null);
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
                new java.util.ArrayList<>(),
                new java.util.ArrayList<>(selectedImages),
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

    private TableCell<RestockProductRow, Image> restockImageCell() {
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

    private static class RestockProductRow {

        private final String productName;
        private final String brandName;
        private final String vehicleTypeName;
        private final String modelCode;
        private final String drNumber;
        private final int quantity;
        private final BigDecimal unitPrice;
        private final byte[] productImage;
        private final String productImageType;

        private RestockProductRow(
                String productName,
                String brandName,
                String vehicleTypeName,
                String modelCode,
                String drNumber,
                int quantity,
                BigDecimal unitPrice,
                byte[] productImage,
                String productImageType) {
            this.productName = productName;
            this.brandName = brandName;
            this.vehicleTypeName = vehicleTypeName;
            this.modelCode = modelCode;
            this.drNumber = drNumber;
            this.quantity = quantity;
            this.unitPrice = unitPrice;
            this.productImage = productImage;
            this.productImageType = productImageType;
        }

        private static RestockProductRow fromFields(
                String productName,
                String brandName,
                String vehicleTypeName,
                String modelCode,
                String drNumber,
                String quantityText,
                String priceText,
                byte[] productImage,
                String productImageType) {
            String cleanProductName = requireText(productName, "Product");
            String cleanBrandName = requireText(brandName, "Brand");
            String cleanVehicleTypeName = requireText(vehicleTypeName, "Vehicle");
            String cleanModelCode = requireText(modelCode, "Model");
            String cleanDrNumber = requireText(drNumber, "Stock Number");
            int quantity = parseQuantityText(quantityText);
            if (quantity <= 0) {
                throw new NumberFormatException("Restock quantity must be greater than 0.");
            }
            BigDecimal unitPrice = parsePriceText(priceText);
            return new RestockProductRow(
                    cleanProductName,
                    cleanBrandName,
                    cleanVehicleTypeName,
                    cleanModelCode,
                    cleanDrNumber,
                    quantity,
                    unitPrice,
                    productImage,
                    productImageType);
        }

        private static String requireText(String value, String fieldName) {
            if (value == null || value.isBlank()) {
                throw new BusinessException(fieldName + " is required.");
            }
            return value.trim();
        }

        private static int parseQuantityText(String value) {
            if (value == null || value.isBlank()) {
                return 0;
            }
            try {
                int quantity = Integer.parseInt(value.trim());
                if (quantity < 0) {
                    throw new NumberFormatException();
                }
                return quantity;
            } catch (NumberFormatException exception) {
                throw new NumberFormatException("Restock quantities must be whole numbers greater than 0.");
            }
        }

        private static BigDecimal parsePriceText(String value) {
            if (value == null || value.isBlank()) {
                throw new NumberFormatException("Default price is required.");
            }
            try {
                BigDecimal price = new BigDecimal(value.trim());
                if (price.compareTo(BigDecimal.ZERO) < 0) {
                    throw new NumberFormatException();
                }
                return price;
            } catch (NumberFormatException exception) {
                throw new NumberFormatException("Default price must be a valid number.");
            }
        }

        private String getProductName() {
            return productName;
        }

        private String getBrandName() {
            return brandName;
        }

        private String getVehicleTypeName() {
            return vehicleTypeName;
        }

        private String getModelCode() {
            return modelCode;
        }

        private String getDrNumber() {
            return drNumber;
        }

        private int getQuantity() {
            return quantity;
        }

        private BigDecimal getUnitPrice() {
            return unitPrice;
        }

        private Image getImage() {
            if (productImage == null || productImage.length == 0) {
                return null;
            }
            return new Image(new ByteArrayInputStream(productImage));
        }

        private CreateProductCommand toCreateCommand() {
            java.util.List<NewProductImage> images = new java.util.ArrayList<>();
            if (productImage != null && productImage.length > 0) {
                images.add(new NewProductImage(productImage, productImageType));
            }
            return new CreateProductCommand(
                    productName,
                    brandName,
                    vehicleTypeName,
                    modelCode,
                    quantity,
                    unitPrice,
                    images,
                    null,
                    drNumber);
        }
    }
}
