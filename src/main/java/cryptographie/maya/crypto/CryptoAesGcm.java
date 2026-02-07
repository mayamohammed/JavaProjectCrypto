package cryptographie.maya.crypto;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.SecureRandom;

public final class CryptoAesGcm {
    private static final SecureRandom RND = new SecureRandom();

    public static final int DEK_LEN = 32;
    public static final int IV_LEN = 12;
    private static final int TAG_LEN_BITS = 128;

    private CryptoAesGcm() {}

    public static byte[] newDek() {
        byte[] dek = new byte[DEK_LEN];
        RND.nextBytes(dek);
        return dek;
    }

    public static byte[] newIv() {
        byte[] iv = new byte[IV_LEN];
        RND.nextBytes(iv);
        return iv;
    }

    public static byte[] encrypt(byte[] plaintext, byte[] dek, byte[] iv, byte[] aad) throws Exception {
        Cipher c = Cipher.getInstance("AES/GCM/NoPadding");
        c.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(dek, "AES"), new GCMParameterSpec(TAG_LEN_BITS, iv));
        if (aad != null) c.updateAAD(aad);
        return c.doFinal(plaintext); // ciphertext + tag
    }

    public static byte[] decrypt(byte[] ciphertext, byte[] dek, byte[] iv, byte[] aad) throws Exception {
        Cipher c = Cipher.getInstance("AES/GCM/NoPadding");
        c.init(Cipher.DECRYPT_MODE, new SecretKeySpec(dek, "AES"), new GCMParameterSpec(TAG_LEN_BITS, iv));
        if (aad != null) c.updateAAD(aad);
        return c.doFinal(ciphertext);
    }
}