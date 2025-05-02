package com.example.homeservice.seguridad;

import android.util.Base64;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Arrays;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

public class CommonCrypto {

    private static SecretKey KEY;                  // se setea desde Login
    private static final String T = "AES/GCM/NoPadding";
    private static final int IV = 12;              // bytes
    private static final int TAG = 128;            // bits

    public static void init(SecretKey k){ KEY = k; }

    public static String encrypt(String plain) throws Exception {
        Cipher c = Cipher.getInstance(T);
        c.init(Cipher.ENCRYPT_MODE, KEY, new SecureRandom());
        byte[] iv  = c.getIV();
        byte[] enc = c.doFinal(plain.getBytes(StandardCharsets.UTF_8));
        ByteBuffer bb = ByteBuffer.allocate(IV + enc.length);
        bb.put(iv).put(enc);
        return Base64.encodeToString(bb.array(), Base64.NO_WRAP);
    }

    public static String decrypt(String b64) throws Exception {
        byte[] all = Base64.decode(b64, Base64.NO_WRAP);
        byte[] iv  = Arrays.copyOfRange(all, 0, IV);
        byte[] enc = Arrays.copyOfRange(all, IV, all.length);
        Cipher c = Cipher.getInstance(T);
        c.init(Cipher.DECRYPT_MODE, KEY, new GCMParameterSpec(TAG, iv));
        return new String(c.doFinal(enc), StandardCharsets.UTF_8);
    }
}
