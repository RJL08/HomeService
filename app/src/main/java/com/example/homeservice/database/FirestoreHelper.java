package com.example.homeservice.database;



import android.util.Log;



import com.example.homeservice.model.Anuncio;
import com.example.homeservice.model.Usuario;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Clase helper para manejar las operaciones en Firestore
 * (colecciones "usuarios" y "anuncios").
 */
public class FirestoreHelper {

    private static final String TAG = "FirestoreHelper";
    private static final String COLECCION_USUARIOS = "usuarios";
    private static final String COLECCION_ANUNCIOS = "anuncios";

    private final FirebaseFirestore db;


    public FirestoreHelper() {
        db = FirebaseFirestore.getInstance();
    }

    public FirebaseFirestore getDb() {
        return db;
    }

    /**
     * Crear o actualizar un usuario en la colección "usuarios".
     * @param userId ID del usuario (Firebase Auth)
     * @param usuario Objeto con los datos de usuario
     * @param onSuccess callback de éxito
     * @param onFailure callback de error
     */
    public void guardarUsuario(String userId, Usuario usuario,
                               OnSuccessListener<Void> onSuccess,
                               OnFailureListener onFailure) {
        db.collection(COLECCION_USUARIOS)
                .document(userId)
                .set(usuario)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Usuario guardado con ID: " + userId);
                    onSuccess.onSuccess(aVoid);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error al guardar usuario: " + e.getMessage());
                    onFailure.onFailure(e);
                });
    }

    /**
     * Leer datos de un usuario por su ID
     * @param userId ID del usuario
     * @param onComplete callback con el objeto Usuario (o null si no existe)
     * @param onFailure callback de error
     */
    public void leerUsuario(String userId,
                            OnSuccessListener<Usuario> onComplete,
                            OnFailureListener onFailure) {
        db.collection(COLECCION_USUARIOS)
                .document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Usuario usuario = documentSnapshot.toObject(Usuario.class);
                        onComplete.onSuccess(usuario);
                    } else {
                        // Si no existe el doc, devolvemos null
                        onComplete.onSuccess(null);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error al leer usuario: " + e.getMessage());
                    onFailure.onFailure(e);
                });
    }



    /**
     * Crear un anuncio en la colección "anuncios" (ID auto-generado).
     * @param anuncio Objeto Anuncio
     * @param onSuccess callback con el ID del documento creado
     * @param onFailure callback de error
     */
    public void crearAnuncio(
            Anuncio anuncio,
            OnSuccessListener<String> onSuccess,     // de com.google.android.gms.tasks
            OnFailureListener onFailure             // de com.google.android.gms.tasks
    ) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("anuncios")
                .add(anuncio)
                .addOnSuccessListener(documentReference -> {
                    // La parte de “éxito” regresa un DocumentReference
                    // Convertimos eso a String => docRef.getId()
                    onSuccess.onSuccess(documentReference.getId());
                })
                .addOnFailureListener(onFailure);
    }


    /**
     * Leer todos los anuncios de la colección "anuncios" y llamar a  onAnunciosCargados con la lista.
     * @param listener callback con la lista de anuncios cargados (o null si no hay anuncios)
     * @param errorListener callback de error (opcional)
     */
    public void leerAnuncios(OnAnunciosCargadosListener listener, OnErrorListener errorListener) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("anuncios")
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        errorListener.onError(error);  // <- aquí pasa el FirestoreException
                        return;
                    }

                    List<Anuncio> lista = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : value) {
                        Anuncio anuncio = doc.toObject(Anuncio.class);  // <- conversión correcta
                        lista.add(anuncio);
                    }
                    listener.onAnunciosCargados(lista);
                });
    }


    public interface OnAnunciosCargadosListener {
        void onAnunciosCargados(List<Anuncio> lista);
    }

    public interface OnErrorListener {
        void onError(Exception e);
    }



    /**
     * Leer los anuncios de un usuario concreto
     * @param userId ID del usuario (Firebase Auth)
     */
    public void leerAnunciosPorUsuario(String userId,
                                       OnSuccessListener<List<Anuncio>> onComplete,
                                       OnFailureListener onFailure) {
        db.collection(COLECCION_ANUNCIOS)
                .whereEqualTo("userId", userId)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<Anuncio> lista = new ArrayList<>();
                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        Anuncio anuncio = doc.toObject(Anuncio.class);
                        if (anuncio != null) {
                            lista.add(anuncio);
                        }
                    }
                    onComplete.onSuccess(lista);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error al leer anuncios por usuario: " + e.getMessage());
                    onFailure.onFailure(e);
                });
    }

    public void obtenerOcrearConversacion(String userId1, String userId2,
                                          OnSuccessListener<String> onSuccess,
                                          OnFailureListener onFailure) {
        // Genera un ID único ordenado (por ejemplo, concatenando los IDs ordenadamente)
        String conversationId = userId1.compareTo(userId2) < 0 ? userId1 + "_" + userId2 : userId2 + "_" + userId1;

        db.collection("conversaciones").document(conversationId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        onSuccess.onSuccess(conversationId);
                    } else {
                        // Si no existe la conversación, créala.
                        Map<String, Object> datos = new HashMap<>();
                        datos.put("participants", Arrays.asList(userId1, userId2));
                        datos.put("timestamp", FieldValue.serverTimestamp());
                        db.collection("conversaciones").document(conversationId)
                                .set(datos)
                                .addOnSuccessListener(aVoid -> onSuccess.onSuccess(conversationId))
                                .addOnFailureListener(onFailure);
                    }
                })
                .addOnFailureListener(onFailure);
    }

}
