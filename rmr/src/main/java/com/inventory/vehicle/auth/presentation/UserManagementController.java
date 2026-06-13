package com.inventory.vehicle.auth.presentation;

import com.inventory.vehicle.auth.application.SessionService;
import com.inventory.vehicle.auth.application.UserManagementService;
import com.inventory.vehicle.auth.domain.Role;
import com.inventory.vehicle.common.exception.BusinessException;
import com.inventory.vehicle.navigation.SceneManager;
import com.inventory.vehicle.navigation.SidebarController;
import java.time.LocalDate;
import java.util.List;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import org.springframework.stereotype.Controller;

@Controller
public class UserManagementController extends SidebarController {

    private final UserManagementService userManagementService;
    private List<LoginHistoryTableRow> allLoginHistory = List.of();

    @FXML
    private TableView<UserAccountTableRow> usersTable;

    @FXML
    private TableColumn<UserAccountTableRow, Long> userIdColumn;

    @FXML
    private TableColumn<UserAccountTableRow, String> usernameColumn;

    @FXML
    private TableColumn<UserAccountTableRow, Role> roleColumn;

    @FXML
    private TableColumn<UserAccountTableRow, String> statusColumn;

    @FXML
    private TableColumn<UserAccountTableRow, String> createdAtColumn;

    @FXML
    private TextField usernameField;

    @FXML
    private ComboBox<Role> roleComboBox;

    @FXML
    private CheckBox activeCheckBox;

    @FXML
    private PasswordField newPasswordField;

    @FXML
    private TableView<LoginHistoryTableRow> loginHistoryTable;

    @FXML
    private TableColumn<LoginHistoryTableRow, String> loginUsernameColumn;

    @FXML
    private TableColumn<LoginHistoryTableRow, String> loginRoleColumn;

    @FXML
    private TableColumn<LoginHistoryTableRow, String> loginDateColumn;

    @FXML
    private TableColumn<LoginHistoryTableRow, String> loginTimeColumn;

    @FXML
    private ComboBox<String> loginHistoryFilterComboBox;

    @FXML
    private Label messageLabel;
    private String loginHistoryFilter = "ALL";

    public UserManagementController(
            SceneManager sceneManager,
            SessionService sessionService,
            UserManagementService userManagementService
    ) {
        super(sceneManager, sessionService);
        this.userManagementService = userManagementService;
    }

    @FXML
    private void initialize() {
        userIdColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        usernameColumn.setCellValueFactory(new PropertyValueFactory<>("username"));
        roleColumn.setCellValueFactory(new PropertyValueFactory<>("role"));
        statusColumn.setCellValueFactory(new PropertyValueFactory<>("status"));
        createdAtColumn.setCellValueFactory(new PropertyValueFactory<>("createdAt"));
        usersTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);

        loginUsernameColumn.setCellValueFactory(new PropertyValueFactory<>("username"));
        loginRoleColumn.setCellValueFactory(new PropertyValueFactory<>("role"));
        loginDateColumn.setCellValueFactory(new PropertyValueFactory<>("loginDate"));
        loginTimeColumn.setCellValueFactory(new PropertyValueFactory<>("loginTime"));
        loginHistoryTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        loginHistoryFilterComboBox.getItems().setAll("All", "Today", "Week", "Month");
        loginHistoryFilterComboBox.setValue("All");
        loginHistoryFilterComboBox.valueProperty().addListener((observable, oldValue, newValue) -> {
            loginHistoryFilter = newValue == null ? "ALL" : newValue.toUpperCase();
            applyLoginHistoryFilter();
        });

        roleComboBox.getItems().setAll(Role.ADMIN, Role.EMPLOYEE);
        activeCheckBox.setSelected(true);
        usersTable.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, selectedUser) -> {
            if (selectedUser != null) {
                showSelectedUser(selectedUser);
            }
        });

        refresh();
    }

    @FXML
    private void refresh() {
        usersTable.getItems().setAll(userManagementService.listUsers()
                .stream()
                .map(UserAccountTableRow::new)
                .toList());
        allLoginHistory = userManagementService.loginHistory()
                .stream()
                .map(LoginHistoryTableRow::new)
                .toList();
        applyLoginHistoryFilter();
        messageLabel.setText("");
    }

    private void applyLoginHistoryFilter() {
        LocalDate today = LocalDate.now();
        loginHistoryTable.getItems().setAll(allLoginHistory.stream()
                .filter(row -> switch (loginHistoryFilter) {
                    case "TODAY" -> row.getLoggedInAt().toLocalDate().isEqual(today);
                    case "WEEK" -> !row.getLoggedInAt().toLocalDate().isBefore(today.minusDays(6));
                    case "MONTH" -> row.getLoggedInAt().getYear() == today.getYear()
                            && row.getLoggedInAt().getMonth() == today.getMonth();
                    default -> true;
                })
                .toList());
    }

    @FXML
    private void saveAccount() {
        UserAccountTableRow selectedUser = usersTable.getSelectionModel().getSelectedItem();
        if (selectedUser == null) {
            messageLabel.setText("Select an account to edit.");
            return;
        }

        try {
            userManagementService.updateUser(
                    selectedUser.getId(),
                    usernameField.getText(),
                    roleComboBox.getValue(),
                    activeCheckBox.isSelected(),
                    newPasswordField.getText()
            );
            newPasswordField.clear();
            refresh();
            messageLabel.setText("Account updated.");
        } catch (BusinessException exception) {
            messageLabel.setText(exception.getMessage());
        }
    }

    @FXML
    private void createAccount() {
        try {
            userManagementService.createUser(
                    usernameField.getText(),
                    roleComboBox.getValue(),
                    activeCheckBox.isSelected(),
                    newPasswordField.getText()
            );
            clearForm();
            refresh();
            messageLabel.setText("Account created.");
        } catch (BusinessException exception) {
            messageLabel.setText(exception.getMessage());
        }
    }

    @FXML
    private void deleteAccount() {
        UserAccountTableRow selectedUser = usersTable.getSelectionModel().getSelectedItem();
        if (selectedUser == null) {
            messageLabel.setText("Select an account to delete.");
            return;
        }
        if (!confirmDelete(selectedUser)) {
            return;
        }

        try {
            userManagementService.deleteUser(selectedUser.getId());
            clearForm();
            refresh();
            messageLabel.setText("Account deleted.");
        } catch (BusinessException exception) {
            messageLabel.setText(exception.getMessage());
        }
    }

    @FXML
    private void clearForm() {
        usersTable.getSelectionModel().clearSelection();
        usernameField.clear();
        roleComboBox.setValue(Role.EMPLOYEE);
        activeCheckBox.setSelected(true);
        newPasswordField.clear();
        messageLabel.setText("");
    }

    private void showSelectedUser(UserAccountTableRow user) {
        usernameField.setText(user.getUsername());
        roleComboBox.setValue(user.getRole());
        activeCheckBox.setSelected(user.isActive());
        newPasswordField.clear();
        messageLabel.setText("Editing " + user.getUsername() + ". Leave password blank to keep it.");
    }

    private boolean confirmDelete(UserAccountTableRow user) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Delete account?");
        alert.setHeaderText("Delete " + user.getUsername() + "?");
        alert.setContentText("This removes the login account but keeps sales and audit records.");
        return alert.showAndWait()
                .filter(buttonType -> buttonType == ButtonType.OK)
                .isPresent();
    }
}
