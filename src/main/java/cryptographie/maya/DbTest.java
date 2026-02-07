package cryptographie.maya;

import cryptographie.maya.service.AdminService;

public class DbTest {
    public static void main(String[] args) {
        try {
            System.out.println("Initialisation AdminService...");
            AdminService s = new AdminService();

            System.out.println("Listing users...");
            var users = s.listAllUsers();
            System.out.println("users count = " + (users == null ? "null" : users.size()));
            if (users != null && !users.isEmpty()) {
                System.out.println("First user: " + users.get(0).getUsername() + " (id=" + users.get(0).getId() + ")");
            }

            System.out.println("Listing items for user id=9...");
            var items = s.listItemsForUser(9);
            System.out.println("items count = " + (items == null ? "null" : items.size()));

            System.out.println("DbTest finished OK.");
        } catch (Exception e) {
            System.err.println("Exception in DbTest:");
            e.printStackTrace();
            // exit non-zero for CI clarity
            System.exit(2);
        }
    }
}