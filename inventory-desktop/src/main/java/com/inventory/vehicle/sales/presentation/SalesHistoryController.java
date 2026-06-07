package com.inventory.vehicle.sales.presentation;

import com.inventory.vehicle.auth.application.SessionService;
import com.inventory.vehicle.navigation.SceneManager;
import com.inventory.vehicle.navigation.SidebarController;
import com.inventory.vehicle.sales.application.SalesQueryService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import javafx.fxml.FXML;
import javafx.scene.control.DatePicker;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import org.springframework.stereotype.Controller;

@Controller
public class SalesHistoryController extends SidebarController {

    private final SalesQueryService salesQueryService;

    @FXML
    private DatePicker soldDatePicker;

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
    private TableColumn<SaleTableRow, LocalDateTime> createdAtColumn;

    public SalesHistoryController(SceneManager sceneManager, SessionService sessionService, SalesQueryService salesQueryService) {
        super(sceneManager, sessionService);
        this.salesQueryService = salesQueryService;
    }

    @FXML
    private void initialize() {
        idColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        sellerColumn.setCellValueFactory(new PropertyValueFactory<>("sellerName"));
        soldDateColumn.setCellValueFactory(new PropertyValueFactory<>("soldDate"));
        totalColumn.setCellValueFactory(new PropertyValueFactory<>("totalAmount"));
        encodedByColumn.setCellValueFactory(new PropertyValueFactory<>("encodedBy"));
        createdAtColumn.setCellValueFactory(new PropertyValueFactory<>("createdAt"));
        loadAllSales();
    }

    @FXML
    private void loadSalesByDate() {
        if (soldDatePicker.getValue() == null) {
            loadAllSales();
            return;
        }

        salesTable.getItems().setAll(salesQueryService.findSalesByDate(soldDatePicker.getValue())
                .stream()
                .map(SaleTableRow::new)
                .toList());
    }

    @FXML
    private void loadAllSales() {
        soldDatePicker.setValue(null);
        salesTable.getItems().setAll(salesQueryService.findAllSales()
                .stream()
                .map(SaleTableRow::new)
                .toList());
    }

    @FXML
    private void backToDashboard() {
        showDashboard();
    }
}
