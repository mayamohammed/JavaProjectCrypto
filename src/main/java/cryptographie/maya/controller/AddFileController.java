package cryptographie.maya.controller;

import cryptographie.maya.service.DriveService;
import cryptographie.maya.service.SessionManager;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.FileChooser;
import javafx.stage.Window;

import java.io.File;
import java.nio.file.Path;
import java.util.Optional;

public class AddFileController {

    @FXML private Button chooseFileButton;
    @FXML private Button closeButton;

    private final DriveService driveService = new DriveService();
    private Runnable onSaved;

    public void setOnSaved(Runnable onSaved) {
        this.onSaved = onSaved;
    }

    @FXML
    private void initialize() {
        // Tu peux garder, mais ce n’est pas obligatoire si onAction est dans le FXML
        // if (chooseFileButton != null) chooseFileButton.setOnAction(e -> handleChooseAndUpload());
        // if (closeButton != null) closeButton.setOnAction(e -> handleClose());
    }

    @FXML
    public void handleChooseAndUpload() {
        if (!SessionManager.isLoggedIn()) {
            showAlert(Alert.AlertType.ERROR, "Session", "Utilisateur non connecté.");
            return;
        }

        Window owner = (chooseFileButton != null && chooseFileButton.getScene() != null)
                ? chooseFileButton.getScene().getWindow()
                : null;

        FileChooser fc = new FileChooser();
        fc.setTitle("Choisir un fichier à chiffrer et enregistrer");
        File file = (owner != null) ? fc.showOpenDialog(owner) : fc.showOpenDialog(null);
        if (file == null) return;

        String suggestedTitle = file.getName();
        TextInputDialog dialog = new TextInputDialog(suggestedTitle);
        dialog.setTitle("Titre");
        dialog.setHeaderText("Donner un titre à ce fichier");
        dialog.setContentText("Titre :");
        if (owner != null) dialog.initOwner(owner);

        Optional<String> titleOpt = dialog.showAndWait();
        if (titleOpt.isEmpty()) return;

        String title = titleOpt.get().trim();
        if (title.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Titre", "Titre invalide.");
            return;
        }

        int userId = SessionManager.getUserId();
        String username = SessionManager.getUsername();
        Path path = file.toPath();

        if (chooseFileButton != null) chooseFileButton.setDisable(true);

        Task<Integer> task = new Task<>() {
            @Override
            protected Integer call() throws Exception {
                return driveService.addFile(userId, username, title, path);
            }
        };

        task.setOnSucceeded(ev -> {
            if (chooseFileButton != null) chooseFileButton.setDisable(false);
            Integer itemId = task.getValue();

            Platform.runLater(() -> {
                showAlert(Alert.AlertType.INFORMATION, "Succès",
                        "Fichier chiffré et enregistré.\nItem ID = " + itemId);

                if (onSaved != null) onSaved.run();
                handleClose(); // ✅ réutilise la méthode
            });
        });

        task.setOnFailed(ev -> {
            if (chooseFileButton != null) chooseFileButton.setDisable(false);
            Throwable ex = task.getException();
            if (ex != null) ex.printStackTrace();
            Platform.runLater(() ->
                    showAlert(Alert.AlertType.ERROR, "Erreur",
                            "Upload échoué : " + (ex == null ? "inconnu" : ex.getMessage()))
            );
        });

        Thread th = new Thread(task, "upload-file");
        th.setDaemon(true);
        th.start();
    }

    @FXML
    private void handleClose() { // ✅ maintenant FXMLLoader peut l’appeler
        Window w = (closeButton != null && closeButton.getScene() != null)
                ? closeButton.getScene().getWindow()
                : null;
        if (w != null) w.hide();
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(type);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(message);

            // Récupération du DialogPane pour appliquer le CSS
            DialogPane dialogPane = alert.getDialogPane();

            // Chargement du fichier style.css
            String cssPath = getClass().getResource("/fxml/style.css").toExternalForm();
            dialogPane.getStylesheets().add(cssPath);

            // Ajout de la classe de style spécifique
            dialogPane.getStyleClass().add("cyber-alert");

            // Liaison avec la fenêtre parente pour le centrage
            Window owner = closeButton != null ? closeButton.getScene().getWindow() : null;
            if (owner != null) alert.initOwner(owner);

            alert.showAndWait();
        });
    }
}