package cryptographie.maya.dao.impl;

import cryptographie.maya.dao.DatabaseManager;
import cryptographie.maya.dao.SecureItemDAO;
import cryptographie.maya.model.SecureItem;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class SecureItemDAOImpl implements SecureItemDAO {

    @Override
    public int create(SecureItem item) throws Exception {
        String sql = """
            INSERT INTO secure_items
            (user_id, title, item_type, file_size, encrypted_data, iv, encrypted_dek, dek_alg, created_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, NOW())
            """;

        try (Connection con = DatabaseManager.getConnection();
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setInt(1, item.getUserId());
            ps.setString(2, item.getTitle());
            ps.setString(3, item.getItemType());
            ps.setLong(4, item.getFileSize());
            ps.setBytes(5, item.getEncryptedData());
            ps.setBytes(6, item.getIv()); // 12 bytes
            ps.setBytes(7, item.getEncryptedDek());
            ps.setString(8, item.getDekAlg()); // "RSA-OAEP"

            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) return rs.getInt(1);
            }
            throw new SQLException("SecureItem insertion failed: no generated key returned.");
        }
    }

    @Override
    public List<SecureItem> listByUserId(int userId) throws Exception {
        String sql = """
            SELECT id, user_id, title, item_type, file_size, created_at
            FROM secure_items
            WHERE user_id = ?
            ORDER BY created_at DESC
            """;

        List<SecureItem> out = new ArrayList<>();

        try (Connection con = DatabaseManager.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, userId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    SecureItem i = new SecureItem();
                    i.setId(rs.getInt("id"));
                    i.setUserId(rs.getInt("user_id"));
                    i.setTitle(rs.getString("title"));
                    i.setItemType(rs.getString("item_type"));
                    i.setFileSize(rs.getLong("file_size"));

                    Timestamp ts = rs.getTimestamp("created_at");
                    if (ts != null) {
                        i.setCreatedAt(ts.toLocalDateTime());
                    }

                    out.add(i);
                }
            }
        }

        return out;
    }

    @Override
    public Optional<SecureItem> findByIdForUser(int itemId, int userId) throws Exception {
        String sql = """
            SELECT id, user_id, title, item_type, file_size,
                   encrypted_data, iv, encrypted_dek, dek_alg, created_at
            FROM secure_items
            WHERE id = ? AND user_id = ?
            """;

        try (Connection con = DatabaseManager.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, itemId);
            ps.setInt(2, userId);

            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return Optional.empty();

                SecureItem i = new SecureItem();
                i.setId(rs.getInt("id"));
                i.setUserId(rs.getInt("user_id"));
                i.setTitle(rs.getString("title"));
                i.setItemType(rs.getString("item_type"));
                i.setFileSize(rs.getLong("file_size"));

                i.setEncryptedData(rs.getBytes("encrypted_data"));
                i.setIv(rs.getBytes("iv"));
                i.setEncryptedDek(rs.getBytes("encrypted_dek"));
                i.setDekAlg(rs.getString("dek_alg"));

                Timestamp ts = rs.getTimestamp("created_at");
                if (ts != null) {
                    i.setCreatedAt(ts.toLocalDateTime());
                }

                return Optional.of(i);
            }
        }
    }

    @Override
    public boolean deleteForUser(int itemId, int userId) throws Exception {
        String sql = "DELETE FROM secure_items WHERE id = ? AND user_id = ?";

        try (Connection con = DatabaseManager.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, itemId);
            ps.setInt(2, userId);

            return ps.executeUpdate() > 0;
        }
    }
}