package cryptographie.maya.crypto;

import javax.crypto.Cipher;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Objects;

public final class CryptoRsaOaep {
    private CryptoRsaOaep() {}

    public static byte[] encrypt(byte[] data, PublicKey publicKey) throws Exception {
        Objects.requireNonNull(data);
        Objects.requireNonNull(publicKey);
        Cipher c = Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding");
        c.init(Cipher.ENCRYPT_MODE, publicKey);
        return c.doFinal(data);
    }

    public static byte[] decrypt(byte[] data, PrivateKey privateKey) throws Exception {
        Objects.requireNonNull(data);
        Objects.requireNonNull(privateKey);
        Cipher c = Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding");
        c.init(Cipher.DECRYPT_MODE, privateKey);
        return c.doFinal(data);
    }
}