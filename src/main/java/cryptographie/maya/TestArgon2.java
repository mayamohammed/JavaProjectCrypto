package cryptographie.maya;

import de.mkammerer.argon2.Argon2;
import de.mkammerer.argon2.Argon2Factory;

import java.util.Arrays;


public class TestArgon2 {

    public static void main(String[] args) {

        String password = "MotDePasseDeTest!";
        int iterations = 3;
        int memoryKiB = 65536; // 64 MiB
        int parallelism = 1;

        System.out.println("password  " + password);


        char[] pwChars = password.toCharArray();
        Argon2 argon2 = Argon2Factory.create( Argon2Factory.Argon2Types.ARGON2id); // ARGON2id by default


        String hash = argon2.hash(iterations, memoryKiB, parallelism, pwChars);
        System.out.println("password hacher   " + hash);
    }
}
