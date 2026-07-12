package com.zuzdog.security;

import org.springframework.stereotype.Component;
import org.springframework.boot.autoconfigure.security.SecurityProperties;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;


import java.security.SecureRandom;
import java.util.HexFormat;


@Component
public class PasswordHasher {
    private static final int SALT_BYTES = 32; // 32 bytes = 256 bits
    private static final int BCRYPT_STRENGTH = 12;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder(BCRYPT_STRENGTH);
    private final SecureRandom secureRandom = new SecureRandom();
    private final SecurityProperties securityProperties;

    // Constructor to inject securityProperties
    public PasswordHasher(SecurityProperties securityProperties) {
        this.securityProperties = securityProperties;
    }

    // Hashes the password with a generated salt
    // The salt is generated using a secure random number generator and is unique for each password.
    public String generateSalt() {
        byte[] saltBytes = new byte[SALT_BYTES];
        secureRandom.nextBytes(saltBytes);
        return HexFormat.of().formatHex(saltBytes);
    }

    // Combines the plain password with the salt and pepper
    public String hashPassword(String plainPassword, String salt) {
        String combined = combine(plainPassword, salt);
        return passwordEncoder.encode(combined);
        }

    public boolean verifyPassword(String plainPassword, String salt, String storedHash){
        if (plainPassword == null || salt == null || storedHash == null) {
            return false;
        }
        String combined = combine(plainPassword, salt);
        return passwordEncoder.matches(combined, storedHash);
    }

    private String combine(String plainPassword, String salt){
        return plainPassword + salt + securityProperties.getPepper();

    }



}
