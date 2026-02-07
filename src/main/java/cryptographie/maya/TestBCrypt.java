package cryptographie.maya;

import org.mindrot.jbcrypt.BCrypt;

public class TestBCrypt {
    public static void main(String[] args) {
        String pw = "MonSuperMotDePasse!";
        String hash = BCrypt.hashpw(pw, BCrypt.gensalt(12));
        System.out.println("Hash: " + hash);
        System.out.println("Check ObK: " + BCrypt.checkpw(pw, hash));
    }
}
