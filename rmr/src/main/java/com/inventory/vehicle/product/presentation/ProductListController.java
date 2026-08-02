package com.inventory.vehicle.product.presentation;

import com.inventory.vehicle.common.exception.BusinessException;
import com.inventory.vehicle.auth.application.SessionService;
import com.inventory.vehicle.common.util.MoneyFormat;
import com.inventory.vehicle.navigation.SceneManager;
import com.inventory.vehicle.navigation.SidebarController;
import com.inventory.vehicle.product.application.CreateProductCommand;
import com.inventory.vehicle.product.application.NewProductImage;
import com.inventory.vehicle.product.application.ProductImageResult;
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
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.TilePane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Screen;
import javafx.stage.Window;
import org.springframework.stereotype.Controller;

@Controller
public class ProductListController extends SidebarController {

    private static final int MAX_PRODUCT_IMAGES = 6;

    private final ProductQueryService productQueryService;
    private final ProductService productService;
    private List<ProductTableRow> allProducts = List.of();
    private final java.util.List<EditableProductImage> editableImages = new java.util.ArrayList<>();
    private final java.util.List<Long> removedImageIds = new java.util.ArrayList<>();

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
    private TableColumn<ProductTableRow, String> productLocationColumn;

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
    private TextField productLocationField;

    @FXML
    private TextField stockQuantityField;

    @FXML
    private TextField priceField;

    @FXML
    private DatePicker lastRestockedDatePicker;

    @FXML
    private TilePane productImagePane;

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
        productLocationColumn.setCellValueFactory(new PropertyValueFactory<>("productLocation"));
        stockColumn.setCellValueFactory(new PropertyValueFactory<>("currentStock"));
        priceColumn.setCellValueFactory(new PropertyValueFactory<>("unitPrice"));
        priceColumn.setCellFactory(column -> moneyCell());
        restockedColumn.setCellValueFactory(new PropertyValueFactory<>("lastRestockedDateText"));
        drNumberColumn.setCellValueFactory(new PropertyValueFactory<>("lastDrNumber"));
        productTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        productTable.setMaxHeight(Double.MAX_VALUE);
        productTable.setMaxWidth(Double.MAX_VALUE);

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
            productLocationField.clear();
            stockQuantityField.clear();
            priceField.clear();
            lastRestockedDatePicker.setValue(null);
        }
        editableImages.clear();
        removedImageIds.clear();
        if (productImagePane != null) {
            productImagePane.getChildren().clear();
        }
        if (imageNameLabel != null) {
            imageNameLabel.setText("No photo selected");
        }
    }

    @FXML
    private void chooseProductImage() {
        if (editableImages.size() >= MAX_PRODUCT_IMAGES) {
            messageLabel.setText("Maximum " + MAX_PRODUCT_IMAGES + " images allowed. Remove an image to add another.");
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
            editableImages.add(EditableProductImage.newImage(new NewProductImage(imageData, imageType),
                    new Image(file.toURI().toString())));
            updateProductImagePreviews();
            messageLabel.setText(
                    "Photo " + editableImages.size() + " added. "
                            + (MAX_PRODUCT_IMAGES - editableImages.size()) + " more allowed.");
        } catch (IOException exception) {
            messageLabel.setText("Could not load product photo.");
        }
    }

    @FXML
    private void removeProductImage() {
        if (!editableImages.isEmpty()) {
            removeProductImage(editableImages.get(editableImages.size() - 1));
        }
    }

    private void openProductDetailsModal(ProductTableRow product) {
        initializeProductFormFields();
        editableImages.clear();
        removedImageIds.clear();
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
        ScrollPane scrollPane = new ScrollPane(content);
        scrollPane.setFitToWidth(true);
        scrollPane.setPannable(true);
        scrollPane.getStyleClass().add("content-scroll");
        dialog.getDialogPane().setContent(scrollPane);
        sizeDialogToScreen(dialog, 560, 700);
        dialog.setResizable(true);

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
        productImagePane = new TilePane();
        productImagePane.setHgap(10);
        productImagePane.setVgap(10);
        productImagePane.setPrefColumns(3);
        productImagePane.setAlignment(Pos.CENTER_LEFT);
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
        productLocationField = new TextField();
        productLocationField.setPromptText("Rack A1");
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
                imageActions,
                productImagePreviewScroll(),
                imageNameLabel,
                drNumberDisplayLabel,
                labeledField("Product", productNameField),
                labeledField("Brand", brandField),
                labeledField("Vehicle", vehicleTypeField),
                labeledField("Model", modelCodeField),
                labeledField("Location", productLocationField),
                stockPriceRow,
                labeledField("Last Restocked Date", lastRestockedDatePicker),
                productErrorLabel);
        return content;
    }

    private ScrollPane productImagePreviewScroll() {
        ScrollPane scrollPane = new ScrollPane(productImagePane);
        scrollPane.setFitToWidth(true);
        scrollPane.setPannable(true);
        scrollPane.setMinHeight(120);
        scrollPane.setPrefHeight(210);
        scrollPane.setMaxHeight(230);
        scrollPane.getStyleClass().add("content-scroll");
        return scrollPane;
    }

    @FXML
    private void openRestockModal() {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Restock / Add Stock");
        dialog.initOwner(productTable.getScene().getWindow());

        ButtonType applyButtonType = new ButtonType("Apply Restock", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(applyButtonType, ButtonType.CANCEL);

        DatePicker restockedDatePicker = new DatePicker(LocalDate.now());
        Button addProductButton = new Button("Add Restock Item");
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
        sizeDialogToScreen(dialog, 860, 640);
        dialog.setResizable(true);

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
                messageLabel.setText("Applied restock for " + products.size() + " product row(s).");
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
        TextField productLocationField = new TextField();
        productLocationField.setPromptText("Rack A1");
        TextField drNumberField = new TextField();
        drNumberField.setPromptText("Stock Number");
        TextField quantityField = new TextField();
        quantityField.setPromptText("Restock quantity");
        TextField priceField = new TextField();
        priceField.setPromptText("Default price");
        java.util.List<EditableProductImage> restockImages = new java.util.ArrayList<>();
        TilePane photoPreviewPane = new TilePane();
        photoPreviewPane.setHgap(10);
        photoPreviewPane.setVgap(10);
        photoPreviewPane.setPrefColumns(3);
        photoPreviewPane.setAlignment(Pos.CENTER_LEFT);
        ScrollPane photoPreviewScroll = new ScrollPane(photoPreviewPane);
        photoPreviewScroll.setFitToWidth(true);
        photoPreviewScroll.setPannable(true);
        photoPreviewScroll.setMinHeight(120);
        photoPreviewScroll.setPrefHeight(170);
        photoPreviewScroll.setMaxHeight(190);
        photoPreviewScroll.getStyleClass().add("content-scroll");
        Label photoLabel = new Label("No photo selected");
        photoLabel.getStyleClass().add("subtitle");
        photoLabel.setWrapText(true);
        Button choosePhotoButton = new Button("Choose Photo");
        choosePhotoButton.getStyleClass().add("secondary");
        choosePhotoButton.setMaxWidth(Double.MAX_VALUE);
        choosePhotoButton.setOnAction(
                event -> chooseRestockProductImage(photoPreviewPane, photoLabel, restockImages));
        Label productErrorLabel = new Label();
        productErrorLabel.getStyleClass().add("message");

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Add Restock Item");
        dialog.setHeaderText("Enter a new product or an existing model to add stock.");
        dialog.initOwner(productTable.getScene().getWindow());

        ButtonType addButtonType = new ButtonType("Save Product", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(addButtonType, ButtonType.CANCEL);

        HBox quantityPriceRow = new HBox(10);
        quantityPriceRow.getChildren().addAll(
                labeledField("Restock Quantity", quantityField),
                labeledField("Default Price", priceField));

        VBox photoRow = new VBox(8, choosePhotoButton, photoPreviewScroll, photoLabel);
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
                labeledField("Location", productLocationField),
                labeledField("Stock Number", drNumberField),
                quantityPriceRow,
                productErrorLabel);
        ScrollPane scrollPane = new ScrollPane(content);
        scrollPane.setFitToWidth(true);
        scrollPane.setPannable(true);
        scrollPane.getStyleClass().add("content-scroll");
        dialog.getDialogPane().setContent(scrollPane);
        sizeDialogToScreen(dialog, 540, 620);
        dialog.setResizable(true);

        Button addButton = (Button) dialog.getDialogPane().lookupButton(addButtonType);
        addButton.addEventFilter(ActionEvent.ACTION, event -> {
            try {
                RestockProductRow row = RestockProductRow.fromFields(
                        productField.getText(),
                        brandField.getText(),
                        vehicleTypeField.getText(),
                        modelCodeField.getText(),
                        productLocationField.getText(),
                        drNumberField.getText(),
                        quantityField.getText(),
                        priceField.getText(),
                        restockImages.stream()
                                .map(EditableProductImage::newImage)
                                .filter(image -> image != null)
                                .toList());
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
            TilePane photoPreviewPane,
            Label photoLabel,
            java.util.List<EditableProductImage> restockImages) {
        if (restockImages.size() >= MAX_PRODUCT_IMAGES) {
            photoLabel.setText("Maximum " + MAX_PRODUCT_IMAGES + " images allowed.");
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
            Image image = new Image(file.toURI().toString());
            EditableProductImage productImage = EditableProductImage.newImage(new NewProductImage(imageData, imageType), image);
            restockImages.add(productImage);
            updateRestockImagePreviews(photoPreviewPane, photoLabel, restockImages);
        } catch (IOException exception) {
            photoLabel.setText("Could not load product photo.");
        }
    }

    private void updateRestockImagePreviews(
            TilePane photoPreviewPane,
            Label photoLabel,
            java.util.List<EditableProductImage> restockImages) {
        photoPreviewPane.getChildren().setAll(restockImages.stream()
                .map(image -> createRestockImagePreview(photoPreviewPane, photoLabel, restockImages, image))
                .toList());
        photoLabel.setText(restockImages.isEmpty()
                ? "No photo selected"
                : "(" + restockImages.size() + "/" + MAX_PRODUCT_IMAGES + " images)");
    }

    private VBox createRestockImagePreview(
            TilePane photoPreviewPane,
            Label photoLabel,
            java.util.List<EditableProductImage> restockImages,
            EditableProductImage productImage) {
        ImageView imageView = new ImageView(productImage.image());
        imageView.setFitWidth(120);
        imageView.setFitHeight(84);
        imageView.setPreserveRatio(true);
        imageView.setPickOnBounds(true);
        imageView.getStyleClass().add("product-image-preview");
        imageView.setOnMouseClicked(event -> {
            openProductImage("Restock Product Photo", productImage.image());
            event.consume();
        });

        Button removeButton = new Button("Remove");
        removeButton.getStyleClass().add("secondary");
        removeButton.setMaxWidth(Double.MAX_VALUE);
        removeButton.setOnAction(event -> {
            restockImages.remove(productImage);
            updateRestockImagePreviews(photoPreviewPane, photoLabel, restockImages);
        });

        VBox preview = new VBox(6, imageView, removeButton);
        preview.setAlignment(Pos.CENTER);
        preview.setPrefWidth(132);
        return preview;
    }

    private TableView<RestockProductRow> createRestockTable() {
        TableView<RestockProductRow> restockTable = new TableView<>();
        restockTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        restockTable.setMinHeight(220);
        restockTable.setPrefHeight(460);
        VBox.setVgrow(restockTable, Priority.ALWAYS);

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

        TableColumn<RestockProductRow, String> locationColumn = new TableColumn<>("Location");
        locationColumn.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().getProductLocation()));
        locationColumn.setPrefWidth(120);

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
        restockTable.getColumns().add(locationColumn);
        restockTable.getColumns().add(drColumn);
        restockTable.getColumns().add(quantityColumn);
        restockTable.getColumns().add(priceColumn);
        return restockTable;
    }

    private void sizeDialogToScreen(Dialog<?> dialog, double preferredWidth, double preferredHeight) {
        Window owner = productTable.getScene().getWindow();
        var bounds = Screen.getScreensForRectangle(owner.getX(), owner.getY(), owner.getWidth(), owner.getHeight())
                .stream()
                .findFirst()
                .orElse(Screen.getPrimary())
                .getVisualBounds();
        dialog.getDialogPane().setPrefWidth(Math.min(preferredWidth, bounds.getWidth() - 80));
        dialog.getDialogPane().setPrefHeight(Math.min(preferredHeight, bounds.getHeight() - 80));
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
                        || contains(product.getModelCode(), normalizedSearch)
                        || contains(product.getProductLocation(), normalizedSearch))
                .toList());
    }

    private boolean contains(String value, String searchText) {
        return value != null && value.toLowerCase().contains(searchText);
    }

    private void updateProductImagePreviews() {
        if (productImagePane == null) {
            return;
        }

        productImagePane.getChildren().setAll(editableImages.stream()
                .map(this::createProductImagePreview)
                .toList());
        imageNameLabel.setText(editableImages.isEmpty()
                ? "No photo selected"
                : "(" + editableImages.size() + "/" + MAX_PRODUCT_IMAGES + " images)");
    }

    private VBox createProductImagePreview(EditableProductImage productImage) {
        ImageView imageView = new ImageView(productImage.image());
        imageView.setFitWidth(130);
        imageView.setFitHeight(92);
        imageView.setPreserveRatio(true);
        imageView.setPickOnBounds(true);
        imageView.getStyleClass().add("product-image-preview");
        imageView.setOnMouseClicked(event -> {
            openProductImage("Product Photo", productImage.image());
            event.consume();
        });

        Button removeButton = new Button("Remove");
        removeButton.getStyleClass().add("secondary");
        removeButton.setMaxWidth(Double.MAX_VALUE);
        removeButton.setOnAction(event -> removeProductImage(productImage));

        VBox preview = new VBox(6, imageView, removeButton);
        preview.setAlignment(Pos.CENTER);
        preview.setPrefWidth(142);
        return preview;
    }

    private void removeProductImage(EditableProductImage productImage) {
        if (productImage.savedImageId() != null) {
            removedImageIds.add(productImage.savedImageId());
        }
        editableImages.remove(productImage);
        updateProductImagePreviews();
        messageLabel.setText(editableImages.isEmpty()
                ? "Photo removed."
                : "Photo removed. " + (MAX_PRODUCT_IMAGES - editableImages.size()) + " more allowed.");
    }

    private java.util.List<NewProductImage> addedImages() {
        return editableImages.stream()
                .map(EditableProductImage::newImage)
                .filter(image -> image != null)
                .toList();
    }

    private void fillForm(ProductTableRow product) {
        product = loadFullProduct(product);
        productNameField.setText(product.getProductName());
        brandField.setText(product.getBrandName());
        vehicleTypeField.setText(product.getVehicleTypeName());
        modelCodeField.setText(product.getModelCode());
        productLocationField.setText(product.getProductLocation());
        stockQuantityField.setText(String.valueOf(product.getCurrentStock()));
        priceField.setText(product.getUnitPrice().toPlainString());
        lastRestockedDatePicker.setValue(product.getLastRestockedDate());
        editableImages.clear();
        removedImageIds.clear();
        for (ProductImageResult imageResult : product.getImageResults()) {
            byte[] imageData = imageResult.imageData();
            if (imageData != null && imageData.length > 0) {
                editableImages.add(EditableProductImage.savedImage(
                        imageResult.id(),
                        new Image(new ByteArrayInputStream(imageData))));
            }
        }
        updateProductImagePreviews();
        drNumberDisplayLabel.setText("Stock Number: " + product.getLastDrNumber());
    }

    private ProductTableRow loadFullProduct(ProductTableRow product) {
        return new ProductTableRow(productQueryService.findProduct(product.getId()));
    }

    private CreateProductCommand toCreateCommand() {
        return new CreateProductCommand(
                productNameField.getText(),
                brandField.getText(),
                vehicleTypeField.getText(),
                modelCodeField.getText(),
                productLocationField.getText(),
                parseInteger(stockQuantityField.getText(), "Stock quantity"),
                parsePrice(),
                addedImages(),
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
                productLocationField.getText(),
                parseInteger(stockQuantityField.getText(), "Stock quantity"),
                parsePrice(),
                new java.util.ArrayList<>(removedImageIds),
                addedImages(),
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
            imageView.setPickOnBounds(true);
            imageView.setOnMouseClicked(event -> {
                openProductImage("Restock Product Photo", imageView.getImage());
                event.consume();
            });
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
            imageView.setPickOnBounds(true);
            imageView.setOnMouseClicked(event -> {
                ProductTableRow product = getTableRow() == null ? null : getTableRow().getItem();
                if (product == null) {
                    openProductImage("Product Photo", imageView.getImage());
                } else {
                    openProductImages(product);
                }
                event.consume();
            });
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

    private void openProductImages(ProductTableRow product) {
        ProductTableRow fullProduct = loadFullProduct(product);
        List<Image> images = fullProduct.getImages();
        if (images.isEmpty()) {
            messageLabel.setText("No product photo available.");
            return;
        }
        if (images.size() == 1) {
            openProductImage(fullProduct.getProductName(), images.get(0));
            return;
        }

        TilePane imagePane = new TilePane();
        imagePane.setHgap(12);
        imagePane.setVgap(12);
        imagePane.setPrefColumns(3);
        imagePane.setAlignment(Pos.CENTER_LEFT);
        for (Image image : images) {
            ImageView imageView = new ImageView(image);
            imageView.setFitWidth(180);
            imageView.setFitHeight(130);
            imageView.setPreserveRatio(true);
            imageView.setPickOnBounds(true);
            imageView.getStyleClass().add("product-image-preview");
            imageView.setOnMouseClicked(event -> {
                openProductImage(fullProduct.getProductName(), image);
                event.consume();
            });
            imagePane.getChildren().add(imageView);
        }

        ScrollPane imageScroll = new ScrollPane(imagePane);
        imageScroll.setFitToWidth(true);
        imageScroll.setPannable(true);
        imageScroll.getStyleClass().add("content-scroll");

        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Product Photos");
        dialog.setHeaderText(fullProduct.getProductName());
        dialog.getDialogPane().getButtonTypes().add(new ButtonType("Close", ButtonBar.ButtonData.CANCEL_CLOSE));
        dialog.getDialogPane().setContent(imageScroll);
        sizeDialogToScreen(dialog, 720, 560);
        dialog.setResizable(true);
        dialog.showAndWait();
    }

    private void openProductImage(String title, Image image) {
        if (image == null) {
            messageLabel.setText("No product photo available.");
            return;
        }

        ImageView imageView = new ImageView(image);
        imageView.setPreserveRatio(true);
        imageView.setFitWidth(760);
        imageView.setFitHeight(520);

        ScrollPane imageScroll = new ScrollPane(imageView);
        imageScroll.setFitToWidth(true);
        imageScroll.setFitToHeight(true);
        imageScroll.setPannable(true);
        imageScroll.getStyleClass().add("content-scroll");

        VBox content = new VBox(12, imageScroll);
        content.setPrefSize(820, 600);

        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Product Photo");
        dialog.setHeaderText(title);
        dialog.getDialogPane().getButtonTypes().add(new ButtonType("Close", ButtonBar.ButtonData.CANCEL_CLOSE));
        dialog.getDialogPane().setContent(content);
        sizeDialogToScreen(dialog, 860, 680);
        dialog.setResizable(true);
        dialog.showAndWait();
    }

    @FXML
    private void backToDashboard() {
        showDashboard();
    }

    private record EditableProductImage(Long savedImageId, NewProductImage newImage, Image image) {

        private static EditableProductImage savedImage(Long savedImageId, Image image) {
            return new EditableProductImage(savedImageId, null, image);
        }

        private static EditableProductImage newImage(NewProductImage newImage, Image image) {
            return new EditableProductImage(null, newImage, image);
        }
    }

    private static class RestockProductRow {

        private final String productName;
        private final String brandName;
        private final String vehicleTypeName;
        private final String modelCode;
        private final String productLocation;
        private final String drNumber;
        private final int quantity;
        private final BigDecimal unitPrice;
        private final java.util.List<NewProductImage> productImages;
        private Image thumbnailImage;
        private boolean thumbnailLoaded;

        private RestockProductRow(
                String productName,
                String brandName,
                String vehicleTypeName,
                String modelCode,
                String productLocation,
                String drNumber,
                int quantity,
                BigDecimal unitPrice,
                java.util.List<NewProductImage> productImages) {
            this.productName = productName;
            this.brandName = brandName;
            this.vehicleTypeName = vehicleTypeName;
            this.modelCode = modelCode;
            this.productLocation = productLocation;
            this.drNumber = drNumber;
            this.quantity = quantity;
            this.unitPrice = unitPrice;
            this.productImages = productImages == null ? List.of() : productImages;
        }

        private static RestockProductRow fromFields(
                String productName,
                String brandName,
                String vehicleTypeName,
                String modelCode,
                String productLocation,
                String drNumber,
                String quantityText,
                String priceText,
                java.util.List<NewProductImage> productImages) {
            String cleanProductName = requireText(productName, "Product");
            String cleanBrandName = requireText(brandName, "Brand");
            String cleanVehicleTypeName = requireText(vehicleTypeName, "Vehicle");
            String cleanModelCode = requireText(modelCode, "Model");
            String cleanProductLocation = trimToNullText(productLocation);
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
                    cleanProductLocation,
                    cleanDrNumber,
                    quantity,
                    unitPrice,
                    productImages);
        }

        private static String requireText(String value, String fieldName) {
            if (value == null || value.isBlank()) {
                throw new BusinessException(fieldName + " is required.");
            }
            return value.trim();
        }

        private static String trimToNullText(String value) {
            return value == null || value.isBlank() ? null : value.trim();
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

        private String getProductLocation() {
            return productLocation == null ? "" : productLocation;
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
            if (!thumbnailLoaded) {
                thumbnailImage = createThumbnailImage();
                thumbnailLoaded = true;
            }
            return thumbnailImage;
        }

        private Image createThumbnailImage() {
            if (productImages.isEmpty()
                    || productImages.get(0).data() == null
                    || productImages.get(0).data().length == 0) {
                return null;
            }
            return new Image(new ByteArrayInputStream(productImages.get(0).data()), 120, 90, true, true);
        }

        private CreateProductCommand toCreateCommand() {
            return new CreateProductCommand(
                    productName,
                    brandName,
                    vehicleTypeName,
                    modelCode,
                    productLocation,
                    quantity,
                    unitPrice,
                    productImages,
                    null,
                    drNumber);
        }
    }
}
