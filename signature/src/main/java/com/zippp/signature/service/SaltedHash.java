package com.zippp.signature.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

public class SaltedHash {

    private static final int SALT_LENGTH = 16; // 16 bytes = 128 bits

    public static byte[] generateSalt() {
        byte[] salt = new byte[SALT_LENGTH];
        new SecureRandom().nextBytes(salt);
        return salt;
    }

    public static String hashSha256WithSalt(String rawValue, byte[] salt) {
        if (rawValue == null) throw new IllegalArgumentException("rawValue cannot be null");
        if (salt == null) throw new IllegalArgumentException("salt cannot be null");

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            digest.update(salt);
            byte[] hashBytes = digest.digest(rawValue.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hashBytes);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }

    public static String concatenatedSaltAndHash(String rawValue) {
        byte[] salt = generateSalt();
        String hash = hashSha256WithSalt(rawValue, salt);
        return Base64.getEncoder().encodeToString(salt) + "$" + hash;
    }

    public static boolean matches(String rawValue, String concatenatedSaltAndHash) {
        if (rawValue == null || concatenatedSaltAndHash == null) return false;

        String[] parts = concatenatedSaltAndHash.split("\\$");
        if (parts.length != 2) return false;

        byte[] salt = Base64.getDecoder().decode(parts[0]);
        String expectedHash = parts[1];
        String computedHash = hashSha256WithSalt(rawValue, salt);
        return MessageDigest.isEqual(
                computedHash.getBytes(StandardCharsets.UTF_8),
                expectedHash.getBytes(StandardCharsets.UTF_8)
        );
    }
}