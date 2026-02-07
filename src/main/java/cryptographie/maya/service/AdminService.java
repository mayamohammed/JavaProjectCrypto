package cryptographie.maya.service;

import cryptographie.maya.dao.SecureItemDAO;
import cryptographie.maya.dao.UserDAO;
import cryptographie.maya.dao.impl.SecureItemDAOImpl;
import cryptographie.maya.dao.impl.UserDAOImpl;
import cryptographie.maya.model.SecureItem;
import cryptographie.maya.model.User;

import java.util.*;

/**
 * Service pour fonctionnalités admin (list users, promote/demote, delete).
 * Utilise les DAO existants.
 *
 * NOTE:
 * - Les nouvelles méthodes getRoleCounts() et getTopItemsPerUser(int) effectuent un calcul en mémoire
 *   en s'appuyant sur les méthodes DAO existantes. Pour de grandes bases, il est préférable d'ajouter
 *   des méthodes DAO spécifiques qui effectuent ces agrégations côté base de données (GROUP BY),
 *   cela évitera le N+1 et améliorera fortement les performances.
 */
public class AdminService {

    private final UserDAO userDao = new UserDAOImpl();
    private final SecureItemDAO secureItemDao = new SecureItemDAOImpl();

    public List<User> listAllUsers() throws Exception {
        return userDao.listAll();
    }

    public List<SecureItem> listItemsForUser(int userId) throws Exception {
        return secureItemDao.listByUserId(userId);
    }

    public boolean updateUserRole(int userId, String newRole) throws Exception {
        return userDao.updateRole(userId, newRole);
    }

    public boolean deleteUser(int userId) throws Exception {
        // Selon ton schéma, supprimer l'utilisateur devrait cascade sur secure_items si FK ON DELETE CASCADE,
        // sinon assure-toi de supprimer d'abord ses items via secureItemDao.deleteByUserId(userId) si nécessaire.
        return userDao.deleteById(userId);
    }

    /**
     * Calcule la répartition des rôles (role -> count) en mémoire.
     * Utilise userDao.listAll(). Pour grande table users, préférez implémenter une requête SQL GROUP BY côté DAO.
     */
    public Map<String, Integer> getRoleCounts() throws Exception {
        List<User> list = userDao.listAll();
        Map<String, Integer> map = new LinkedHashMap<>();
        if (list == null) return map;
        for (User u : list) {
            String role = u.getRole() == null ? "UNKNOWN" : u.getRole();
            map.put(role, map.getOrDefault(role, 0) + 1);
        }
        return map;
    }

    /**
     * Retourne une liste (username -> count) triée par count décroissant, limitée à 'limit'.
     * Implémentation actuelle effectue un appel listByUserId pour chaque utilisateur (N+1).
     * Pour de meilleures performances, ajoutez une méthode DAO qui exécute :
     *   SELECT u.username, COUNT(s.id) AS cnt FROM users u LEFT JOIN secure_items s ON u.id = s.user_id
     *   GROUP BY u.id, u.username ORDER BY cnt DESC LIMIT ?
     */
    public List<Map.Entry<String, Integer>> getTopItemsPerUser(int limit) throws Exception {
        List<User> users = userDao.listAll();
        if (users == null || users.isEmpty()) return Collections.emptyList();

        List<Map.Entry<String, Integer>> list = new ArrayList<>(users.size());
        for (User u : users) {
            int count = 0;
            try {
                List<SecureItem> items = secureItemDao.listByUserId(u.getId());
                count = items == null ? 0 : items.size();
            } catch (Exception ex) {
                // En cas d'erreur sur un utilisateur, on continue (mais on peut logger).
                ex.printStackTrace();
            }
            String name = u.getUsername() == null ? ("user#" + u.getId()) : u.getUsername();
            list.add(new AbstractMap.SimpleEntry<>(name, count));
        }

        // Trier par count décroissant
        list.sort((a, b) -> Integer.compare(b.getValue(), a.getValue()));

        if (limit > 0 && list.size() > limit) {
            return new ArrayList<>(list.subList(0, limit));
        }
        return list;
    }
}