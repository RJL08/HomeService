package com.example.homeservice.utils;


import android.util.Base64;

import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.util.Arrays;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;

public class KeystoreManager {

    private static final String ANDROID_KEY_STORE = "AndroidKeyStore";
    private static final String ALIAS = "MySecretKey";
    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int IV_SIZE = 12; // Tamaño IV recomendado para GCM (12 bytes)
    private static final int TAG_LENGTH = 128; // Longitud de la etiqueta en bits

    private KeyStore keyStore;

    /**
     * Constructor de la clase KeystoreManager.
     * @throws Exception Si ocurre algún error al inicializar el KeyStore.
     */
    public KeystoreManager() throws Exception {
        // Inicializa el KeyStore
        keyStore = KeyStore.getInstance(ANDROID_KEY_STORE);
        keyStore.load(null);
        // Si no existe la clave con el alias, se genera
        if (!keyStore.containsAlias(ALIAS)) {
            generateSecretKey();
        }
    }

    /**
     * Genera una clave secreta utilizando KeyGenerator.
     * @throws Exception Si ocurre algún error durante la generación de la clave.
     */
    private void generateSecretKey() throws Exception {
        KeyGenerator keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEY_STORE);
        KeyGenParameterSpec keyGenParameterSpec = new KeyGenParameterSpec.Builder(
                ALIAS,
                KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setRandomizedEncryptionRequired(true)
                .build();
        keyGenerator.init(keyGenParameterSpec);
        keyGenerator.generateKey();
    }

    private SecretKey getSecretKey() throws Exception {
        return (SecretKey) keyStore.getKey(ALIAS, null);
    }

    /**
     * Cifra el texto plano utilizando AES/GCM y lo devuelve codificado en Base64.
     *
     * @param plainText El texto a cifrar.
     * @return El texto cifrado en Base64 (incluye el IV en los primeros 12 bytes).
     * @throws Exception Si ocurre algún error durante el cifrado.
     */
    public String encryptData(String plainText) throws Exception {
        Cipher cipher = Cipher.getInstance(TRANSFORMATION);
        cipher.init(Cipher.ENCRYPT_MODE, getSecretKey());
        // Obtén el IV generado automáticamente
        byte[] iv = cipher.getIV();
        byte[] encryptedBytes = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
        // Combina el IV y los datos cifrados (IV || ciphertext)
        byte[] combined = new byte[iv.length + encryptedBytes.length];
        System.arraycopy(iv, 0, combined, 0, iv.length);
        System.arraycopy(encryptedBytes, 0, combined, iv.length, encryptedBytes.length);
        // Devuelve la cadena codificada en Base64
        return Base64.encodeToString(combined, Base64.DEFAULT);
    }

    /**
     * Descifra el texto cifrado (codificado en Base64) utilizando AES/GCM.
     *
     * @param cipherText El texto cifrado en Base64 (debe incluir el IV).
     * @return El texto plano descifrado.
     * @throws Exception Si ocurre algún error durante el descifrado.
     */
    public String decryptData(String cipherText) throws Exception {
        byte[] combined = Base64.decode(cipherText, Base64.DEFAULT);
        // Extrae el IV y el ciphertext
        byte[] iv = Arrays.copyOfRange(combined, 0, IV_SIZE);
        byte[] encryptedBytes = Arrays.copyOfRange(combined, IV_SIZE, combined.length);
        Cipher cipher = Cipher.getInstance(TRANSFORMATION);
        GCMParameterSpec spec = new GCMParameterSpec(TAG_LENGTH, iv);
        cipher.init(Cipher.DECRYPT_MODE, getSecretKey(), spec);
        byte[] decryptedBytes = cipher.doFinal(encryptedBytes);
        return new String(decryptedBytes, StandardCharsets.UTF_8);
    }
}
