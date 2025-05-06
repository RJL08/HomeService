package com.example.homeservice;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.util.Patterns;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.MimeTypeMap;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;
import android.os.Bundle;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.core.view.WindowCompat;

import com.example.homeservice.database.FirestoreHelper;
import com.example.homeservice.model.Usuario;
import com.example.homeservice.notificaciones.GestorNotificaciones;
import com.example.homeservice.seguridad.CommonCrypto;
import com.example.homeservice.seguridad.CommonKeyProvider;
import com.example.homeservice.utils.KeystoreManager;
import com.example.homeservice.utils.LocationHelper;
import com.example.homeservice.utils.LocationIQHelper;
import com.example.homeservice.utils.ValidacionUtils;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.firebase.auth.*;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageMetadata;
import com.google.firebase.storage.StorageReference;

import java.io.File;

import javax.crypto.SecretKey;

/**
 * Actividad para registrar nuevos usuarios en Firebase Authentication
 * y guardar datos extra (nombre, apellidos) en Firestore.
 */
public class Registro extends AppCompatActivity {

    private EditText etNombre, etApellidos;
    private EditText etCorreoRegistro, etContrasenaRegistro;
    private Button btnRegistrar;

    private FirebaseAuth firebaseAuth;
    private FirebaseFirestore firestore;

    private LocationHelper locationHelper;
    private String userIdCreado;
    private boolean registroCompleto = false;
    private EditText etRepetirContrasena;

    // Al principio de la clase:
    private static final int LIMITE_IMAGENES = 1; // aquí sólo 1
    private Uri perfilUri = null;

    // ActivityResultLaunchers:
    private ActivityResultLauncher<Intent> cameraLauncher;
    private ActivityResultLauncher<String> galleryLauncher;
    private ActivityResultLauncher<String> requestCameraPermissionLauncher;
    private ActivityResultLauncher<String> requestGalleryPermissionLauncher;

    // Y enlaza tu ImageButton:
    private ImageButton btnAgregarFoto;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d("RegistroDebug", "onCreate: Iniciando Registro activity");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_registro);

        // 2) Flags edge-to-edge (para que se pinte hasta arriba)
        Window window = getWindow();
        WindowCompat.setDecorFitsSystemWindows(window, false);

        // 3) Quitamos translúcido, permitimos dibujar fondo de barra
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            // 4) Fijamos el mismo color que tu toolbar
            window.setStatusBarColor(ContextCompat.getColor(this, R.color.logo_background));
        }



        firebaseAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();

        etNombre = findViewById(R.id.etNombre);
        etApellidos = findViewById(R.id.etApellidos);
        etCorreoRegistro = findViewById(R.id.etCorreoRegistro);
        etContrasenaRegistro = findViewById(R.id.etContrasenaRegistro);
        btnRegistrar = findViewById(R.id.btnRegistrar);
        etRepetirContrasena = findViewById(R.id.etRepetirContrasena);
        btnAgregarFoto = findViewById(R.id.btnAgregarFoto);

        // 1) Tomar foto por cámara
        cameraLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && perfilUri != null) {
                        btnAgregarFoto.setImageURI(perfilUri);
                    }
                }
        );

// 2) Pedir permiso de cámara
        requestCameraPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                granted -> {
                    if (granted) lanzarCamara();
                    else Toast.makeText(this, "Permiso cámara denegado", Toast.LENGTH_SHORT).show();
                }
        );

// 3) Elegir de galería
        galleryLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null) {
                        perfilUri = uri;
                        btnAgregarFoto.setImageURI(uri);
                    }
                }
        );

// 4) Pedir permiso lectura
        requestGalleryPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                granted -> {
                    if (granted) openGallery();
                    else Toast.makeText(this, "Permiso almacenamiento denegado", Toast.LENGTH_SHORT).show();
                }
        );

// 5) OnClick del botón:
        btnAgregarFoto.setOnClickListener(v -> {
            String[] options = {"Hacer foto", "Elegir de galería"};
            new AlertDialog.Builder(this)
                    .setTitle("Seleccionar imagen de perfil")
                    .setItems(options, (d, which) -> {
                        if (which == 0) { // cámara
                            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                                    == PackageManager.PERMISSION_GRANTED) {
                                lanzarCamara();
                            } else {
                                requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA);
                            }
                        } else { // galería
                            String permiso = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                                    ? Manifest.permission.READ_MEDIA_IMAGES
                                    : Manifest.permission.READ_EXTERNAL_STORAGE;
                            if (ContextCompat.checkSelfPermission(this, permiso)
                                    == PackageManager.PERMISSION_GRANTED) {
                                openGallery();
                            } else {
                                requestGalleryPermissionLauncher.launch(permiso);
                            }
                        }
                    })
                    .show();
        });

        btnRegistrar.setOnClickListener(v -> {
            Log.d("RegistroDebug", "btnRegistrar onClick");
            registrarUsuario();
        });
        locationHelper = new LocationHelper(this);

        Log.d("RegistroDebug", "onCreate: finalizado");
    }

    private void openGallery() {
        galleryLauncher.launch("image/*");
    }

    private void lanzarCamara() {
        File fotosDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File foto = new File(fotosDir, "perfil_" + System.currentTimeMillis() + ".jpg");
        perfilUri = FileProvider.getUriForFile(
                this,
                getPackageName() + ".fileprovider",
                foto
        );
        Intent cam = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        cam.putExtra(MediaStore.EXTRA_OUTPUT, perfilUri);
        cameraLauncher.launch(cam);
    }

    /**
     * 1) Validar campos
     * 2) Ver si el correo existe
     * 3) Si no existe => crearUsuario
     */
    private void registrarUsuario() {
        Log.d("RegistroDebug", "registrarUsuario: entrando");

        String nombre = etNombre.getText().toString().trim();
        String apellidos = etApellidos.getText().toString().trim();
        String correo = etCorreoRegistro.getText().toString().trim();
        String contrasena = etContrasenaRegistro.getText().toString().trim();
        String repetir   = etRepetirContrasena.getText().toString().trim();

        if (!contrasena.equals(repetir)) {
            etRepetirContrasena.setError("Las contraseñas no coinciden");
            etRepetirContrasena.requestFocus();
            Toast.makeText(this, "Las contraseñas deben coincidir", Toast.LENGTH_SHORT).show();
            return;
        }


        // Validaciones
        if (nombre.isEmpty() || !ValidacionUtils.validarNombre(nombre)) {
            Log.d("RegistroDebug", "registrarUsuario: nombre inválido");
            etNombre.setError("Nombre inválido (máx 20 chars)");
            etNombre.requestFocus();
            return;
        }
        if (!ValidacionUtils.validarApellidos(apellidos)) {
            Log.d("RegistroDebug", "registrarUsuario: apellidos muy largos");
            etApellidos.setError("Apellidos muy largos (máx 30 chars)");
            etApellidos.requestFocus();
            return;
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(correo).matches()) {
            Log.d("RegistroDebug", "registrarUsuario: correo inválido");
            etCorreoRegistro.setError("Correo inválido");
            etCorreoRegistro.requestFocus();
            return;
        }
        if (!ValidacionUtils.validarContrasena(contrasena)) {
            Log.d("RegistroDebug", "registrarUsuario: contrasena inválida");
            etContrasenaRegistro.setError("Contraseña 8-18 chars");
            etContrasenaRegistro.requestFocus();
            return;
        }

        firebaseAuth.fetchSignInMethodsForEmail(correo)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        if (task.getResult() != null) {
                            if (!task.getResult().getSignInMethods().isEmpty()) {
                                Log.d("RegistroDebug", "El correo ya está registrado: " + correo);
                                Toast.makeText(this, "El correo ya está registrado", Toast.LENGTH_SHORT).show();
                            } else {
                                Log.d("RegistroDebug", "El correo NO está registrado -> crearUsuario");
                                crearUsuario(correo, contrasena, nombre, apellidos);
                            }
                        } else {
                            Log.d("RegistroDebug", "fetchSignInMethodsForEmail: getResult() es null");
                        }
                    } else {
                        Log.e("RegistroDebug", "Error en fetchSignInMethodsForEmail", task.getException());
                        Toast.makeText(this, "Error: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    /**
     * createUserWithEmailAndPassword => doc con ciudad="", luego ver permisos
     */
    private void crearUsuario(String correo,
                              String contrasena,
                              String nombre,
                              String apellidos) {

        Log.d("RegistroDebug", "crearUsuario => " + correo);

        firebaseAuth.createUserWithEmailAndPassword(correo, contrasena)
                .addOnSuccessListener(authResult -> {

                    FirebaseUser user = authResult.getUser();
                    if (user == null) {
                        Toast.makeText(this,
                                "Ocurrió un problema inesperado", Toast.LENGTH_LONG).show();
                        return;
                    }

                    String uid = user.getUid();
                    Toast.makeText(this,
                            " Usuario creado correctamente", Toast.LENGTH_SHORT).show();

                    // ── 1) Descarga e inicializa la clave común ─────────
                    CommonKeyProvider.get(new CommonKeyProvider.Callback() {
                        @Override
                        public void onReady(SecretKey key) {
                            CommonCrypto.init(key);
                            Toast.makeText(Registro.this, "Clave de cifrado preparada", Toast.LENGTH_SHORT).show();

                            // ─────────── [Añade esto] ─────────── //
                            // Generar y guardar token FCM
                            FirebaseMessaging.getInstance().getToken()
                                    .addOnSuccessListener(token -> {
                                        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
                                            GestorNotificaciones.subirTokenAFirestore(token);
                                        }
                                    })
                                    .addOnFailureListener(e ->
                                            Log.e("FCM", "Error al obtener token", e)
                                    );
                            // ── 3) Subir foto de perfil (si existe) y luego guardar todo junto ──
                            if (perfilUri != null) {
                                // Detectar extensión y MIME
                                String mime = getContentResolver().getType(perfilUri);
                                String ext  = MimeTypeMap.getSingleton()
                                        .getExtensionFromMimeType(mime);
                                if (ext == null) ext = "jpg";

                                // Referencia en Storage
                                StorageReference ref = FirebaseStorage.getInstance()
                                        .getReference("perfiles/" + uid + "." + ext);

                                // Metadatos para ContentType
                                StorageMetadata meta = new StorageMetadata.Builder()
                                        .setContentType(mime)
                                        .build();

                                // Subida y obtención de URL
                                ref.putFile(perfilUri, meta)
                                        .continueWithTask(task -> {
                                            if (!task.isSuccessful()) throw task.getException();
                                            return ref.getDownloadUrl();
                                        })
                                        .addOnSuccessListener(uriDownload -> {
                                            String fotoUrl = uriDownload.toString();

                                            // ── 4) Guardar usuario cifrado con URL real ──
                                            guardarDatosExtra(uid,
                                                    nombre, apellidos, correo, "",
                                                    fotoUrl);

                                            // ── 5) Actualizar ubicación (inline) ──
                                            if (checkSelfPermission(
                                                    Manifest.permission.ACCESS_FINE_LOCATION)
                                                    == PackageManager.PERMISSION_GRANTED) {
                                                actualizarLocalizacionSiYaConcedido();
                                            } else {
                                                locationHelper.solicitarPermisoUbicacion();
                                            }
                                        })
                                        .addOnFailureListener(e -> {
                                            Log.e("RegistroDebug","Error subiendo foto", e);

                                            // En caso de fallo, guardamos con "default"
                                            guardarDatosExtra(uid,
                                                    nombre, apellidos, correo, "",
                                                    "default");

                                            // Y actualizamos ubicación
                                            if (checkSelfPermission(
                                                    Manifest.permission.ACCESS_FINE_LOCATION)
                                                    == PackageManager.PERMISSION_GRANTED) {
                                                actualizarLocalizacionSiYaConcedido();
                                            } else {
                                                locationHelper.solicitarPermisoUbicacion();
                                            }
                                        });

                            } else {
                                // Si NO hay foto, guardamos directamente con "default"
                                guardarDatosExtra(uid,
                                        nombre, apellidos, correo, "",
                                        "default");

                                // Y actualizamos ubicación
                                if (checkSelfPermission(
                                        Manifest.permission.ACCESS_FINE_LOCATION)
                                        == PackageManager.PERMISSION_GRANTED) {
                                    actualizarLocalizacionSiYaConcedido();
                                } else {
                                    locationHelper.solicitarPermisoUbicacion();
                                }
                            }
                        }
                        @Override
                        public void onError(Exception e) {
                            Log.e("RegistroDebug","getCommonKey",e);
                            Toast.makeText(Registro.this,
                                    "Error preparando clave. Intenta más tarde",
                                    Toast.LENGTH_LONG).show();
                        }
                    });

                })
                .addOnFailureListener(e -> {
                    Log.e("RegistroDebug","createUser",e);
                    Toast.makeText(this,
                            "Error al crear usuario: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                });
    }


    /**
     * Actualiza la ciudad y lat/lon si el permiso YA estaba concedido
     */
    private void actualizarLocalizacionSiYaConcedido() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        FusedLocationProviderClient client = LocationServices.getFusedLocationProviderClient(this);

        client.getLastLocation().addOnSuccessListener(location -> {
            if (location != null) {
                double lat = location.getLatitude();
                double lon = location.getLongitude();

                // Llamar a LocationIQ si deseas ciudad
                LocationIQHelper.reverseGeocode(lat, lon,
                        cityName -> {
                            Log.d("RegistroDebug", "LocationIQ => " + cityName);

                            // ———> AÑADE ESTA LLAMADA AL PRINCIPIO:
                            actualizarSoloCiudad(userIdCreado, cityName);
                            // ———<

                            // Ahora usamos tu helper para leer, modificar y guardar cifrado
                            FirestoreHelper helper = new FirestoreHelper();
                            helper.leerUsuarioDescifrado(
                                    userIdCreado,
                                    usuario -> {
                                        if (usuario != null) {
                                            // 1) Actualiza los campos en el objeto
                                            usuario.setLocalizacion(cityName);
                                            usuario.setLat(lat);
                                            usuario.setLon(lon);
                                            // 2) Guarda TODO cifrado de nuevo
                                            helper.guardarUsuarioCifrado(
                                                    userIdCreado,
                                                    usuario,
                                                    aVoid -> Log.d("RegistroDebug", "Localización y coords guardados cifrados"),
                                                    e -> Log.e("RegistroDebug", "Error guardando cifrado: " + e.getMessage())
                                            );
                                        } else {
                                            Log.w("RegistroDebug", "Usuario no existe al actualizar localización");
                                        }
                                    },
                                    error -> Log.e("RegistroDebug", "Error leyendo usuario para cifrar: " + error.getMessage())
                            );
                        },
                        error -> {
                            Log.e("RegistroDebug", "Error locationIQ => " + error.getMessage());
                            // Si falla, asigna "Desconocido" igual que antes
                            FirestoreHelper helper = new FirestoreHelper();
                            helper.leerUsuarioDescifrado(
                                    userIdCreado,
                                    usuario -> {
                                        if (usuario != null) {
                                            usuario.setLocalizacion("Desconocido");
                                            usuario.setLat(lat);
                                            usuario.setLon(lon);
                                            helper.guardarUsuarioCifrado(
                                                    userIdCreado,
                                                    usuario,
                                                    aVoid -> Log.d("RegistroDebug", "Coords guardados, city=Desconocido (cifrado)"),
                                                    e -> Log.e("RegistroDebug", "Error guardando coords cifrados: " + e.getMessage())
                                            );
                                        }
                                    },
                                    err -> Log.e("RegistroDebug", "Error leyendo usuario al fallar LocationIQ: " + err.getMessage())
                            );
                        }
                );

            } else {
                Log.d("RegistroDebug", "location == null => no se pudo obtener lat/lon");
            }
        }).addOnFailureListener(e -> {
            Log.e("RegistroDebug", "getLastLocation => onFailure: " + e.getMessage());
        });
    }


    /**
     * onRequestPermissionsResult => si se concede => actualizamos
     */
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        locationHelper.handleRequestPermissionsResult(
                requestCode,
                permissions,
                grantResults,
                (lat, lon) -> {
                    // Éxito => LocationHelper nos dio lat y lon
                    LocationIQHelper.reverseGeocode(lat, lon,
                            cityName -> {
                                Log.d("RegistroDebug", "CityName => " + cityName);
                                if (userIdCreado != null) {
                                    // ciudad
                                    actualizarSoloCiudad(userIdCreado, cityName);

                                    // lat/lon
                                    FirebaseFirestore.getInstance()
                                            .collection("usuarios")
                                            .document(userIdCreado)
                                            .update("lat", lat, "lon", lon)
                                            .addOnSuccessListener(aVoid -> {
                                                Log.d("RegistroDebug", "Lat/Lon guardados con permisos concedidos");
                                            })
                                            .addOnFailureListener(e -> {
                                                Log.e("RegistroDebug", "Error guardando lat/lon: " + e.getMessage());
                                            });
                                }
                            },
                            ex -> {
                                Log.e("RegistroDebug", "Error al hacer reverse geocode: " + ex.getMessage());
                                if (userIdCreado != null) {
                                    // Ciudad "Desconocido"
                                    actualizarSoloCiudad(userIdCreado, "Desconocido");
                                }
                            }
                    );
                },
                ex -> {
                    Log.e("RegistroDebug", "handleRequestPermissionsResult -> onFailure: " + ex.getMessage());
                    Toast.makeText(this, "No se obtuvo la localización: " + ex.getMessage(), Toast.LENGTH_SHORT).show();
                }
        );
    }


    /**
     * Actualiza SOLO localizacion
     */
    private void actualizarSoloCiudad(String userId, String ciudad) {
        Log.d("RegistroDebug", "actualizarSoloCiudad -> userId=" + userId + ", ciudad=" + ciudad);

        FirestoreHelper helper = new FirestoreHelper();
        // Primero leemos el usuario (descifrado)
        helper.leerUsuarioDescifrado(
                userId,
                usuario -> {
                    if (usuario != null) {
                        // Actualizamos en el objeto
                        usuario.setLocalizacion(ciudad);
                        // Y guardamos todo cifrado de nuevo
                        helper.guardarUsuarioCifrado(
                                userId,
                                usuario,
                                aVoid -> {
                                    Log.d("RegistroDebug", "Ciudad actualizada con éxito a " + ciudad);
                                    Toast.makeText(Registro.this,
                                            "Ciudad actualizada a " + ciudad,
                                            Toast.LENGTH_SHORT).show();
                                    // Ir a Main si quieres
                                    startActivity(new Intent(this, MainActivity.class));
                                    finish();
                                },
                                e -> {
                                    Log.e("RegistroDebug", "Error al actualizar ciudad: " + e.getMessage());
                                    Toast.makeText(Registro.this,
                                            "Error al actualizar ciudad: " + e.getMessage(),
                                            Toast.LENGTH_SHORT).show();
                                }
                        );
                    } else {
                        // Si por alguna razón no existe el usuario
                        Log.w("RegistroDebug", "Usuario no encontrado para actualizar ciudad");
                    }
                },
                e -> {
                    Log.e("RegistroDebug", "Error leyendo usuario para actualizar ciudad: " + e.getMessage());
                    Toast.makeText(Registro.this,
                            "No se pudo leer usuario: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                }
        );
    }


    /**
     * Guardar datos básicos con ciudad vacía
     */
    private void guardarDatosExtra(String userId, String nombre, String apellidos, String correo, String ciudad, String fotoPerfil) {


        // lat/lon = null al principio
        Usuario usuario = new Usuario(
                userId, nombre, apellidos, correo, ciudad, fotoPerfil, null, null
        );

        // ← Sustituimos la llamada directa a Firestore por la versión cifrada:
        FirestoreHelper helper = new FirestoreHelper();
        helper.guardarUsuarioCifrado(
                userId,
                usuario,
                aVoid -> {
                    Toast.makeText(Registro.this, "Datos guardados correctamente", Toast.LENGTH_SHORT).show();

                    // (A) Guardar en SharedPreferences para que MainActivity pueda leerlos
                    guardarDatosEnPrefs(nombre, correo, fotoPerfil);

                    // (B) Mantienes la lógica de registroCompleto
                    if (!registroCompleto) {
                        registroCompleto = true;
                        startActivity(new Intent(this, MainActivity.class));
                        finish();
                    }
                },
                e -> {
                    Toast.makeText(Registro.this, "Error al guardar datos: " + e.getMessage(), Toast.LENGTH_SHORT).show();

                    if (!registroCompleto) {
                        registroCompleto = true;
                        startActivity(new Intent(this, MainActivity.class));
                        finish();
                    }
                }
        );

        // ——— fin de sustitución ———
    }


    private void guardarDatosEnPrefs(String nombre, String correo, String foto) {
        SharedPreferences.Editor editor = getSharedPreferences("MyAppPrefs", MODE_PRIVATE).edit();
        editor.putString("userName", nombre);
        editor.putString("userEmail", correo);
        editor.putString("userPhoto", foto);
        editor.apply();
    }

}
