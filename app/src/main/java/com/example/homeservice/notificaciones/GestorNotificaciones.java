package com.example.homeservice.notificaciones;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;

import androidx.activity.result.ActivityResultCaller;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.Collections;

public class GestorNotificaciones {


    // En GestorNotificaciones.java
    public static void subirTokenAFirestore(String token) {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) return;

        FirebaseFirestore.getInstance()
                .collection("usuarios")
                .document(uid)
                .update( // <--- Usa update(), no set()
                        "fcmTokens", FieldValue.arrayUnion(token)
                )
                .addOnSuccessListener(aVoid ->
                        Log.d("Firestore", "Token añadido al array")
                )
                .addOnFailureListener(e ->
                        Log.e("Firestore", "Error al añadir token", e)
                );
    }

}
