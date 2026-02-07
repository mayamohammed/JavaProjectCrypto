package cryptographie.maya.dao;

import cryptographie.maya.model.User;

import java.util.List;
import java.util.Optional;

public interface UserDAO {
    int create(User user) throws Exception;

    Optional<User> findByUsername(String username) throws Exception;

    Optional<User> findById(int id) throws Exception;

    String getPublicKeyById(int userId) throws Exception;

    // Nouveaux : méthodes admin / listing
    List<User> listAll() throws Exception;

    boolean updateRole(int userId, String newRole) throws Exception;

    boolean deleteById(int userId) throws Exception;
}