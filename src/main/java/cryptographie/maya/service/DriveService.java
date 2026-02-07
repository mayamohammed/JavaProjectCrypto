package cryptographie.maya.service;

import cryptographie.maya.crypto.CryptoService;
import cryptographie.maya.crypto.RsaKeyManager;
import cryptographie.maya.dao.SecureItemDAO;
import cryptographie.maya.dao.UserDAO;
import cryptographie.maya.dao.impl.SecureItemDAOImpl;
import cryptographie.maya.dao.impl.UserDAOImpl;
import cryptographie.maya.model.SecureItem;

import java.nio.file.Files;
import java.nio.file.Path;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.List;
import java.util.Optional;

public class DriveService {

    private final UserDAO userDAO;
    private final SecureItemDAO secureItemDAO;
    private final CryptoService cryptoService;

    public DriveService() {
        this(new UserDAOImpl(), new SecureItemDAOImpl(), new CryptoService());
    }

    public DriveService(UserDAO userDAO, SecureItemDAO secureItemDAO, CryptoService cryptoService) {
        this.userDAO = userDAO;
        this.secureItemDAO = secureItemDAO;
        this.cryptoService = cryptoService;
    }

    public int addFile(int userId, String username, String title, Path filePath) throws Exception {
        if (title == null || title.isBlank()) throw new IllegalArgumentException("title is required");
        if (filePath == null) throw new IllegalArgumentException("filePath is required");

        byte[] plain = Files.readAllBytes(filePath);
        long size = Files.size(filePath);

        String publicKeyB64 = userDAO.getPublicKeyById(userId);
        if (publicKeyB64 == null || publicKeyB64.isBlank()) {
            throw new IllegalStateException("Public key not found for userId=" + userId);
        }
        PublicKey userPk = RsaKeyManager.publicKeyFromBase64(publicKeyB64);

        var payload = cryptoService.encryptForUser(plain, userPk, userId, "file");

        SecureItem item = new SecureItem();
        item.setUserId(userId);
        item.setTitle(title);
        item.setItemType("file");
        item.setFileSize(size);
        item.setEncryptedData(payload.encryptedData());
        item.setIv(payload.iv());
        item.setEncryptedDek(payload.encryptedDek());
        item.setDekAlg(payload.dekAlg());

        return secureItemDAO.create(item);
    }

    public int addNote(int userId, String title, String noteText) throws Exception {
        if (title == null || title.isBlank()) throw new IllegalArgumentException("title is required");
        if (noteText == null) noteText = "";

        String publicKeyB64 = userDAO.getPublicKeyById(userId);
        if (publicKeyB64 == null || publicKeyB64.isBlank()) {
            throw new IllegalStateException("Public key not found for userId=" + userId);
        }
        var userPk = RsaKeyManager.publicKeyFromBase64(publicKeyB64);
        byte[] plain = noteText.getBytes(java.nio.charset.StandardCharsets.UTF_8);

        var payload = cryptoService.encryptForUser(plain, userPk, userId, "note");

        SecureItem item = new SecureItem();
        item.setUserId(userId);
        item.setTitle(title);
        item.setItemType("note");
        item.setFileSize(plain.length);
        item.setEncryptedData(payload.encryptedData());
        item.setIv(payload.iv());
        item.setEncryptedDek(payload.encryptedDek());
        item.setDekAlg(payload.dekAlg());

        return secureItemDAO.create(item);
    }

    public List<SecureItem> listItems(int userId) throws Exception {
        return secureItemDAO.listByUserId(userId);
    }

    public byte[] downloadItemBytes(int userId, String username, int itemId) throws Exception {
        Optional<SecureItem> opt = secureItemDAO.findByIdForUser(itemId, userId);
        if (opt.isEmpty()) {
            throw new IllegalArgumentException("Item not found or not owned by user (id=" + itemId + ")");
        }

        SecureItem item = opt.get();

        if (item.getEncryptedData() == null || item.getIv() == null || item.getEncryptedDek() == null) {
            throw new IllegalStateException("Encrypted fields missing for item id=" + itemId);
        }

        // Load user's private key from local file keys/<username>.pk8
        PrivateKey sk = RsaKeyManager.loadPrivateKey(username);

        return cryptoService.decryptForUser(
                item.getEncryptedData(),
                item.getIv(),
                item.getEncryptedDek(),
                sk,
                userId,
                item.getItemType()
        );
    }

    public boolean deleteItem(int userId, int itemId) throws Exception {
        return secureItemDAO.deleteForUser(itemId, userId);
    }
}