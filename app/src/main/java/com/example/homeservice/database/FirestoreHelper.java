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
import java.util.stream.Collectors;

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

    /******************* CIFRADO AL GUARDAR ANUNCIOS***********************/
    /**
     * Crea un anuncio cifrando los campos sensibles.
     */
    public void crearAnuncioCifrado(
            Anuncio anuncio,
            OnSuccessListener<String> onSuccess,
            OnFailureListener onFailure
    ) {
        Map<String,Object> datos = new HashMap<>();
        try {
            datos.put("titulo",       keystore.encryptData(anuncio.getTitulo()));
            datos.put("descripcion",  keystore.encryptData(anuncio.getDescripcion()));
            datos.put("oficio",       keystore.encryptData(anuncio.getOficio()));
            datos.put("localizacion", keystore.encryptData(anuncio.getLocalizacion()));
            datos.put("userId",       anuncio.getUserId());
            datos.put("fechaPublicacion", anuncio.getFechaPublicacion());
            // Coordenadas: si quieres cifrarlas, conviértelas a String
            datos.put("latitud",  keystore.encryptData(String.valueOf(anuncio.getLatitud())));
            datos.put("longitud", keystore.encryptData(String.valueOf(anuncio.getLongitud())));
            // Para las URLs de las imágenes, puedes dejar en claro o cifrar cada URL:
            List<String> imgs = anuncio.getListaImagenes().stream()
                    .map(url -> {
                        try { return keystore.encryptData(url); }
                        catch (Exception e) { return url; }
                    })
                    .collect(Collectors.toList());
            datos.put("listaImagenes", imgs);

        } catch(Exception e) {
            // Si algo falla, guardamos TODO en claro
            Log.w(TAG, "Error cifrando anuncio, guardo en claro", e);
            datos.clear();
            datos.put("titulo",       anuncio.getTitulo());
            datos.put("descripcion",  anuncio.getDescripcion());
            datos.put("oficio",       anuncio.getOficio());
            datos.put("localizacion", anuncio.getLocalizacion());
            datos.put("userId",       anuncio.getUserId());
            datos.put("fechaPublicacion", anuncio.getFechaPublicacion());
            datos.put("latitud",      anuncio.getLatitud());
            datos.put("longitud",     anuncio.getLongitud());
            datos.put("listaImagenes", anuncio.getListaImagenes());
        }

        db.collection("anuncios")
                .add(datos)
                .addOnSuccessListener(docRef -> onSuccess.onSuccess(docRef.getId()))
                .addOnFailureListener(onFailure);
    }

    /**
     * Lee un anuncio descifrando los campos que estaban cifrados.
     */
    public void leerAnunciosDescifrados(OnAnunciosCargadosListener listener,
                                        OnErrorListener errorListener) {
        db.collection(COLECCION_ANUNCIOS)
                .addSnapshotListener((snapshots, ex) -> {
                    if (ex != null) {
                        errorListener.onError(ex);
                        return;
                    }
                    List<Anuncio> lista = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : snapshots) {
                        Anuncio a = new Anuncio();
                        a.setId(doc.getId());
                        try {
                            // Campos cifrados
                            a.setTitulo(      keystore.decryptData(doc.getString("titulo")));
                            a.setDescripcion( keystore.decryptData(doc.getString("descripcion")));
                            a.setOficio(      keystore.decryptData(doc.getString("oficio")));
                            a.setLocalizacion(keystore.decryptData(doc.getString("localizacion")));
                            a.setUserId(      doc.getString("userId"));
                            a.setFechaPublicacion(doc.getLong("fechaPublicacion"));
                            a.setLatitud(  Double.parseDouble(keystore.decryptData(doc.getString("latitud"))));
                            a.setLongitud( Double.parseDouble(keystore.decryptData(doc.getString("longitud"))));
                            // Lista de imágenes cifradas
                            List<String> encImgs = (List<String>)doc.get("listaImagenes");
                            List<String> urls = new ArrayList<>();
                            for (String enc : encImgs) {
                                try {
                                    urls.add(keystore.decryptData(enc));
                                } catch (Exception e) {
                                    urls.add(enc);
                                }
                            }
                            a.setListaImagenes(urls);
                        } catch (Exception e) {
                            Log.w(TAG, "Error descifrando anuncio, uso valores en claro", e);
                            // Caer al modo “en claro” si falla el descifrado
                            a.setTitulo(      doc.getString("titulo"));
                            a.setDescripcion( doc.getString("descripcion"));
                            a.setOficio(      doc.getString("oficio"));
                            a.setLocalizacion(doc.getString("localizacion"));
                            a.setUserId(      doc.getString("userId"));
                            a.setFechaPublicacion(doc.getLong("fechaPublicacion"));
                            a.setLatitud(  doc.getDouble("latitud"));
                            a.setLongitud(doc.getDouble("longitud"));
                            a.setListaImagenes((List<String>)doc.get("listaImagenes"));
                        }
                        lista.add(a);
                    }
                    listener.onAnunciosCargados(lista);
                });
    }

    /**
     * Busca si ya existe una conversación entre uid1 y uid2.
     * Si la encuentra, devuelve su ID. Si no, la crea con los metadatos necesarios.
     *
     * @param uid1          El usuario que inicia el chat.
     * @param uid2          El otro usuario (publisher del anuncio).
     * @param serviceTitle  El título del servicio/anuncio.
     * @param onSuccess     Recibe el conversationId.
     * @param onFailure     Recibe el error.
     */
    public void getOrCreateConversation(
            String uid1,
            String uid2,
            String serviceTitle,
            OnSuccessListener<String> onSuccess,
            OnFailureListener onFailure
    ) {
        CollectionReference convRef = db.collection("conversaciones");

        // 1) Busca cualquier doc donde participants contenga uid1...
        convRef.whereArrayContains("participants", uid1)
                .get()
                .addOnSuccessListener(query -> {
                    // 2) ...y entre esos comprueba si también participan uid2
                    for (DocumentSnapshot doc : query.getDocuments()) {
                        List<String> parts = (List<String>) doc.get("participants");
                        if (parts != null && parts.contains(uid2)) {
                            onSuccess.onSuccess(doc.getId());
                            return;
                        }
                    }
                    // 3) Si no existe, creamos uno nuevo:
                    Map<String,Object> data = new HashMap<>();
                    data.put("participants", Arrays.asList(uid1, uid2));
                    data.put("serviceTitle", serviceTitle);
                    data.put("lastMessage", "");
                    data.put("timestamp", FieldValue.serverTimestamp());
                    // Inicializamos los campos de “última lectura” para cada usuario
                    data.put("lastReadBy_" + uid1, 0L);
                    data.put("lastReadBy_" + uid2, 0L);

                    convRef
                            .add(data)
                            .addOnSuccessListener(ref -> onSuccess.onSuccess(ref.getId()))
                            .addOnFailureListener(onFailure);

                })
                .addOnFailureListener(onFailure);
    }

}


