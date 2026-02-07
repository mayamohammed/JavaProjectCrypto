package cryptographie.maya;

import cryptographie.maya.dao.DatabaseManager;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class MainApp extends Application {

    @Override
    public void start(Stage stage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/login.fxml"));

        Scene scene = new Scene(loader.load(), 1280, 720);

        // optionnel: charger CSS global
        var css = getClass().getResource("/css/style.css");
        if (css != null) {
            scene.getStylesheets().add(css.toExternalForm());
        }

        stage.setTitle("Secure Drive");
        stage.setResizable(false);
        stage.setScene(scene);
        stage.show();
    }

    @Override
    public void stop() {
        // ✅ ferme le pool Hikari + cleanup MySQL thread
        DatabaseManager.shutdown();
    }

    public static void main(String[] args) {
        launch(args);
    }
}