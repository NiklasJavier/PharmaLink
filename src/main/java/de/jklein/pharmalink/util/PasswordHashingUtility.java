package de.jklein.pharmalink.util;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component; // Allows Spring to manage this class as a bean

@Component
public class PasswordHashingUtility {

    private final PasswordEncoder passwordEncoder;

    public PasswordHashingUtility(PasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
    }

    public String hashPassword(String rawPassword) {
        return passwordEncoder.encode(rawPassword);
    }

    public static void main(String[] args) {
        if (args.length == 0) {
            System.out.println("Usage: java -cp <your-app-jar.jar> de.jklein.pharmalink.util.PasswordHashingUtility <password>");
            System.out.println("To hash a password.");
            return;
        }
        String rawPassword = args[0];
        PasswordEncoder encoder = new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder();
        String hashedPassword = encoder.encode(rawPassword);
        System.out.println("Plaintext password: " + rawPassword);
        System.out.println("Hashed password: " + hashedPassword);
    }
}