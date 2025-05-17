package com.example.homeservice.notificaciones;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;

import androidx.activity.result.ActivityResultCaller;


import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;




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

    // 5.3 – Nuevo método para QUITAR el token si el usuario desactiva notificaciones
    public static void eliminarTokenDeFirestore(String token) {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) return;
        FirebaseFirestore.getInstance()
                .collection("usuarios")
                .document(uid)
                .update("fcmTokens", FieldValue.arrayRemove(token))
                .addOnSuccessListener(aVoid ->
                        Log.d("Firestore", "Token eliminado del array")
                )
                .addOnFailureListener(e ->
                        Log.e("Firestore", "Error al eliminar token", e)
                );
    }


}
