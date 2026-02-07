package cryptographie.maya.controller;

import cryptographie.maya.model.SecureItem;
import cryptographie.maya.model.User;
import cryptographie.maya.service.AdminService;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.chart.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class AdminDashboardController {

    @FXML private SplitPane mainSplit;

    // Users table + columns
    @FXML private TableView<UserRow> usersTable;
    @FXML private TableColumn<UserRow, Integer> colUserId;
    @FXML private TableColumn<UserRow, String> colUsername;
    @FXML private TableColumn<UserRow, String> colEmail;
    @FXML private TableColumn<UserRow, String> colRole;
    @FXML private TableColumn<UserRow, Integer> colItemCount;
    @FXML private TableColumn<UserRow, String> colCreated;

    // Items table + columns
    @FXML private TableView<ItemRow> itemsTable;
    @FXML private TableColumn<ItemRow, Integer> colItemId;
    @FXML private TableColumn<ItemRow, String> colItemTitle;
    @FXML private TableColumn<ItemRow, String> colItemType;

    // Buttons
    @FXML private Button refreshUsersBtn;
    @FXML private Button promoteBtn;
    @FXML private Button demoteBtn;
    @FXML private Button deleteUserBtn;
    @FXML private Button viewItemsBtn;
    @FXML private Button logoutBtn;

    // Stats & filters
    @FXML private Label lblTotalUsers;
    @FXML private Label lblAdmins;
    @FXML private Label lblTotalItems;
    @FXML private TextField searchField;
    @FXML private ChoiceBox<String> roleFilter;

    // Detail pane
    @FXML private ImageView avatarView;
    @FXML private Label lblUserName, lblUserEmail, lblUserRole, lblUserCreated;
    @FXML private Label lblPublicKeyLabel;
    @FXML private TextArea txtPublicKey;

    // Charts
    @FXML private PieChart roleChart;
    @FXML private BarChart<String, Number> itemsBarChart;
    @FXML private CategoryAxis itemsCategoryAxis;
    @FXML private NumberAxis itemsNumberAxis;

    private final AdminService adminService = new AdminService();
    private final ObservableList<UserRow> users = FXCollections.observableArrayList();
    private final ObservableList<ItemRow> items = FXCollections.observableArrayList();

    private final DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    // cached default avatar
    private Image defaultAvatar;

    @FXML
    public void initialize() {
        // load default avatar (place file at src/main/resources/img/default-avatar.png)
        defaultAvatar = loadDefaultAvatar("/img/default-avatar.png");

        // User table column bindings
        if (colUserId != null)    colUserId.setCellValueFactory(c -> c.getValue().idProperty().asObject());
        if (colUsername != null)  colUsername.setCellValueFactory(c -> c.getValue().usernameProperty());
        if (colEmail != null)     colEmail.setCellValueFactory(c -> c.getValue().emailProperty());
        if (colRole != null)      colRole.setCellValueFactory(c -> c.getValue().roleProperty());
        if (colItemCount != null) colItemCount.setCellValueFactory(c -> c.getValue().itemCountProperty().asObject());
        if (colCreated != null)   colCreated.setCellValueFactory(c -> c.getValue().createdAtProperty());

        // Items table column bindings
        if (colItemId != null)    colItemId.setCellValueFactory(c -> c.getValue().idProperty().asObject());
        if (colItemTitle != null) colItemTitle.setCellValueFactory(c -> c.getValue().titleProperty());
        if (colItemType != null)  colItemType.setCellValueFactory(c -> c.getValue().typeProperty());

        // Set up tables
        if (usersTable != null) {
            usersTable.setItems(users);
            usersTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
            usersTable.getSelectionModel().selectedItemProperty().addListener((obs, oldSel, newSel) -> onUserSelectionChanged(newSel));
            VBox.setVgrow(usersTable, Priority.ALWAYS);
            usersTable.setRowFactory(tv -> {
                TableRow<UserRow> row = new TableRow<>();
                row.setOnMouseClicked(e -> {
                    if (e.getClickCount() == 2 && !row.isEmpty()) {
                        onUserSelectionChanged(row.getItem());
                    }
                });
                return row;
            });
        }

        if (itemsTable != null) {
            itemsTable.setItems(items);
            itemsTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
            VBox.setVgrow(itemsTable, Priority.ALWAYS);
        }

        // Buttons
        if (refreshUsersBtn != null) refreshUsersBtn.setOnAction(e -> refreshAll());
        if (promoteBtn != null)      promoteBtn.setOnAction(e -> changeRoleSelected("ADMIN"));
        if (demoteBtn != null)       demoteBtn.setOnAction(e -> changeRoleSelected("USER"));
        if (deleteUserBtn != null)   deleteUserBtn.setOnAction(e -> deleteSelectedUser());
        if (viewItemsBtn != null)    viewItemsBtn.setOnAction(e -> loadItemsForSelectedUser());
        // NE PAS remettre de setOnAction sur logoutBtn ici, on utilise onAction dans le FXML

        // Search & filter
        if (searchField != null) searchField.textProperty().addListener((obs, o, n) -> applyFilter());
        if (roleFilter != null) {
            roleFilter.setItems(FXCollections.observableArrayList("ALL", "ADMIN", "USER"));
            roleFilter.setValue("ALL");
            roleFilter.getSelectionModel().selectedItemProperty().addListener((obs, o, n) -> applyFilter());
        }

        // hide public key area by default
        hidePublicKeyArea();

        // ensure SplitPane divider after layout ready
        Platform.runLater(() -> {
            if (mainSplit != null) mainSplit.setDividerPositions(0.62);
            if (usersTable != null) VBox.setVgrow(usersTable, Priority.ALWAYS);
            if (itemsTable != null) VBox.setVgrow(itemsTable, Priority.ALWAYS);
        });

        // initial load
        refreshAll();
        loadCharts();
    }

    /* ------------------ LOGOUT HANDLER ------------------ */

    @FXML
    private void handleLogout(javafx.event.ActionEvent event) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Voulez-vous vraiment vous déconnecter ?",
                ButtonType.YES, ButtonType.NO);
        styleDialog(confirm);
        Window owner = usersTable == null || usersTable.getScene() == null ? null : usersTable.getScene().getWindow();
        if (owner != null) confirm.initOwner(owner);

        Optional<ButtonType> res = confirm.showAndWait();
        if (res.isEmpty() || res.get() != ButtonType.YES) {
            return;
        }

        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.close();
    }

    /* ------------------ RESTE DU CONTRÔLEUR ------------------ */

    private Image loadDefaultAvatar(String resPath) {
        try (InputStream is = getClass().getResourceAsStream(resPath)) {
            if (is != null) return new Image(is);
        } catch (Exception ignored) {}
        return new Image("data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR4nGNgYAAAAAMAAWgmWQ0AAAAASUVORK5CYII=");
    }

    private void hidePublicKeyArea() {
        if (txtPublicKey != null) {
            txtPublicKey.setVisible(false);
            txtPublicKey.setManaged(false);
        }
        if (lblPublicKeyLabel != null) {
            lblPublicKeyLabel.setVisible(false);
            lblPublicKeyLabel.setManaged(false);
        }
    }

    private void showPublicKeyArea() {
        if (txtPublicKey != null) {
            txtPublicKey.setVisible(true);
            txtPublicKey.setManaged(true);
        }
        if (lblPublicKeyLabel != null) {
            lblPublicKeyLabel.setVisible(true);
            lblPublicKeyLabel.setManaged(true);
        }
    }

    private void refreshAll() {
        CompletableFuture.runAsync(() -> {
            try {
                List<User> list = adminService.listAllUsers();
                if (list == null) list = Collections.emptyList();

                List<UserRow> rows = new ArrayList<>();
                int totalItems = 0;
                int admins = 0;
                for (User u : list) {
                    int count = 0;
                    try {
                        List<SecureItem> userItems = adminService.listItemsForUser(u.getId());
                        count = (userItems == null) ? 0 : userItems.size();
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                    totalItems += count;
                    if ("ADMIN".equalsIgnoreCase(u.getRole())) admins++;
                    String created = u.getCreatedAt() == null ? "N/A" : dtf.format(u.getCreatedAt());
                    rows.add(new UserRow(u.getId(), u.getUsername(), safe(u.getEmail()), u.getRole(), count, created, safe(u.getPublicKey())));
                }

                final int finalTotalItems = totalItems;
                final int finalAdmins = admins;
                Platform.runLater(() -> {
                    users.setAll(rows);
                    if (lblTotalUsers != null) lblTotalUsers.setText(String.valueOf(rows.size()));
                    if (lblAdmins != null) lblAdmins.setText(String.valueOf(finalAdmins));
                    if (lblTotalItems != null) lblTotalItems.setText(String.valueOf(finalTotalItems));
                    applyFilter();
                    loadCharts();
                });
            } catch (Exception e) {
                e.printStackTrace();
                showDetailedErrorAlert("Impossible de charger les utilisateurs.", e);
            }
        });
    }

    private String safe(String s) { return s == null ? "" : s; }

    private void applyFilter() {
        if (users == null || users.isEmpty()) {
            if (usersTable != null) usersTable.setItems(users);
            return;
        }
        String q = (searchField == null) ? "" : searchField.getText().trim().toLowerCase();
        String role = (roleFilter == null) ? "ALL" : roleFilter.getValue();
        List<UserRow> filtered = users.stream()
                .filter(u -> {
                    boolean matchesQ = q.isEmpty()
                            || (u.getUsername() != null && u.getUsername().toLowerCase().contains(q))
                            || (u.getEmail() != null && u.getEmail().toLowerCase().contains(q));
                    boolean matchesRole = "ALL".equalsIgnoreCase(role) || (u.getRole() != null && u.getRole().equalsIgnoreCase(role));
                    return matchesQ && matchesRole;
                }).collect(Collectors.toList());
        usersTable.setItems(FXCollections.observableArrayList(filtered));
    }

    private UserRow getSelectedUserRow() {
        return (usersTable == null) ? null : usersTable.getSelectionModel().getSelectedItem();
    }

    private void onUserSelectionChanged(UserRow row) {
        items.clear();
        if (row == null) {
            clearDetailPane();
            return;
        }
        if (lblUserName != null)   lblUserName.setText(row.getUsername());
        if (lblUserEmail != null)  lblUserEmail.setText(row.getEmail() == null ? "-" : row.getEmail());
        if (lblUserRole != null)   lblUserRole.setText(row.getRole());
        if (lblUserCreated != null) lblUserCreated.setText("Créé le: " + row.getCreatedAt());

        if (avatarView != null) {
            avatarView.setImage(defaultAvatar);
            avatarView.setFitWidth(96);
            avatarView.setFitHeight(96);
            avatarView.setPreserveRatio(true);
        }

        hidePublicKeyArea();

        CompletableFuture.runAsync(() -> {
            try {
                List<SecureItem> its = adminService.listItemsForUser(row.getId());
                if (its == null) its = Collections.emptyList();
                List<ItemRow> itsRows = its.stream()
                        .map(si -> new ItemRow(si.getId(), si.getTitle(), si.getItemType()))
                        .collect(Collectors.toList());
                System.out.println("Loaded items for user " + row.getUsername() + " (id=" + row.getId() + "): " + itsRows.size());
                Platform.runLater(() -> items.setAll(itsRows));
            } catch (Exception e) {
                e.printStackTrace();
                showDetailedErrorAlert("Impossible de charger les éléments.", e);
            }
        });
    }

    private void loadItemsForSelectedUser() {
        UserRow sel = getSelectedUserRow();
        if (sel == null) {
            showAlert(Alert.AlertType.WARNING, "Sélection", "Veuillez sélectionner un utilisateur.");
            return;
        }
        onUserSelectionChanged(sel);
    }

    private void changeRoleSelected(String newRole) {
        UserRow sel = getSelectedUserRow();
        if (sel == null) {
            showAlert(Alert.AlertType.WARNING, "Sélection", "Veuillez sélectionner un utilisateur.");
            return;
        }
        if (sel.getRole() != null && sel.getRole().equalsIgnoreCase(newRole)) {
            showAlert(Alert.AlertType.INFORMATION, "Info", "L'utilisateur a déjà le rôle " + newRole);
            return;
        }
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "Changer le rôle de " + sel.getUsername() + " en " + newRole + " ?", ButtonType.YES, ButtonType.NO);
        styleDialog(confirm);
        Window owner = usersTable == null || usersTable.getScene() == null ? null : usersTable.getScene().getWindow();
        if (owner != null) confirm.initOwner(owner);
        Optional<ButtonType> res = confirm.showAndWait();
        if (res.isEmpty() || res.get() != ButtonType.YES) return;

        CompletableFuture.runAsync(() -> {
            try {
                boolean ok = adminService.updateUserRole(sel.getId(), newRole);
                if (ok) {
                    sel.setRole(newRole);
                    Platform.runLater(() -> {
                        usersTable.refresh();
                        showAlert(Alert.AlertType.INFORMATION, "Succès", "Rôle mis à jour.");
                    });
                } else {
                    Platform.runLater(() -> showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible de mettre à jour le rôle."));
                }
            } catch (Exception e) {
                e.printStackTrace();
                showDetailedErrorAlert("Erreur lors de la mise à jour du rôle.", e);
            }
        });
    }

    private void deleteSelectedUser() {
        UserRow sel = getSelectedUserRow();
        if (sel == null) {
            showAlert(Alert.AlertType.WARNING, "Sélection", "Veuillez sélectionner un utilisateur.");
            return;
        }
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "Supprimer l'utilisateur " + sel.getUsername() + " et tout son contenu ?", ButtonType.YES, ButtonType.NO);
        styleDialog(confirm);
        Window owner = usersTable == null || usersTable.getScene() == null ? null : usersTable.getScene().getWindow();
        if (owner != null) confirm.initOwner(owner);
        Optional<ButtonType> res = confirm.showAndWait();
        if (res.isEmpty() || res.get() != ButtonType.YES) return;

        CompletableFuture.runAsync(() -> {
            try {
                boolean ok = adminService.deleteUser(sel.getId());
                if (ok) {
                    Platform.runLater(() -> {
                        users.remove(sel);
                        items.clear();
                        showAlert(Alert.AlertType.INFORMATION, "Succès", "Utilisateur supprimé.");
                    });
                } else {
                    Platform.runLater(() -> showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible de supprimer l'utilisateur."));
                }
            } catch (Exception e) {
                e.printStackTrace();
                showDetailedErrorAlert("Erreur lors de la suppression de l'utilisateur.", e);
            }
        });
    }

    private void clearDetailPane() {
        if (lblUserName != null)   lblUserName.setText("Sélectionnez un utilisateur");
        if (lblUserEmail != null)  lblUserEmail.setText("-");
        if (lblUserRole != null)   lblUserRole.setText("-");
        if (lblUserCreated != null) lblUserCreated.setText("-");
        if (txtPublicKey != null)  txtPublicKey.clear();
        if (avatarView != null)    avatarView.setImage(null);
        items.clear();
    }

    // Charts
    private void loadCharts() {
        CompletableFuture.runAsync(() -> {
            try {
                Map<String, Integer> roleCounts = adminService.getRoleCounts();
                List<Map.Entry<String, Integer>> topUsers = adminService.getTopItemsPerUser(10);
                Platform.runLater(() -> {
                    populateRoleChart(roleCounts);
                    populateItemsBarChart(topUsers);
                });
            } catch (Exception e) {
                e.printStackTrace();
                showDetailedErrorAlert("Impossible de charger les graphiques.", e);
            }
        });
    }

    private void populateRoleChart(Map<String, Integer> roleCounts) {
        if (roleChart == null) return;
        roleChart.getData().clear();
        if (roleCounts == null || roleCounts.isEmpty()) {
            roleChart.setTitle("Aucune donnée");
            return;
        }
        for (Map.Entry<String, Integer> e : roleCounts.entrySet()) {
            PieChart.Data slice = new PieChart.Data(e.getKey(), e.getValue());
            roleChart.getData().add(slice);
        }
        roleChart.setLegendVisible(true);
    }

    private void populateItemsBarChart(List<Map.Entry<String, Integer>> topUsers) {
        if (itemsBarChart == null) return;
        itemsBarChart.getData().clear();
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Items");
        if (topUsers != null) {
            for (Map.Entry<String, Integer> e : topUsers) {
                String label = e.getKey() == null ? "?" : e.getKey();
                series.getData().add(new XYChart.Data<>(label, e.getValue()));
            }
        }
        itemsBarChart.getData().add(series);
        if (itemsCategoryAxis != null) itemsCategoryAxis.setTickLabelRotation(-45);
    }

    // Alerts helpers
    private void showAlert(Alert.AlertType t, String title, String msg) {
        Platform.runLater(() -> {
            Alert a = new Alert(t);
            styleDialog(a);
            a.setTitle(title);
            a.setHeaderText(null);
            a.setContentText(msg);
            Window owner = usersTable == null || usersTable.getScene() == null ? null : usersTable.getScene().getWindow();
            if (owner != null) a.initOwner(owner);
            a.showAndWait();
        });
    }

    private void showDetailedErrorAlert(String header, Throwable ex) {
        Platform.runLater(() -> {
            Alert a = new Alert(Alert.AlertType.ERROR);
            styleDialog(a);
            a.setTitle("Erreur");
            a.setHeaderText(header);
            a.setContentText(ex == null ? "Erreur inconnue" : ex.toString());
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            if (ex != null) ex.printStackTrace(pw);
            TextArea ta = new TextArea(sw.toString());
            ta.setEditable(false);
            ta.setWrapText(true);
            ta.setPrefRowCount(10);
            a.getDialogPane().setExpandableContent(ta);
            Window owner = usersTable == null || usersTable.getScene() == null ? null : usersTable.getScene().getWindow();
            if (owner != null) a.initOwner(owner);
            a.showAndWait();
        });
    }

    private void styleDialog(Dialog<?> d) {
        if (d != null && d.getDialogPane() != null) {
            d.getDialogPane().getStyleClass().add("dialog-pane");
        }
    }

    // Row wrappers
    public static class UserRow {
        private final javafx.beans.property.IntegerProperty id;
        private final javafx.beans.property.StringProperty username;
        private final javafx.beans.property.StringProperty email;
        private final javafx.beans.property.StringProperty role;
        private final javafx.beans.property.IntegerProperty itemCount;
        private final javafx.beans.property.StringProperty createdAt;
        private final String publicKey;

        public UserRow(int id, String username, String email, String role, int itemCount, String createdAt, String publicKey) {
            this.id = new javafx.beans.property.SimpleIntegerProperty(id);
            this.username = new javafx.beans.property.SimpleStringProperty(username);
            this.email = new javafx.beans.property.SimpleStringProperty(email == null ? "" : email);
            this.role = new javafx.beans.property.SimpleStringProperty(role == null ? "USER" : role);
            this.itemCount = new javafx.beans.property.SimpleIntegerProperty(itemCount);
            this.createdAt = new javafx.beans.property.SimpleStringProperty(createdAt == null ? "N/A" : createdAt);
            this.publicKey = publicKey;
        }

        public int getId() { return id.get(); }
        public String getUsername() { return username.get(); }
        public String getEmail() { return email.get(); }
        public String getRole() { return role.get(); }
        public int getItemCount() { return itemCount.get(); }
        public String getCreatedAt() { return createdAt.get(); }
        public String getPublicKey() { return publicKey; }

        public void setRole(String r) { this.role.set(r); }
        public void setItemCount(int c) { this.itemCount.set(c); }

        public javafx.beans.property.IntegerProperty idProperty() { return id; }
        public javafx.beans.property.StringProperty usernameProperty() { return username; }
        public javafx.beans.property.StringProperty emailProperty() { return email; }
        public javafx.beans.property.StringProperty roleProperty() { return role; }
        public javafx.beans.property.IntegerProperty itemCountProperty() { return itemCount; }
        public javafx.beans.property.StringProperty createdAtProperty() { return createdAt; }
    }

    public static class ItemRow {
        private final javafx.beans.property.IntegerProperty id;
        private final javafx.beans.property.StringProperty title;
        private final javafx.beans.property.StringProperty type;

        public ItemRow(int id, String title, String type) {
            this.id = new javafx.beans.property.SimpleIntegerProperty(id);
            this.title = new javafx.beans.property.SimpleStringProperty(title);
            this.type = new javafx.beans.property.SimpleStringProperty(type);
        }

        public int getId() { return id.get(); }
        public String getTitle() { return title.get(); }
        public String getType() { return type.get(); }

        public javafx.beans.property.IntegerProperty idProperty() { return id; }
        public javafx.beans.property.StringProperty titleProperty() { return title; }
        public javafx.beans.property.StringProperty typeProperty() { return type; }
    }
}