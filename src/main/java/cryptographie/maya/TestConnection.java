package cryptographie.maya;

import cryptographie.maya.dao.DatabaseManager;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

public class TestConnection {
    public static void main(String[] args) {
        System.out.println("Test connexion MySQL...");
        try (Connection conn = DatabaseManager.getConnection()) {
            System.out.println("Connecté à : " + conn.getMetaData().getURL());
            try (Statement st = conn.createStatement()) {
                // Test basique : SELECT 1
                try (ResultSet rs = st.executeQuery("SELECT 1")) {
                    if (rs.next()) {
                        System.out.println("SELECT 1 -> OK");
                    }
                }
                // Test sur une table (si schema importé)
                try (ResultSet rs2 = st.executeQuery("SELECT COUNT(*) AS c FROM users")) {
                    if (rs2.next()) {
                        System.out.println("Nombre d'utilisateurs dans users : " + rs2.getInt("c"));
                    }
                } catch (Exception e) {
                    System.out.println("Table 'users' absente ou erreur : " + e.getMessage());
                }
            }
        } catch (Exception e) {
            System.err.println("Erreur de connexion : " + e.getMessage());
            e.printStackTrace();
        } finally {
            DatabaseManager.shutdown();
        }
    }
}