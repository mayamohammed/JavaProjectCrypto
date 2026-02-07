package cryptographie.maya.dao;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.util.Properties;

/**
 * DatabaseManager simple avec shutdown qui appelle la méthode de nettoyage
 * du driver MySQL pour stopper AbandonedConnectionCleanupThread.
 */
public class DatabaseManager {

    private static HikariDataSource ds;

    static {
        try {
            Properties props = new Properties();
            try (InputStream in = DatabaseManager.class.getClassLoader().getResourceAsStream("db.properties")) {
                if (in == null) {
                    throw new IllegalStateException("Erreur initialisation DatabaseManager: db.properties introuvable dans src/main/resources");
                }
                props.load(in);
            }

            HikariConfig cfg = new HikariConfig();
            String host = props.getProperty("db.host", "localhost");
            String port = props.getProperty("db.port", "3306");
            String db = props.getProperty("db.name", "bluelocker_db");
            String user = props.getProperty("db.user", "root");
            String pass = props.getProperty("db.password", "");
            String url = String.format("jdbc:mysql://%s:%s/%s?useSSL=%s&serverTimezone=%s&allowPublicKeyRetrieval=true",
                    host, port, db,
                    props.getProperty("db.useSSL", "false"),
                    props.getProperty("db.serverTimezone", "UTC"));

            cfg.setJdbcUrl(url);
            cfg.setUsername(user);
            cfg.setPassword(pass);
            cfg.setMaximumPoolSize(Integer.parseInt(props.getProperty("db.maximumPoolSize", "10")));
            cfg.setMinimumIdle(Integer.parseInt(props.getProperty("db.minimumIdle", "2")));
            cfg.setConnectionTimeout(Long.parseLong(props.getProperty("db.connectionTimeout", "30000")));
            cfg.setIdleTimeout(Long.parseLong(props.getProperty("db.idleTimeout", "600000")));

            ds = new HikariDataSource(cfg);
        } catch (IOException e) {
            throw new ExceptionInInitializerError(e);
        } catch (RuntimeException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    public static Connection getConnection() throws Exception {
        return ds.getConnection();
    }

    /**
     * Shutdown the datasource and attempt to stop the MySQL cleanup thread if present.
     * Safe to call multiple times.
     */
    public static void shutdown() {
        if (ds != null) {
            try {
                ds.close();
            } catch (Exception ignored) {}
            ds = null;
        }

        // Arrêter le thread de nettoyage abandonné du driver MySQL (prévenir le warning)
        try {
            Class<?> cleanupClass = Class.forName("com.mysql.cj.jdbc.AbandonedConnectionCleanupThread");
            cleanupClass.getMethod("checkedShutdown").invoke(null);
        } catch (ClassNotFoundException e) {
            // Classe absente : driver plus ancien / différent -> rien à faire
        } catch (Throwable t) {
            // ignorer les erreurs réflexives
        }
    }
}