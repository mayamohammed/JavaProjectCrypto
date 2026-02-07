package cryptographie.maya.crypto;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.security.*;
import java.security.spec.*;
import java.util.Base64;
import java.util.Set;

public final class RsaKeyManager {
    private static final String ALG = "RSA";
    private static final int BITS = 2048;

    private RsaKeyManager() {}

    public static KeyPair generateKeyPair() throws Exception {
        KeyPairGenerator kpg = KeyPairGenerator.getInstance(ALG);
        kpg.initialize(BITS);
        return kpg.generateKeyPair();
    }

    public static String publicKeyToBase64(PublicKey pk) {
        return Base64.getEncoder().encodeToString(pk.getEncoded()); // X.509
    }

    public static PublicKey publicKeyFromBase64(String b64) throws Exception {
        byte[] bytes = Base64.getDecoder().decode(b64);
        return KeyFactory.getInstance(ALG).generatePublic(new X509EncodedKeySpec(bytes));
    }

    public static void savePrivateKey(String username, PrivateKey privateKey) throws IOException {
        Path dir = Paths.get("keys");
        Files.createDirectories(dir);
        Path file = dir.resolve(username + ".pk8");
        // Set file permissions after creation if possible (POSIX)
        Files.write(file, privateKey.getEncoded(), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        try {
            // Try to set strict permissions on POSIX sistemas
            Set<PosixFilePermission> perms = PosixFilePermissions.fromString("rw-------");
            Files.setPosixFilePermissions(file, perms);
        } catch (UnsupportedOperationException ignored) {
            // Windows or FS don't support Posix permissions — make a note in logs
        }
    }

    public static PrivateKey loadPrivateKey(String username) throws Exception {
        Path file = Paths.get("keys").resolve(username + ".pk8");
        if (!Files.exists(file)) {
            throw new IllegalStateException("Private key file not found for user '" + username + "' at: " + file.toAbsolutePath());
        }
        byte[] bytes = Files.readAllBytes(file);
        return KeyFactory.getInstance(ALG).generatePrivate(new PKCS8EncodedKeySpec(bytes));
    }
}