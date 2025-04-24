package com.example.homeservice.database;



import android.util.Log;



import com.example.homeservice.model.Anuncio;
import com.example.homeservice.model.Usuario;
import com.example.homeservice.utils.KeystoreManager;
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
    private static final String COLECCION_FAVORITOS = "favoritos";
    private final FirebaseFirestore db;
    private final KeystoreManager keystore;

    public FirestoreHelper() {
        db = FirebaseFirestore.getInstance();
        KeystoreManager tmp = null;
        try {
            tmp = new KeystoreManager();
        } catch (Exception e) {
            Log.e(TAG, "No se pudo inicializar KeystoreManager, seguiré sin cifrado", e);
            // tmp queda nulo, tus métodos de cifrado deberán comprobarlo antes de usar
        }
        keystore = tmp;
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

/***********************************CIFRDO*******************************/

    /**
     * Guarda un Usuario cifrando los campos sensibles si keystore != null,
     * o en claro si falla la inicialización.
     */
    public void guardarUsuarioCifrado(String userId,
                                      Usuario u,
                                      OnSuccessListener<Void> onSuccess,
                                      OnFailureListener onFailure) {
        Map<String,Object> datos = new HashMap<>();

        if (keystore != null) {
            try {
                datos.put("nombre",       keystore.encryptData(u.getNombre()));
                datos.put("apellidos",    keystore.encryptData(u.getApellidos()));
                datos.put("correo",       keystore.encryptData(u.getCorreo()));
                datos.put("localizacion", keystore.encryptData(u.getLocalizacion()));
                datos.put("fotoPerfil",   keystore.encryptData(u.getFotoPerfil()));
                // lat/lon cifrados:
                datos.put("lat",  keystore.encryptData(Double.toString(u.getLat())));
                datos.put("lon",  keystore.encryptData(Double.toString(u.getLon())));
            } catch (Exception e) {
                Log.w(TAG, "Error cifrando campos, guardo todos en claro", e);
                datos.clear();
                datos.put("nombre",       u.getNombre());
                datos.put("apellidos",    u.getApellidos());
                datos.put("correo",       u.getCorreo());
                datos.put("localizacion", u.getLocalizacion());
                datos.put("fotoPerfil",   u.getFotoPerfil());
                datos.put("lat",          u.getLat());
                datos.put("lon",          u.getLon());
            }
        } else {
            // Keystore no disponible -> todo en claro
            datos.put("nombre",       u.getNombre());
            datos.put("apellidos",    u.getApellidos());
            datos.put("correo",       u.getCorreo());
            datos.put("localizacion", u.getLocalizacion());
            datos.put("fotoPerfil",   u.getFotoPerfil());
            datos.put("lat",          u.getLat());
            datos.put("lon",          u.getLon());
        }

        db.collection(COLECCION_USUARIOS)
                .document(userId)
                .set(datos)
                .addOnSuccessListener(onSuccess)
                .addOnFailureListener(onFailure);
    }

    public void leerUsuarioDescifrado(String userId,
                                      OnSuccessListener<Usuario> onComplete,
                                      OnFailureListener onFailure) {
        db.collection(COLECCION_USUARIOS)
                .document(userId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (!doc.exists()) {
                        onComplete.onSuccess(null);
                        return;
                    }
                    Usuario u = new Usuario();
                    u.setId(userId);

                    if (keystore != null) {
                        try {
                            u.setNombre(       keystore.decryptData(doc.getString("nombre")));
                            u.setApellidos(    keystore.decryptData(doc.getString("apellidos")));
                            u.setCorreo(       keystore.decryptData(doc.getString("correo")));
                            u.setLocalizacion( keystore.decryptData(doc.getString("localizacion")));
                            u.setFotoPerfil(   keystore.decryptData(doc.getString("fotoPerfil")));
                            // lat/lon descifrados:
                            String latStr = keystore.decryptData(doc.getString("lat"));
                            String lonStr = keystore.decryptData(doc.getString("lon"));
                            u.setLat(Double.parseDouble(latStr));
                            u.setLon(Double.parseDouble(lonStr));
                        } catch (Exception e) {
                            Log.w(TAG, "Error descifrando campos, uso valores en claro", e);
                            u.setNombre(       doc.getString("nombre"));
                            u.setApellidos(    doc.getString("apellidos"));
                            u.setCorreo(       doc.getString("correo"));
                            u.setLocalizacion( doc.getString("localizacion"));
                            u.setFotoPerfil(   doc.getString("fotoPerfil"));
                            u.setLat(          doc.getDouble("lat"));
                            u.setLon(          doc.getDouble("lon"));
                        }
                    } else {
                        // Keystore no disponible: todo en claro
                        u.setNombre(       doc.getString("nombre"));
                        u.setApellidos(    doc.getString("apellidos"));
                        u.setCorreo(       doc.getString("correo"));
                        u.setLocalizacion( doc.getString("localizacion"));
                        u.setFotoPerfil(   doc.getString("fotoPerfil"));
                        u.setLat(          doc.getDouble("lat"));
                        u.setLon(          doc.getDouble("lon"));
                    }

                    onComplete.onSuccess(u);
                })
                .addOnFailureListener(onFailure);
    }

    /******************************************************************/

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
                        Anuncio anuncio = doc.toObject(Anuncio.class);
                        anuncio.setId(doc.getId());// <- conversión correcta
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

    /**
     * Agrega un anuncio a favoritos.
     * Se crea un documento en la colección "favoritos" con el userId y anuncioId.
     */
    public void agregarAFavoritos(String userId, String anuncioId,
                                  OnSuccessListener<Void> onSuccess,
                                  OnFailureListener onFailure) {
        // Creamos un mapa con los datos
        Map<String, Object> datos = new HashMap<>();
        datos.put("userId", userId);
        datos.put("anuncioId", anuncioId);
        datos.put("timestamp", FieldValue.serverTimestamp());

        // Usamos como id, por ejemplo, la concatenación de userId y anuncioId, o Firestore podrá generarlo.
        String favoriteDocId = userId + "_" + anuncioId;
        db.collection(COLECCION_FAVORITOS)
                .document(favoriteDocId)
                .set(datos)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Favorito agregado: " + favoriteDocId);
                    onSuccess.onSuccess(aVoid);
                })
                .addOnFailureListener(onFailure);
    }

    /**
     * Elimina un anuncio de favoritos.
     */
    public void eliminarDeFavoritos(String userId, String anuncioId,
                                    OnSuccessListener<Void> onSuccess,
                                    OnFailureListener onFailure) {
        String favoriteDocId = userId + "_" + anuncioId;
        db.collection(COLECCION_FAVORITOS)
                .document(favoriteDocId)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Favorito eliminado: " + favoriteDocId);
                    onSuccess.onSuccess(aVoid);
                })
                .addOnFailureListener(onFailure);
    }

    /**
     * Obtiene la lista de favoritos para un usuario.
     */
    public void leerFavoritosDeUsuario(String userId,
                                       OnSuccessListener<List<String>> onSuccess,
                                       OnFailureListener onFailure) {
        db.collection(COLECCION_FAVORITOS)
                .whereEqualTo("userId", userId)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<String> listaAnuncioIds = new ArrayList<>();
                    for (DocumentSnapshot doc : querySnapshot) {
                        String anuncioId = doc.getString("anuncioId");
                        if (anuncioId != null) {
                            listaAnuncioIds.add(anuncioId);
                        }
                    }
                    onSuccess.onSuccess(listaAnuncioIds);
                })
                .addOnFailureListener(onFailure);
    }
}


