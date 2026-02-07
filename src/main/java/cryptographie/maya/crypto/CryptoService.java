package cryptographie.maya.crypto;

import java.security.PrivateKey;
import java.security.PublicKey;

public class CryptoService {

    public FileCryptoEnvelope.EncryptedPayload encryptForUser(byte[] plain, PublicKey userPublicKey, int userId, String itemType) throws Exception {
        return FileCryptoEnvelope.encryptForUser(plain, userPublicKey, userId, itemType );
    }

    public byte[] decryptForUser(byte[] encryptedData, byte[] iv, byte[] encryptedDek,
                                 PrivateKey userPrivateKey, int userId, String itemType) throws Exception {
        return FileCryptoEnvelope.decryptForUser(encryptedData, iv, encryptedDek, userPrivateKey, userId, itemType);
    }
}