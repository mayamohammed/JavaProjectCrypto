package cryptographie.maya.util;

// DbTest.java
public class DbTest {
    public static void main(String[] args) {
        try {
            cryptographie.maya.service.AdminService s = new cryptographie.maya.service.AdminService();
            System.out.println("Listing users...");
            var users = s.listAllUsers();
            System.out.println("users count = " + (users == null ? "null" : users.size()));
            System.out.println("Listing items for user 9...");
            var items = s.listItemsForUser(9);
            System.out.println("items count = " + (items == null ? "null" : items.size()));
        } catch (Exception e) {
            System.err.println("Exception in DbTest:");
            e.printStackTrace();
        }
    }
}