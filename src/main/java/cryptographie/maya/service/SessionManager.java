package cryptographie.maya.service;

public final class SessionManager {

    private static Integer userId;
    private static String username;
    private static String role;

    private SessionManager() {}

    public static void startSession(int userId, String username, String role) {
        SessionManager.userId = userId;
        SessionManager.username = username;
        SessionManager.role = role;
    }

    public static boolean isLoggedIn() {
        return userId != null && username != null && !username.isBlank();
    }

    /** Nullable: utiliser seulement si tu gères le null toi-même. */
    public static Integer getUserId() {
        return userId;
    }

    /** Non-null: throw si pas connecté (évite NPE par auto-unboxing). */
    public static int requireUserId() {
        if (!isLoggedIn()) throw new IllegalStateException("User not logged in");
        return userId;
    }

    public static String getUsername() {
        return username;
    }

    public static String requireUsername() {
        if (!isLoggedIn()) throw new IllegalStateException("User not logged in");
        return username;
    }

    public static String getRole() {
        return role;
    }

    public static boolean isAdmin() {
        return role != null && role.equalsIgnoreCase("ADMIN");
    }

    public static void clear() {
        userId = null;
        username = null;
        role = null;
    }
}