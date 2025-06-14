package com.example.homeservice.ui.Anuncios;

import static android.content.Context.MODE_PRIVATE;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.homeservice.MyApp;
import com.example.homeservice.R;
import com.example.homeservice.interfaz.OnAnuncioClickListener;
import com.example.homeservice.database.FirestoreHelper;
import com.example.homeservice.databinding.FragmentHomeBinding;
import com.example.homeservice.interfaz.OnFavoriteToggleListener;
import com.example.homeservice.model.Anuncio;
import com.example.homeservice.adapter.AnuncioAdapter;
import com.example.homeservice.notificaciones.GestorNotificaciones;
import com.example.homeservice.utils.LocationHelper;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.functions.FirebaseFunctions;
import com.google.firebase.ktx.BuildConfig;
import com.google.firebase.messaging.FirebaseMessaging;


import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class HomeFragment extends Fragment implements OnAnuncioClickListener {

    private FragmentHomeBinding binding;
    private RecyclerView recyclerView;
    private AnuncioAdapter adapter;
    private List<Anuncio> listaAnuncios;
    private Button btnCategorias;
    private Set<String> favoritosIds = new HashSet<>();
    private FirebaseAuth firebaseAuth;
    private LocationHelper locationHelper;
    private ActivityResultLauncher<String> locationPermissionLauncher;



    private final ActivityResultLauncher<String> notiPermLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.RequestPermission(),
                    isGranted -> {
                        if (isGranted) {
                            // Ya puedes mostrar notificaciones: sube el token
                            FirebaseMessaging.getInstance().getToken()
                                    .addOnSuccessListener(GestorNotificaciones::subirTokenAFirestore);
                        } else {
                            // Aquí informas al usuario que sin este permiso no habrá notis
                            Toast.makeText(requireContext(),
                                    "Sin permiso de notificaciones no recibirás alertas",
                                    Toast.LENGTH_LONG).show();
                        }
                    }
            );

    private void pedirPermisoNoti() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) { // Android 13+
            if (ContextCompat.checkSelfPermission(requireContext(),
                    Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {

                // Mostrar explicación si el usuario ya denegó el permiso antes
                if (shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS)) {
                    new AlertDialog.Builder(requireContext())
                            .setTitle("Permiso necesario")
                            .setMessage("Permite notificaciones para recibir alertas de mensajes nuevos")
                            .setPositiveButton("Aceptar", (d, w) ->
                                    notiPermLauncher.launch(Manifest.permission.POST_NOTIFICATIONS))
                            .setNegativeButton("Cancelar", null)
                            .show();
                } else {
                    // Pedir permiso directamente
                    notiPermLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
                }
            } else {
                // Permiso ya concedido: generar token
                manejarTokenFCM();
            }
        } else {
            // Android <13: No requiere permiso, pero hay que generar token
            manejarTokenFCM();
        }
    }

    private void manejarTokenFCM() {
        FirebaseMessaging.getInstance().getToken()
                .addOnSuccessListener(token -> {
                    // Guardar token SOLO si el usuario está autenticado
                    if (FirebaseAuth.getInstance().getCurrentUser() != null) {
                        GestorNotificaciones.subirTokenAFirestore(token);
                    } else {
                        Log.w("FCM", "Usuario no autenticado, token no guardado");
                    }
                })
                .addOnFailureListener(e ->
                        Log.e("FCM", "Error al obtener token", e)
                );
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        pedirPermisoNoti();

        firebaseAuth   = FirebaseAuth.getInstance();
        locationHelper = new LocationHelper(requireActivity());

        // 1) Configura el lanzador de permiso
        locationPermissionLauncher =
                registerForActivityResult(
                        new ActivityResultContracts.RequestPermission(),
                        isGranted -> {
                            if (isGranted) {
                                obtenerYActualizarUbicacion();
                            } else {
                                Log.w("HomeFragment", "Permiso de ubicación denegado");
                            }
                        }
                );

        // Configuro el RecyclerView
        recyclerView = binding.recyclerViewAnuncios;
        // ① Creo y asigno el GridLayoutManager
        GridLayoutManager glm = new GridLayoutManager(getContext(), 2);
        glm.setInitialPrefetchItemCount(5);
        recyclerView.setLayoutManager(glm);

        // ② Cache interno de views para evitar inflados constantes
        recyclerView.setItemViewCacheSize(20);


        // Inicializo la lista y el adapter con el listener de favoritos
        listaAnuncios = new ArrayList<>();
        adapter = new AnuncioAdapter(
                listaAnuncios,
                this,
                new OnFavoriteToggleListener() {
                    @Override
                    public void onFavoriteAdded(Anuncio anuncio) {
                        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
                        new FirestoreHelper().agregarAFavoritos(uid, anuncio.getId(),
                                aVoid -> Toast.makeText(getContext(),"Añadido a favoritos",Toast.LENGTH_SHORT).show(),
                                e -> Toast.makeText(getContext(),"Error: "+e.getMessage(),Toast.LENGTH_SHORT).show()
                        );
                    }
                    @Override
                    public void onFavoriteRemoved(Anuncio anuncio) {
                        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
                        new FirestoreHelper().eliminarDeFavoritos(uid, anuncio.getId(),
                                aVoid -> Toast.makeText(getContext(),"Eliminado de favoritos",Toast.LENGTH_SHORT).show(),
                                e -> Toast.makeText(getContext(),"Error: "+e.getMessage(),Toast.LENGTH_SHORT).show()
                        );
                    }
                }
        );
        recyclerView.setAdapter(adapter);


        // Botón de categorías
        btnCategorias = root.findViewById(R.id.btnCategorias);
        btnCategorias.setOnClickListener(v -> {
            startActivity(new Intent(requireContext(), CategoriasActivity.class));
        });

        // Cargo los anuncios
        cargarAnuncios();


        // 3) Pide o comprueba permiso de ubicación
        if (ContextCompat.checkSelfPermission(requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            obtenerYActualizarUbicacion();
        } else {
            locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION);
        }

        // se hostea la ip del emulador para reaalizar purebas con las notificaciones
        if (BuildConfig.DEBUG &&
                (Build.FINGERPRINT.startsWith("generic")
                        || Build.MODEL.contains("Emulator")
                        || Build.MODEL.contains("Android SDK built for x86"))) {
            // uso de emulador local
            FirebaseFirestore.getInstance().useEmulator("10.0.2.2", 8082);
            FirebaseFunctions.getInstance().useEmulator("10.0.2.2", 5001);
        }

        MyApp.getKeyReady().observe(getViewLifecycleOwner(), ready -> {
            if (Boolean.TRUE.equals(ready)) {
                cargarAnuncios();
            } else {
                Toast.makeText(requireContext(),
                        "Error al inicializar clave de descifrado", Toast.LENGTH_LONG).show();
            }
        });


        return root;
    }

    /** Copiado tal cual de tu Login: obtiene lat/lon, geocodifica y actualiza Firestore */
    private void obtenerYActualizarUbicacion() {
        FusedLocationProviderClient client =
                LocationServices.getFusedLocationProviderClient(requireActivity());
        if (ActivityCompat.checkSelfPermission(requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        client.getLastLocation()
                .addOnSuccessListener(location -> {
                   // if (location == null) return;
                    if (!isAdded() || location == null) return;
                    double lat = location.getLatitude();
                    double lon = location.getLongitude();
                    // Guardamos en SharedPreferences
                    guardarCoords(lat, lon);

                    // Reverse-geocode con Geocoder:
                    String ciudad = "Desconocido";
                    try {
                        //List<Address> dirs = new Geocoder(requireContext(), Locale.getDefault())
                          //      .getFromLocation(lat, lon, 1);
                        List<Address> dirs = new Geocoder(
                                /*ctx=*/ requireContext().getApplicationContext(),
                                Locale.getDefault())
                                .getFromLocation(lat, lon, 1);
                        if (!dirs.isEmpty()) {
                            Address a = dirs.get(0);
                            ciudad = a.getLocality() != null
                                    ? a.getLocality()
                                    : a.getAdminArea();
                        }
                    } catch (IOException e) {
                        Log.e("HomeFragment", "Error geocodificando", e);
                    }

                    // Leemos y actualizamos el usuario cifrado:
                    String uid = firebaseAuth.getCurrentUser().getUid();
                    if (uid == null) return;
                    FirestoreHelper helper = new FirestoreHelper();
                    String finalCiudad = ciudad;
                    helper.leerUsuarioDescifrado(
                            uid,
                            usuarioLeido -> {
                                if (usuarioLeido == null) return;
                                usuarioLeido.setLat(lat);
                                usuarioLeido.setLon(lon);
                                usuarioLeido.setLocalizacion(finalCiudad);
                                helper.guardarUsuarioCifrado(
                                        uid,
                                        usuarioLeido,
                                        aVoid -> Log.d("HomeFragment",
                                                "Ubicación actualizada a " + finalCiudad),
                                        e -> Log.e("HomeFragment",
                                                "Error guardando ubicación", e)
                                );
                            },
                            e -> Log.e("HomeFragment", "Error leyendo usuario", e)
                    );
                })
                .addOnFailureListener(e ->
                        Log.e("HomeFragment", "Error obteniendo localización", e)
                );
    }


    private void guardarCoords(double lat, double lon) {

        Context appCtx = requireContext().getApplicationContext();
        appCtx.getSharedPreferences("MyAppPrefs", Context.MODE_PRIVATE)
                .edit()
                .putFloat("userLat", (float) lat)
                .putFloat("userLon", (float) lon)
                .apply();
    }


    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        locationHelper.handleRequestPermissionsResult(
                requestCode, permissions, grantResults,
                (lat, lon) -> {
                    // permiso concedido: volvemos a llamar
                    obtenerYActualizarUbicacion();
                },
                ex -> {
                    Log.w("HomeFragment", "Permiso ubicación denegado o fallo", ex);
                }
        );
    }

    @Override
    public void onAnuncioClick(Anuncio anuncio) {
        // Abre pantalla de detalle
        Intent intent = new Intent(requireContext(), DetalleAnuncioActivity.class);
        intent.putExtra("anuncio", anuncio);
        startActivity(intent);
    }

    private void cargarAnuncios() {
        new FirestoreHelper().leerAnunciosDescifrados(
                lista -> {
                    if (!isAdded()) return;
                    /* 1) Obtener ubicación guardada */
                    SharedPreferences prefs = requireContext()
                            .getSharedPreferences("MyAppPrefs", MODE_PRIVATE);
                    double userLat = prefs.getFloat("userLat", 999);
                    double userLon = prefs.getFloat("userLon", 999);

                    /* 2) Calcular distancia para cada anuncio */
                    for (Anuncio a : lista) {
                        if (userLat != 999) {
                            double dKm = haversineKm(userLat, userLon,
                                    a.getLatitud(), a.getLongitud());
                            a.setDistanceKm(dKm);
                        }
                    }

                    /* 3) Ordenar: distancia asc y fecha desc */
                    Collections.sort(lista, (a1, a2) -> {
                        int cmp = Double.compare(a1.getDistanceKm(), a2.getDistanceKm());
                        if (cmp != 0) return cmp;            // distinto radio
                        // misma distancia → más reciente primero
                        return Long.compare(a2.getFechaPublicacion(),
                                a1.getFechaPublicacion());
                    });

                    /* 4) Actualizar RecyclerView */
                    listaAnuncios.clear();
                    listaAnuncios.addAll(lista);
                    /* 4a) Pre‐carga con Glide de la primera imagen de cada anuncio */
                    for (Anuncio a : listaAnuncios) {
                        if (!a.getListaImagenes().isEmpty()) {
                            Glide.with(requireContext())
                                    .load(a.getListaImagenes().get(0))
                                    .preload();
                        }
                    }
                    sincronizarFavoritos();   // mantienes favoritos + notifyDataSetChanged()
                    adapter.notifyDataSetChanged();
                },
                e -> {
                    /* ② ── También aquí ── */
                    if (!isAdded()) return;
                    Toast.makeText(requireContext(),
                            "Error: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                }
        );
    }

    /**
     * Calcula la distancia entre dos puntos en km. Utiliza la fórmula del haversine.
     * @param lat1
     * @param lon1
     * @param lat2
     * @param lon2
     * @return
     */
    private static double haversineKm(double lat1, double lon1,
                                      double lat2, double lon2) {
        double R = 6371;                       // Radio Tierra km
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat/2) * Math.sin(dLat/2) +
                Math.cos(Math.toRadians(lat1)) *
                        Math.cos(Math.toRadians(lat2)) *
                        Math.sin(dLon/2) * Math.sin(dLon/2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
        return R * c;
    }

    /**
     * Metodo para sincronizar favoritos con Firestore y actualizar el adapter.
     */
    private void sincronizarFavoritos() {
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        new FirestoreHelper().leerFavoritosDeUsuario(uid,
                anuncioIds -> {
                    favoritosIds.clear();
                    favoritosIds.addAll(anuncioIds);
                    // marcamos cada anuncio
                    for (Anuncio a : listaAnuncios) {
                        a.setFavorite(favoritosIds.contains(a.getId()));
                    }
                    adapter.notifyDataSetChanged();
                },
                e -> {
                    // opcional: log o toast
                }
        );
    }

    @Override
    public void onResume() {
        super.onResume();
        sincronizarFavoritos();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}