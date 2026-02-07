package cryptographie.maya.controller;

import cryptographie.maya.crypto.RsaKeyManager;
import cryptographie.maya.dao.DatabaseManager;
import de.mkammerer.argon2.Argon2;
import de.mkammerer.argon2.Argon2Factory;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.io.IOException;
import java.security.KeyPair;
import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLIntegrityConstraintViolationException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Arrays;
import java.util.Base64;

public class RegisterController {

    @FXML private TextField firstNameTextField, lastNameTextField, userNameTextField;
    @FXML private PasswordField passwordField, confirmPasswordField;
    @FXML private Button registerButton, closeButton;

    @FXML
    private void handleRegister() {
        String firstName = safeGetTrimmed(firstNameTextField);
        String lastName  = safeGetTrimmed(lastNameTextField);
        String username  = safeGetTrimmed(userNameTextField);
        String password  = passwordField.getText();
        String confirm   = confirmPasswordField.getText();

        if (firstName.isEmpty() || lastName.isEmpty() || username.isEmpty() || password.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Champs manquants", "Veuillez remplir tous les champs.");
            return;
        }
        if (!password.equals(confirm)) {
            showAlert(Alert.AlertType.WARNING, "Erreur", "Les mots de passe ne correspondent pas.");
            return;
        }

        final char[] pwChars = password.toCharArray();
        registerButton.setDisable(true);
        registerButton.setText("Chiffrement en cours...");

        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {
                Argon2 argon2 = Argon2Factory.create(Argon2Factory.Argon2Types.ARGON2id);
                try {
                    String passwordHash = argon2.hash(3, 65536, 1, pwChars);

                    // ✅ salt obligatoire en DB (NOT NULL)
                    String saltB64 = generateSaltBase64(16);

                    // Génération RSA
                    KeyPair kp = RsaKeyManager.generateKeyPair();
                    String publicKeyB64 = RsaKeyManager.publicKeyToBase64(kp.getPublic());

                    String sql = """
                        INSERT INTO users (first_name, last_name, username, password_hash, salt, created_at, public_key)
                        VALUES (?,?,?,?,?,?,?)
                        """;

                    try (Connection conn = DatabaseManager.getConnection();
                         PreparedStatement ps = conn.prepareStatement(sql)) {

                        ps.setString(1, firstName);
                        ps.setString(2, lastName);
                        ps.setString(3, username);
                        ps.setString(4, passwordHash);
                        ps.setString(5, saltB64); // ✅ ajouté
                        ps.setTimestamp(6, Timestamp.from(Instant.now()));
                        ps.setString(7, publicKeyB64);
                        ps.executeUpdate();
                    }

                    RsaKeyManager.savePrivateKey(username, kp.getPrivate());

                } finally {
                    Arrays.fill(pwChars, '\0');
                    argon2.wipeArray(pwChars);
                }
                return null;
            }
        };

        task.setOnSucceeded(e -> {
            showAlert(Alert.AlertType.INFORMATION, "Succès", "Coffre-fort créé avec succès !");
            handleBackToLogin();
        });

        task.setOnFailed(e -> {
            registerButton.setDisable(false);
            registerButton.setText("RÉESSAYER");

            Throwable ex = task.getException();
            ex.printStackTrace(); // ✅ affiche la vraie erreur en console

            if (ex instanceof SQLIntegrityConstraintViolationException) {
                showAlert(Alert.AlertType.ERROR, "Erreur", "Ce nom d'utilisateur existe déjà.");
            } else {
                showAlert(Alert.AlertType.ERROR, "Erreur", "Échec de l'inscription: " + ex.getMessage());
            }
        });

        new Thread(task).start();
    }

    private static String generateSaltBase64(int bytes) {
        byte[] salt = new byte[bytes];
        new SecureRandom().nextBytes(salt);
        return Base64.getEncoder().encodeToString(salt);
    }

    @FXML
    private void handleBackToLogin() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/fxml/login.fxml"));
            Stage stage = (Stage) closeButton.getScene().getWindow();
            stage.setScene(new Scene(root, 1280, 720));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String safeGetTrimmed(TextField tf) { return tf.getText() == null ? "" : tf.getText().trim(); }

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