package cryptographie.maya.service;

import cryptographie.maya.dao.UserDAO;
import cryptographie.maya.dao.impl.UserDAOImpl;
import cryptographie.maya.model.User;
import de.mkammerer.argon2.Argon2;
import de.mkammerer.argon2.Argon2Factory;
import org.mindrot.jbcrypt.BCrypt;

import java.sql.Connection;
import java.util.Optional;

/**
 * Service d'authentification (login/register) : aucune logique UI ici.
 */
public class AuthService {

    private final UserDAO userDAO;

    public AuthService() {
        this(new UserDAOImpl());
    }

    public AuthService(UserDAO userDAO) {
        this.userDAO = userDAO;
    }

    public LoginResult login(String username, char[] passwordAttempt) throws Exception {
        if (username == null || username.isBlank() || passwordAttempt == null || passwordAttempt.length == 0) {
            throw new AuthException("EMPTY_CREDENTIALS");
        }

        Optional<User> userOpt = userDAO.findByUsername(username);
        if (userOpt.isEmpty()) throw new AuthException("USER_NOT_FOUND");

        User user = userOpt.get();
        String storedHash = user.getPasswordHash();
        String role = (user.getRole() == null || user.getRole().isBlank()) ? "USER" : user.getRole();

        Argon2 argon2 = Argon2Factory.create(Argon2Factory.Argon2Types.ARGON2id);
        try {
            boolean verified;
            boolean needsMigration = false;

            if (storedHash != null && storedHash.startsWith("$argon2")) {
                verified = argon2.verify(storedHash, passwordAttempt);
            } else if (storedHash != null && storedHash.startsWith("$2")) {
                verified = BCrypt.checkpw(new String(passwordAttempt), storedHash);
                needsMigration = true;
            } else {
                throw new AuthException("INVALID_HASH_FORMAT");
            }

            if (!verified) throw new AuthException("AUTH_FAILED");

            // Optionnel: migration BCrypt -> Argon2 (si tu veux la garder)
            if (needsMigration) {
                // NOTE: ici on doit faire un UPDATE users.password_hash.
                // Comme ton UserDAO n'a pas updatePasswordHash(), on te le rajoutera après.
                // Pour l’instant: on laisse désactivé ou on fera un patch ensuite.
            }

            return new LoginResult(user.getId(), username, role);

        } finally {
            // important: argon2-jvm 2.11 ne supporte pas try-with-resources chez toi

        }
    }

    public record LoginResult(int userId, String username, String role) {}

    public static class AuthException extends Exception {
        public AuthException(String message) { super(message); }
    }
}