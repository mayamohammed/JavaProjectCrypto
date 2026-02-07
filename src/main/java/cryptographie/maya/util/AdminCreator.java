package cryptographie.maya.util;

import cryptographie.maya.dao.impl.UserDAOImpl;
import cryptographie.maya.model.User;
import cryptographie.maya.crypto.RsaKeyManager;
import de.mkammerer.argon2.Argon2;
import de.mkammerer.argon2.Argon2Factory;

import java.security.KeyPair;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;

/**
 * Utilitaire CLI pour créer un admin ou afficher l'INSERT SQL.
 *
 * Usage:
 *   java -cp target/classes:target/dependency/* cryptographie.maya.util.AdminCreator <username> <password> [--insert] [email]
 *
 * Si --insert est fourni, le programme tente d'insérer via UserDAOImpl (JDBC doit être configuré).
 */
public final class AdminCreator {

    private AdminCreator() {}

    public static void main(String[] args) {
        if (args.length < 2) {
            System.out.println("Usage: AdminCreator <username> <password> [--insert] [email]");
            System.exit(1);
        }

        String username = args[0].trim();
        String password = args[1];
        boolean doInsert = false;
        String email = null;

        for (int i = 2; i < args.length; i++) {
            if ("--insert".equalsIgnoreCase(args[i])) doInsert = true;
            else email = args[i];
        }

        char[] pwdChars = password.toCharArray();
        Argon2 argon2 = Argon2Factory.create(Argon2Factory.Argon2Types.ARGON2id);
        String hash = null;
        String saltB64 = null;

        try {
            // Générer le hash Argon2
            hash = argon2.hash(3, 65536, 1, pwdChars);
            System.out.println("Generated Argon2 hash:");
            System.out.println(hash);

            // Générer un salt (Base64) — ta table exige salt NOT NULL
            saltB64 = generateSaltBase64(16);
            System.out.println("Generated salt (Base64):");
            System.out.println(saltB64);

        } catch (Exception e) {
            System.err.println("Error generating Argon2 hash or salt: " + e.getMessage());
            e.printStackTrace();
            System.exit(2);
        } finally {
            // Wipe sensitive data
            Arrays.fill(pwdChars, '\0');
            try { argon2.wipeArray(pwdChars); } catch (Throwable ignored) {}

        }

        try {
            // Générer clé RSA et sauvegarder la clé privée localement
            KeyPair kp = RsaKeyManager.generateKeyPair();
            RsaKeyManager.savePrivateKey(username, kp.getPrivate());
            String publicKeyB64 = RsaKeyManager.publicKeyToBase64(kp.getPublic());

            System.out.println();
            System.out.println("Private key saved to ./keys/" + username + ".pk8");
            System.out.println("Public key (Base64, can be stored in DB public_key column):");
            System.out.println(publicKeyB64);
            System.out.println();

            // Préparer SQL insert (avec salt non-null)
            String sql = """
                    INSERT INTO users (username, password_hash, salt, first_name, last_name, email, role, public_key)
                    VALUES ('%s', '%s', '%s', 'Super', 'Admin', %s, 'ADMIN', '%s');
                    """.formatted(
                    escapeSql(username),
                    escapeSql(hash),
                    escapeSql(saltB64),
                    email == null ? "NULL" : "'" + escapeSql(email) + "'",
                    escapeSql(publicKeyB64)
            );

            System.out.println("SQL (copy/paste in your DB admin tool):");
            System.out.println(sql);

            if (doInsert) {
                // Insérer via UserDAOImpl
                UserDAOImpl userDao = new UserDAOImpl();
                User u = new User();
                u.setUsername(username);
                u.setPasswordHash(hash);
                u.setSalt(saltB64); // IMPORTANT: fournir salt pour respecter la contrainte NOT NULL
                u.setFirstName("Super");
                u.setLastName("Admin");
                u.setEmail(email);
                u.setRole("ADMIN");
                u.setPublicKey(publicKeyB64);

                int newId = userDao.create(u);
                System.out.println("Inserted user id = " + newId + " into DB.");
            } else {
                System.out.println("--insert not provided: not inserting into database.");
            }

        } catch (Exception e) {
            System.err.println("Error during key generation / DB insert: " + e.getMessage());
            e.printStackTrace();
            System.exit(3);
        }
    }

    private static String generateSaltBase64(int bytes) {
        byte[] salt = new byte[bytes];
        new SecureRandom().nextBytes(salt);
        return Base64.getEncoder().encodeToString(salt);
    }

    // Simple quote-escape for printing SQL only
    private static String escapeSql(String s) {
        if (s == null) return null;
        return s.replace("'", "''");
    }
}