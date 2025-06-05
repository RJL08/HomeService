package com.example.homeservice.utils;


import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Arrays;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class KeystoreManager {

    // ──────────── Constantes ────────────
    private static final String ANDROID_KEY_STORE = "AndroidKeyStore";
    private static final String AES_ALIAS  = "AES_Master";
    private static final String RSA_ALIAS  = "RSA_Wrap";
    private static final String AES_TRANSFORMATION = "AES/GCM/NoPadding";
    private static final String RSA_TRANSFORMATION = "RSA/ECB/PKCS1Padding";

    private static final int IV_SIZE      = 12;   // bytes
    private static final int TAG_LENGTH   = 128;  // bits

    private final KeyStore keyStore;

    // ──────────── INICIALIZACIÓN ────────────
    public KeystoreManager() throws Exception {
        keyStore = KeyStore.getInstance(ANDROID_KEY_STORE);
        keyStore.load(null);

        if (!keyStore.containsAlias(RSA_ALIAS)) generarParRSA();
        if (!keyStore.containsAlias(AES_ALIAS)) generarAES();
    }

    /* CREA un par RSA-2048 (pública/privada) para envolver la AES */
    private void generarParRSA() throws Exception {
        KeyPairGenerator kpg = KeyPairGenerator.getInstance(
                KeyProperties.KEY_ALGORITHM_RSA, ANDROID_KEY_STORE);

        kpg.initialize(new KeyGenParameterSpec.Builder(
                RSA_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                .setDigests(KeyProperties.DIGEST_SHA256)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_RSA_PKCS1)
                .build());
        kpg.generateKeyPair();
    }

    /* CREA la clave AES-256 y la guarda sin envolver */
    private void generarAES() throws Exception {
        KeyGenerator kg = KeyGenerator.getInstance(
                KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEY_STORE);
        kg.init(new KeyGenParameterSpec.Builder(
                AES_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setKeySize(256)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .build());
        kg.generateKey();
    }

    private SecretKey getAES() throws Exception {
        return ((KeyStore.SecretKeyEntry) keyStore.getEntry(AES_ALIAS, null)).getSecretKey();
    }
    private KeyPair getRSA() throws Exception {
        PrivateKey priv = (PrivateKey) keyStore.getKey(RSA_ALIAS, null);
        PublicKey pub  = keyStore.getCertificate(RSA_ALIAS).getPublicKey();
        return new KeyPair(pub, priv);
    }

    // ═══════════ CIFRAR / DESCIFRAR con AES (igual que antes) ═══════════
    public String encryptData(String plain) throws Exception {
        Cipher c = Cipher.getInstance(AES_TRANSFORMATION);
        c.init(Cipher.ENCRYPT_MODE, getAES());
        byte[] iv  = c.getIV();
        byte[] enc = c.doFinal(plain.getBytes(StandardCharsets.UTF_8));

        ByteBuffer bb = ByteBuffer.allocate(iv.length + enc.length);
        bb.put(iv).put(enc);
        return Base64.encodeToString(bb.array(), Base64.NO_WRAP);
    }
    public String decryptData(String base64) throws Exception {
        byte[] all = Base64.decode(base64, Base64.NO_WRAP);
        byte[] iv  = Arrays.copyOfRange(all, 0, IV_SIZE);
        byte[] enc = Arrays.copyOfRange(all, IV_SIZE, all.length);

        GCMParameterSpec spec = new GCMParameterSpec(TAG_LENGTH, iv);
        Cipher c = Cipher.getInstance(AES_TRANSFORMATION);
        c.init(Cipher.DECRYPT_MODE, getAES(), spec);
        return new String(c.doFinal(enc), StandardCharsets.UTF_8);
    }

    // ═══════════ EXPORTAR / IMPORTAR CLAVE AES ═══════════

    /** Devuelve la AES maestra **cifrada** con la clave RSA local (Base-64). */
    public String exportarAES() throws Exception {
        SecretKey aes = getAES();
        Cipher rsa = Cipher.getInstance(RSA_TRANSFORMATION);
        rsa.init(Cipher.ENCRYPT_MODE, getRSA().getPublic());
        byte[] envuelta = rsa.doFinal(aes.getEncoded());
        return Base64.encodeToString(envuelta, Base64.NO_WRAP);
    }

    /**
     * Importa una AES previamente exportada en otro dispositivo.
     * @param base64AESenv Encapsulado AES cifrado (Base-64) con la RSA del
     *                     dispositivo origen.
     */
    public void importarAES(String base64AESenv) throws Exception {
        byte[] env = Base64.decode(base64AESenv, Base64.NO_WRAP);

        Cipher rsa = Cipher.getInstance(RSA_TRANSFORMATION);
        rsa.init(Cipher.DECRYPT_MODE, getRSA().getPrivate());
        byte[] raw = rsa.doFinal(env);                       // ← desenvueltas

        // Guardamos la clave en el KeyStore reemplazando la antigua
        SecretKey nueva = new SecretKeySpec(raw, "AES");
        KeyStore.SecretKeyEntry entry = new KeyStore.SecretKeyEntry(nueva);
        keyStore.setEntry(AES_ALIAS, entry, null);
    }
}
