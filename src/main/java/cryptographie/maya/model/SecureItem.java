package cryptographie.maya.model;

import java.time.LocalDateTime;

public class SecureItem {
    private int id;
    private int userId;
    private String title;
    private String itemType;
    private long fileSize;

    // Crypto fields
    private byte[] encryptedData; // LONGBLOB (ciphertext)
    private byte[] iv;            // VARBINARY(12) for AES-GCM
    private byte[] encryptedDek;  // VARBINARY(512) (RSA-OAEP wrapped DEK)
    private String dekAlg;        // e.g. "RSA-OAEP"

    private LocalDateTime createdAt;

    public SecureItem() {}

    // --- getters/setters ---

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getItemType() {
        return itemType;
    }

    public void setItemType(String itemType) {
        this.itemType = itemType;
    }

    public long getFileSize() {
        return fileSize;
    }

    public void setFileSize(long fileSize) {
        this.fileSize = fileSize;
    }

    public byte[] getEncryptedData() {
        return encryptedData;
    }

    public void setEncryptedData(byte[] encryptedData) {
        this.encryptedData = encryptedData;
    }

    public byte[] getIv() {
        return iv;
    }

    public void setIv(byte[] iv) {
        this.iv = iv;
    }

    public byte[] getEncryptedDek() {
        return encryptedDek;
    }

    public void setEncryptedDek(byte[] encryptedDek) {
        this.encryptedDek = encryptedDek;
    }

    public String getDekAlg() {
        return dekAlg;
    }

    public void setDekAlg(String dekAlg) {
        this.dekAlg = dekAlg;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}