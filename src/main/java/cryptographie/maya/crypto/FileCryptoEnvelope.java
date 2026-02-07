package cryptographie.maya.crypto;

import java.nio.charset.StandardCharsets;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Arrays;
import java.util.Objects;

public final class FileCryptoEnvelope {

    public record EncryptedPayload(byte[] iv, byte[] encryptedData, byte[] encryptedDek, String dekAlg) {}

    private FileCryptoEnvelope() {}

    // AAD optionnel mais recommandé: lie le ciphertext au user + type
    public static byte[] buildAad(int userId, String itemType) {
        String s = "userId=" + userId + ";type=" + itemType;
        return s.getBytes(StandardCharsets.UTF_8);
    }

    public static EncryptedPayload encryptForUser(byte[] plaintext, PublicKey userPublicKey, int userId, String itemType) throws Exception {
        Objects.requireNonNull(plaintext);
        Objects.requireNonNull(userPublicKey);

        byte[] dek = CryptoAesGcm.newDek();
        try {
            byte[] iv = CryptoAesGcm.newIv();
            byte[] aad = buildAad(userId, itemType);

            byte[] encryptedData = CryptoAesGcm.encrypt(plaintext, dek, iv, aad);
            byte[] encryptedDek = CryptoRsaOaep.encrypt(dek, userPublicKey);

            return new EncryptedPayload(iv, encryptedData, encryptedDek, "RSA-OAEP");
        } finally {
            // Wipe the DEK from memory as soon as possible
            if (dek != null) Arrays.fill(dek, (byte) 0);
        }
    }

    public static byte[] decryptForUser(byte[] encryptedData, byte[] iv, byte[] encryptedDek,
                                        PrivateKey userPrivateKey, int userId, String itemType) throws Exception {
        Objects.requireNonNull(encryptedData);
        Objects.requireNonNull(iv);
        Objects.requireNonNull(encryptedDek);
        Objects.requireNonNull(userPrivateKey);

        byte[] dek = null;
        try {
            byte[] aad = buildAad(userId, itemType);
            dek = CryptoRsaOaep.decrypt(encryptedDek, userPrivateKey);
            return CryptoAesGcm.decrypt(encryptedData, dek, iv, aad);
        } finally {
            if (dek != null) Arrays.fill(dek, (byte) 0);
        }
    }
}