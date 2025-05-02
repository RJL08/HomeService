package com.example.homeservice.notificaciones;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.activity.result.ActivityResultCaller;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.messaging.FirebaseMessaging;

import java.util.Collections;

public class GestorNotificaciones {

    /* ════════════════════════════════════════════════
       1. Pedir permiso justo después del login
       ════════════════════════════════════════════════ */
    // NUEVO: para llamar directo desde un Fragment
    public static void pedirPermisoSiHaceFalta(Fragment fragment) {

        if (Build.VERSION.SDK_INT < 33) {      // TIRAMISU+
            obtenerYSubirToken();
            return;
        }

        if (ContextCompat.checkSelfPermission(
                fragment.requireContext(),
                Manifest.permission.POST_NOTIFICATIONS)
                == PackageManager.PERMISSION_GRANTED) {
            obtenerYSubirToken();
            return;
        }

        ActivityResultLauncher<String> lanzador =
                fragment.registerForActivityResult(
                        new ActivityResultContracts.RequestPermission(),
                        concedido -> { if (concedido) obtenerYSubirToken(); });

        lanzador.launch(Manifest.permission.POST_NOTIFICATIONS);
    }


    /* ════════════════════════════════════════════════ */
    private static void obtenerYSubirToken() {
        FirebaseMessaging.getInstance().getToken()
                .addOnSuccessListener(GestorNotificaciones::subirTokenAFirestore);
    }

    public static void subirTokenAFirestore(String token) {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) return;

        FirebaseFirestore.getInstance()
                .collection("usuarios").document(uid)
                .set(
                        Collections.singletonMap(
                                "fcmTokens",
                                FieldValue.arrayUnion(token)),
                        SetOptions.merge()
                );
    }
}
