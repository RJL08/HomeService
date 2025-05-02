package com.example.homeservice.seguridad;

import android.util.Base64;
import android.util.Log;
import androidx.annotation.Nullable;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.DocumentSnapshot;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

public class CommonKeyProvider {

    private static final String TAG = "CommonKeyProvider";
    private static SecretKey COMMON;        // cache en memoria

    public interface Callback {
        void onReady(SecretKey key);
        void onError(Exception e);
    }

    /** Obtiene la AES común del doc config/commonKey. */
    public static void get(Callback cb) {
        if (COMMON != null) { cb.onReady(COMMON); return; }

        FirebaseFirestore.getInstance()
                .collection("config").document("commonKey").get()
                .addOnSuccessListener((DocumentSnapshot d) -> {
                    String b64 = d.getString("key");
                    if (b64 == null) { cb.onError(new Exception("Campo key vacío")); return; }
                    byte[] raw = Base64.decode(b64, Base64.NO_WRAP);
                    COMMON   = new SecretKeySpec(raw, "AES");
                    cb.onReady(COMMON);
                })
                .addOnFailureListener(cb::onError);
    }
}