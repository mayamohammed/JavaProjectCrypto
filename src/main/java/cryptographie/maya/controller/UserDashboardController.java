package cryptographie.maya.controller;

import cryptographie.maya.model.SecureItem;
import cryptographie.maya.service.DriveService;
import cryptographie.maya.service.SessionManager;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.Region;
import javafx.scene.text.Text;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class UserDashboardController {

    @FXML private Label usernameLabel;
    @FXML private Button logoutButton, addItemButton, editItemButton, deleteItemButton, downloadButton;
    @FXML private TableView<RowItem> itemsTable;
    @FXML private TableColumn<RowItem, Integer> colId;
    @FXML private TableColumn<RowItem, String> colTitle, colType, colCreatedAt;

    private final DriveService driveService = new DriveService();
    private final ObservableList<RowItem> items = FXCollections.observableArrayList();
    private static final DateTimeFormatter DT_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @FXML
    public void initialize() {
        // Liaison des colonnes avec RowItem
        if (colId != null) colId.setCellValueFactory(cell -> cell.getValue().idProperty().asObject());
        if (colTitle != null) colTitle.setCellValueFactory(cell -> cell.getValue().titleProperty());
        if (colType != null) colType.setCellValueFactory(cell -> cell.getValue().typeProperty());
        if (colCreatedAt != null) colCreatedAt.setCellValueFactory(cell -> cell.getValue().createdAtProperty());

        if (itemsTable != null) {
            itemsTable.setItems(items);
            itemsTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

            // Double-clic pour le téléchargement direct
            itemsTable.setRowFactory(tv -> {
                TableRow<RowItem> row = new TableRow<>();
                row.setOnMouseClicked(ev -> {
                    if (ev.getClickCount() == 2 && !row.isEmpty()) handleDownloadSelected();
                });
                return row;
            });
        }
    }

    public void setUsername(String username) {
        Platform.runLater(() -> {
            if (usernameLabel != null) usernameLabel.setText("ACCESS_GRANTEE: " + (username == null ? "" : username.toUpperCase()));
        });
        refreshItems();
    }

    @FXML
    public void refreshItems() {
        if (!SessionManager.isLoggedIn()) return;

        Task<List<RowItem>> loadTask = new Task<>() {
            @Override
            protected List<RowItem> call() throws Exception {
                List<SecureItem> dbItems = driveService.listItems(SessionManager.requireUserId());
                List<RowItem> rows = new ArrayList<>();
                for (SecureItem si : dbItems) {
                    String date = si.getCreatedAt() != null ? si.getCreatedAt().format(DT_FMT) : "N/A";
                    rows.add(new RowItem(si.getId(), si.getTitle(), si.getItemType(), date));
                }
                return rows;
            }
        };
        loadTask.setOnSucceeded(e -> items.setAll(loadTask.getValue()));
        new Thread(loadTask).start();
    }

    @FXML
    private void handleLogout() {
        try {
            SessionManager.clear();
            Parent loginRoot = FXMLLoader.load(getClass().getResource("/fxml/login.fxml"));
            Stage stage = (Stage) logoutButton.getScene().getWindow();
            stage.setScene(new Scene(loginRoot, 1280, 720)); // Taille fixe 1280x720
            stage.centerOnScreen();
        } catch (IOException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Déconnexion impossible.");
        }
    }

    @FXML
    private void handleDeleteItem() {
        RowItem sel = itemsTable.getSelectionModel().getSelectedItem();
        if (sel == null) {
            showAlert(Alert.AlertType.WARNING, "Sélection", "Veuillez sélectionner un élément.");
            return;
        }

        // Création de l'alerte stylisée
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "Supprimer '" + sel.getTitle() + "' ?", ButtonType.YES, ButtonType.NO);
        confirm.setTitle("BLUELOCKER - Confirmation");
        confirm.setHeaderText(null);

        applyCyberStyle(confirm); // Application du CSS

        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.YES) {
                new Thread(() -> {
                    try {
                        if (driveService.deleteItem(SessionManager.requireUserId(), sel.getId())) {
                            Platform.runLater(() -> items.remove(sel));
                        }
                    } catch (Exception e) {
                        Platform.runLater(() -> showAlert(Alert.AlertType.ERROR, "Erreur", "Échec de suppression."));
                    }
                }).start();
            }
        });
    }

    @FXML
    public void handleDownloadSelected() {
        RowItem sel = itemsTable.getSelectionModel().getSelectedItem();
        if (sel == null) return;

        new Thread(() -> {
            try {
                byte[] plain = driveService.downloadItemBytes(SessionManager.getUserId(), SessionManager.getUsername(), sel.getId());
                Platform.runLater(() -> {
                    FileChooser fc = new FileChooser();
                    fc.setInitialFileName(sel.getTitle());
                    File out = fc.showSaveDialog(itemsTable.getScene().getWindow());
                    if (out != null) {
                        try {
                            Files.write(out.toPath(), plain);
                            showAlert(Alert.AlertType.INFORMATION, "Succès", "Fichier déchiffré enregistré.");
                        } catch (IOException e) { showAlert(Alert.AlertType.ERROR, "Erreur", "Échec d'écriture."); }
                    }
                });
            } catch (Exception ex) {
                Platform.runLater(() -> showAlert(Alert.AlertType.ERROR, "Crypto Error", "Déchiffrement échoué."));
            }
        }).start();
    }

    @FXML
    private void handleAddItem() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/add_file.fxml"));
            Parent root = loader.load();
            ((AddFileController)loader.getController()).setOnSaved(this::refreshItems);

            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.initOwner(addItemButton.getScene().getWindow());
            stage.setScene(new Scene(root));
            stage.showAndWait();
        } catch (Exception e) { showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible d'ouvrir le formulaire."); }
    }

    @FXML private void handleEditItem() { showAlert(Alert.AlertType.INFORMATION, "Info", "Fonctionnalité à venir."); }

    // --- Utilitaires de Style ---

    private void applyCyberStyle(Dialog<?> dialog) {
        DialogPane dp = dialog.getDialogPane();
        String css = getClass().getResource("/fxml/style.css").toExternalForm();
        dp.getStylesheets().add(css);
        dp.getStyleClass().add("cyber-alert");
        if (itemsTable != null) dialog.initOwner(itemsTable.getScene().getWindow());
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(type, message);
            alert.setTitle(title);
            alert.setHeaderText(null);
            applyCyberStyle(alert);
            alert.showAndWait();
        });
    }

    // --- Modèle RowItem ---
    public static class RowItem {
        private final javafx.beans.property.IntegerProperty id;
        private final javafx.beans.property.StringProperty title, type, createdAt;

        public RowItem(int id, String title, String type, String createdAt) {
            this.id = new javafx.beans.property.SimpleIntegerProperty(id);
            this.title = new javafx.beans.property.SimpleStringProperty(title);
            this.type = new javafx.beans.property.SimpleStringProperty(type);
            this.createdAt = new javafx.beans.property.SimpleStringProperty(createdAt);
        }
        public int getId() { return id.get(); }
        public String getTitle() { return title.get(); }
        public String getType() { return type.get(); }
        public String getCreatedAt() { return createdAt.get(); }
        public javafx.beans.property.IntegerProperty idProperty() { return id; }
        public javafx.beans.property.StringProperty titleProperty() { return title; }
        public javafx.beans.property.StringProperty typeProperty() { return type; }
        public javafx.beans.property.StringProperty createdAtProperty() { return createdAt; }
    }
}