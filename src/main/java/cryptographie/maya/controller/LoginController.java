package cryptographie.maya.controller;

import cryptographie.maya.service.AuthService;
import cryptographie.maya.service.SessionManager;
import cryptographie.maya.service.AuthService.AuthException;
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
import java.lang.reflect.Method;
import java.util.Arrays;

public class LoginController {

    @FXML private TextField userNameTextField;
    @FXML private PasswordField passwordField;
    @FXML private Button loginButton;
    @FXML private Button registerButton;

    private final AuthService authService = new AuthService();

    @FXML
    private void initialize() {
        // La liaison est déjà faite via onAction dans le FXML,
        // mais on peut sécuriser ici si nécessaire.
    }

    @FXML
    private void handleLogin() {
        final String username = userNameTextField == null ? "" : userNameTextField.getText().trim();
        final char[] attempt = passwordField == null ? new char[0] : passwordField.getText().toCharArray();

        if (username.isEmpty() || attempt.length == 0) {
            showAlert(Alert.AlertType.WARNING, "Champs requis", "Veuillez saisir vos identifiants.");
            Arrays.fill(attempt, '\0');
            return;
        }

        loginButton.setDisable(true);
        loginButton.setText("Authentification...");

        Task<AuthService.LoginResult> task = new Task<>() {
            @Override
            protected AuthService.LoginResult call() throws Exception {
                try {
                    return authService.login(username, attempt);
                } finally {
                    Arrays.fill(attempt, '\0');
                }
            }
        };

        task.setOnSucceeded(e -> {
            AuthService.LoginResult result = task.getValue();
            SessionManager.startSession(result.userId(), result.username(), result.role());
            openDashboard(result.role(), result.username());
        });

        task.setOnFailed(e -> {
            loginButton.setDisable(false);
            loginButton.setText("LOGIN");
            handleAuthError(task.getException());
        });

        new Thread(task).start();
    }

    /**
     * Version robuste de l'ouverture du dashboard :
     * - vérifie l'existence du fichier FXML,
     * - affiche le stacktrace complet dans une alerte si le chargement échoue.
     */
    private void openDashboard(String role, String username) {
        Platform.runLater(() -> {
            String fxmlPath = "ADMIN".equalsIgnoreCase(role) ? "/fxml/admin_dashboard.fxml" : "/fxml/user_dashboard.fxml";
            try {
                // Vérification rapide : la ressource existe-t-elle ?
                java.net.URL fxmlUrl = getClass().getResource(fxmlPath);
                if (fxmlUrl == null) {
                    // Message clair si le fichier FXML est introuvable
                    showAlert(Alert.AlertType.ERROR, "Erreur", "Fichier FXML introuvable: " + fxmlPath);
                    System.err.println("FXML not found: " + fxmlPath + " (getResource returned null)");
                    return;
                }

                FXMLLoader loader = new FXMLLoader(fxmlUrl);
                Parent root = loader.load();

                // Transmission du pseudo au contrôleur via réflexion (silencieuse si la méthode n'existe pas)
                Object controller = loader.getController();
                if (controller != null) {
                    try {
                        Method m = controller.getClass().getMethod("setUsername", String.class);
                        m.invoke(controller, username);
                    } catch (NoSuchMethodException ignored) {
                        // ok — pas obligatoire que le contrôleur ait setUsername
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }

                Stage stage = new Stage();
                stage.setTitle("BLUELOCKER - Secure Session [" + username + "]");
                Scene scene = new Scene(root, 1280, 720);
                stage.setScene(scene);
                stage.setResizable(false);
                stage.setFullScreen(false);
                stage.centerOnScreen();

                // Fermer la fenêtre de login si possible
                try {
                    Stage loginStage = (Stage) loginButton.getScene().getWindow();
                    if (loginStage != null) loginStage.close();
                } catch (Exception ignore) {}

                stage.show();

            } catch (Throwable ex) {
                // Affiche le message et la pile complète (utile pour debugging)
                ex.printStackTrace();
                // Affiche l'alerte avec le détail (message + zone extensible pour stacktrace)
                Platform.runLater(() -> {
                    Alert a = new Alert(Alert.AlertType.ERROR);
                    a.setTitle("Erreur");
                    a.setHeaderText("Impossible de charger le tableau de bord.");
                    a.setContentText(ex.toString());

                    java.io.StringWriter sw = new java.io.StringWriter();
                    java.io.PrintWriter pw = new java.io.PrintWriter(sw);
                    ex.printStackTrace(pw);
                    String exceptionText = sw.toString();

                    TextArea ta = new TextArea(exceptionText);
                    ta.setEditable(false);
                    ta.setWrapText(true);
                    ta.setMaxWidth(Double.MAX_VALUE);
                    ta.setMaxHeight(Double.MAX_VALUE);
                    a.getDialogPane().setExpandableContent(ta);

                    Window owner = loginButton == null ? null : loginButton.getScene().getWindow();
                    if (owner != null) a.initOwner(owner);
                    a.showAndWait();
                });
            }
        });
    }

    @FXML
    private void handleShowRegister() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/fxml/register.fxml"));
            Stage stage = (Stage) loginButton.getScene().getWindow();
            stage.setScene(new Scene(root, 1280, 720));
            stage.centerOnScreen();
        } catch (IOException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Échec du chargement de l'inscription.");
        }
    }

    private void handleAuthError(Throwable ex) {
        String msg = (ex instanceof AuthException) ? "Identifiant ou mot de passe incorrect." : "Erreur de connexion au serveur.";
        showAlert(Alert.AlertType.ERROR, "Échec", msg);
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
            Window owner = loginButton != null ? loginButton.getScene().getWindow() : null;
            if (owner != null) alert.initOwner(owner);

            alert.showAndWait();
        });
    }
}