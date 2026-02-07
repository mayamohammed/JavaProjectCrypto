package cryptographie.maya.dao;

import cryptographie.maya.model.SecureItem;

import java.util.List;
import java.util.Optional;

public interface SecureItemDAO {
    int create(SecureItem item) throws Exception;

    List<SecureItem> listByUserId(int userId) throws Exception;

    Optional<SecureItem> findByIdForUser(int itemId, int userId) throws Exception;

    boolean deleteForUser(int itemId, int userId) throws Exception;
}