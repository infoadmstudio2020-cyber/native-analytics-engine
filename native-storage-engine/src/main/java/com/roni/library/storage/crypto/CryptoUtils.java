package com.roni.library.storage.crypto;

import com.roni.library.storage.StorageException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

public final class CryptoUtils {
    private static final String ALGORITHM = "AES";
    private static final String CIPHER_TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int GCM_TAG_LENGTH = 128; // bits
    private static final int IV_LENGTH = 12; // bytes
    private static final int SALT_LENGTH = 16; // bytes
    private static final int ITERATION_COUNT = 65536;
    private static final int KEY_LENGTH = 256; // bits

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private CryptoUtils() {}

    public static byte[] generateSalt(int length) {
        if (length <= 0) length = SALT_LENGTH;
        byte[] salt = new byte[length];
        SECURE_RANDOM.nextBytes(salt);
        return salt;
    }

    public static SecretKey deriveKey(char[] passphrase, byte[] salt) {
        if (passphrase == null || passphrase.length == 0) {
            throw new StorageException("CRYPTO_INVALID_KEY", "Passphrase cannot be null or empty");
        }
        if (salt == null || salt.length == 0) {
            throw new StorageException("CRYPTO_INVALID_SALT", "Salt cannot be null or empty");
        }
        try {
            PBEKeySpec spec = new PBEKeySpec(passphrase, salt, ITERATION_COUNT, KEY_LENGTH);
            SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            byte[] keyBytes = factory.generateSecret(spec).getEncoded();
            return new SecretKeySpec(keyBytes, ALGORITHM);
        } catch (Exception e) {
            throw new StorageException("KEY_DERIVATION_FAILED", "Failed to derive AES key: " + e.getMessage(), e);
        }
    }

    public static String encrypt(String plaintext, String password) {
        if (plaintext == null) {
            return null;
        }
        if (password == null || password.isEmpty()) {
            throw new StorageException("INVALID_PASSWORD", "Password cannot be empty");
        }
        try {
            byte[] salt = generateSalt(SALT_LENGTH);
            SecretKey secretKey = deriveKey(password.toCharArray(), salt);

            byte[] iv = new byte[IV_LENGTH];
            SECURE_RANDOM.nextBytes(iv);

            Cipher cipher = Cipher.getInstance(CIPHER_TRANSFORMATION);
            GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, parameterSpec);

            byte[] cipherText = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));

            ByteBuffer byteBuffer = ByteBuffer.allocate(1 + salt.length + 1 + iv.length + cipherText.length);
            byteBuffer.put((byte) salt.length);
            byteBuffer.put(salt);
            byteBuffer.put((byte) iv.length);
            byteBuffer.put(iv);
            byteBuffer.put(cipherText);

            return Base64.getEncoder().encodeToString(byteBuffer.array());
        } catch (StorageException se) {
            throw se;
        } catch (Exception e) {
            throw new StorageException("ENCRYPTION_FAILED", "Failed to encrypt payload: " + e.getMessage(), e);
        }
    }

    public static String decrypt(String base64Payload, String password) {
        if (base64Payload == null) {
            return null;
        }
        if (password == null || password.isEmpty()) {
            throw new StorageException("INVALID_PASSWORD", "Password cannot be empty");
        }
        try {
            byte[] decoded = Base64.getDecoder().decode(base64Payload);
            ByteBuffer byteBuffer = ByteBuffer.wrap(decoded);

            int saltLength = byteBuffer.get() & 0xFF;
            byte[] salt = new byte[saltLength];
            byteBuffer.get(salt);

            int ivLength = byteBuffer.get() & 0xFF;
            byte[] iv = new byte[ivLength];
            byteBuffer.get(iv);

            byte[] cipherText = new byte[byteBuffer.remaining()];
            byteBuffer.get(cipherText);

            SecretKey secretKey = deriveKey(password.toCharArray(), salt);

            Cipher cipher = Cipher.getInstance(CIPHER_TRANSFORMATION);
            GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, parameterSpec);

            byte[] plainTextBytes = cipher.doFinal(cipherText);
            return new String(plainTextBytes, StandardCharsets.UTF_8);
        } catch (StorageException se) {
            throw se;
        } catch (Exception e) {
            throw new StorageException("DECRYPTION_FAILED", "Failed to decrypt payload: " + e.getMessage(), e);
        }
    }
}
