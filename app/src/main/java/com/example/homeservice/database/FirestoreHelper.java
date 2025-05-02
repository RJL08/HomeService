package com.example.homeservice.database;



import android.util.Log;
import com.example.homeservice.model.Anuncio;
import com.example.homeservice.model.Usuario;
import com.example.homeservice.seguridad.CommonCrypto;
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
    public void leerAnunciosPorUsuario(
            String userId,
            OnSuccessListener<List<Anuncio>> onComplete,
            OnFailureListener onFailure
    ) {
        db.collection(COLECCION_ANUNCIOS)
                .whereEqualTo("userId", userId)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<Anuncio> lista = new ArrayList<>();
                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        Anuncio a = new Anuncio();
                        a.setId(doc.getId());
                        try {
                            // descifrar campos sensibles
                            a.setTitulo(CommonCrypto.decrypt(doc.getString("titulo")));
                            a.setDescripcion(CommonCrypto.decrypt(doc.getString("descripcion")));
                            a.setOficio(CommonCrypto.decrypt(doc.getString("oficio")));
                            a.setLocalizacion(CommonCrypto.decrypt(doc.getString("localizacion")));
                            // lat/lon como String cifrado → descifrar y parsear
                            String latStr = CommonCrypto.decrypt(doc.getString("latitud"));
                            String lonStr = CommonCrypto.decrypt(doc.getString("longitud"));
                            a.setLatitud(Double.parseDouble(latStr));
                            a.setLongitud(Double.parseDouble(lonStr));
                            // lista de URLs
                            List<String> encImgs = (List<String>) doc.get("listaImagenes");
                            List<String> urls = new ArrayList<>();
                            for (String enc : encImgs) {
                                urls.add(CommonCrypto.decrypt(enc));
                            }
                            a.setListaImagenes(urls);

                        } catch (Exception e) {
                            // fallback “en claro”
                            Log.w(TAG, "Error descifrando anuncio propio, uso claro", e);
                            a.setTitulo(doc.getString("titulo"));
                            a.setDescripcion(doc.getString("descripcion"));
                            a.setOficio(doc.getString("oficio"));
                            a.setLocalizacion(doc.getString("localizacion"));
                            Object latF = doc.get("latitud"), lonF = doc.get("longitud");
                            if (latF instanceof Number) a.setLatitud(((Number)latF).doubleValue());
                            if (lonF instanceof Number) a.setLongitud(((Number)lonF).doubleValue());
                            a.setListaImagenes((List<String>) doc.get("listaImagenes"));
                        }
                        // campos no cifrados
                        a.setUserId(doc.getString("userId"));
                        a.setFechaPublicacion(doc.getLong("fechaPublicacion"));
                        lista.add(a);
                    }
                    onComplete.onSuccess(lista);
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
            Anuncio a,
            OnSuccessListener<String> ok,
            OnFailureListener err) {

        Map<String,Object> d = new HashMap<>();

        try {
            d.put("titulo",        CommonCrypto.encrypt(a.getTitulo()));
            d.put("descripcion",   CommonCrypto.encrypt(a.getDescripcion()));
            d.put("oficio",        CommonCrypto.encrypt(a.getOficio()));
            d.put("localizacion",  CommonCrypto.encrypt(a.getLocalizacion()));

            // Coordenadas como String cifrado
            d.put("latitud",  CommonCrypto.encrypt(Double.toString(a.getLatitud())));
            d.put("longitud", CommonCrypto.encrypt(Double.toString(a.getLongitud())));

            // Cada URL cifrada individualmente
            List<String> urlsEnc = new ArrayList<>();
            for (String url : a.getListaImagenes()) {
                urlsEnc.add( CommonCrypto.encrypt(url) );
            }
            d.put("listaImagenes", urlsEnc);

        } catch (Exception e) {                     // Fallback en claro
            Log.e(TAG,"Cifrado anuncio falló → guardo en claro",e);
            d.clear();
            d.put("titulo",        a.getTitulo());
            d.put("descripcion",   a.getDescripcion());
            d.put("oficio",        a.getOficio());
            d.put("localizacion",  a.getLocalizacion());
            d.put("latitud",       a.getLatitud());
            d.put("longitud",      a.getLongitud());
            d.put("listaImagenes", a.getListaImagenes());
        }

        // Campos no sensibles
        d.put("userId",           a.getUserId());
        d.put("fechaPublicacion", a.getFechaPublicacion());

        db.collection(COLECCION_ANUNCIOS)
                .add(d)
                .addOnSuccessListener(ref -> ok.onSuccess(ref.getId()))
                .addOnFailureListener(err);
    }



    /**
     * Lee un anuncio descifrando los campos que estaban cifrados.
     */
    public void leerAnunciosDescifrados(OnAnunciosCargadosListener cb,
                                        OnErrorListener err) {

        db.collection(COLECCION_ANUNCIOS)
                .addSnapshotListener((snap, ex) -> {
                    if (ex != null) { err.onError(ex); return; }

                    List<Anuncio> lista = new ArrayList<>();

                    for (QueryDocumentSnapshot d : snap) {
                        Anuncio a = new Anuncio();  a.setId(d.getId());

                        try {   /* —— descifrar todos los campos —— */
                            a.setTitulo       ( CommonCrypto.decrypt(d.getString("titulo")) );
                            a.setDescripcion  ( CommonCrypto.decrypt(d.getString("descripcion")) );
                            a.setOficio       ( CommonCrypto.decrypt(d.getString("oficio")) );
                            a.setLocalizacion ( CommonCrypto.decrypt(d.getString("localizacion")) );

                            String latStr = CommonCrypto.decrypt(d.getString("latitud"));
                            String lonStr = CommonCrypto.decrypt(d.getString("longitud"));
                            a.setLatitud ( Double.parseDouble(latStr) );
                            a.setLongitud( Double.parseDouble(lonStr) );

                            List<String> urls = new ArrayList<>();
                            for (String enc : (List<String>) d.get("listaImagenes")) {
                                urls.add( CommonCrypto.decrypt(enc) );
                            }
                            a.setListaImagenes(urls);

                        } catch (Exception e) {      /* —— si algo falla, intenta en claro —— */
                            Log.w(TAG,"Descifrado falló, intento claro",e);
                            a.setTitulo       ( d.getString("titulo") );
                            a.setDescripcion  ( d.getString("descripcion") );
                            a.setOficio       ( d.getString("oficio") );
                            a.setLocalizacion ( d.getString("localizacion") );
                            a.setLatitud      ( d.getDouble("latitud") );
                            a.setLongitud     ( d.getDouble("longitud") );
                            a.setListaImagenes( (List<String>) d.get("listaImagenes") );
                        }

                        a.setUserId          ( d.getString("userId") );
                        a.setFechaPublicacion( d.getLong("fechaPublicacion") );

                        lista.add(a);
                    }
                    cb.onAnunciosCargados(lista);
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


