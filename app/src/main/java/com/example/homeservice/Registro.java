package com.example.homeservice;

import static android.content.pm.PackageManager.PERMISSION_GRANTED;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.util.Log;
import android.util.Patterns;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.example.homeservice.model.Usuario;
import com.example.homeservice.utils.LocationHelper;
import com.example.homeservice.utils.LocationIQHelper;
import com.example.homeservice.utils.ValidacionUtils;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.firebase.auth.*;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.List;

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d("RegistroDebug", "onCreate: Iniciando Registro activity");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_registro);

        firebaseAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();

        etNombre = findViewById(R.id.etNombre);
        etApellidos = findViewById(R.id.etApellidos);
        etCorreoRegistro = findViewById(R.id.etCorreoRegistro);
        etContrasenaRegistro = findViewById(R.id.etContrasenaRegistro);
        btnRegistrar = findViewById(R.id.btnRegistrar);

        btnRegistrar.setOnClickListener(v -> {
            Log.d("RegistroDebug", "btnRegistrar onClick");
            registrarUsuario();
        });
        locationHelper = new LocationHelper(this);

        Log.d("RegistroDebug", "onCreate: finalizado");
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

        Log.d("RegistroDebug", "Datos recogidos -> Nombre: " + nombre
                + ", Apellidos: " + apellidos
                + ", Correo: " + correo
                + ", Contrasena: " + contrasena);

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
    private void crearUsuario(String correo, String contrasena, String nombre, String apellidos) {
        Log.d("RegistroDebug", "crearUsuario: email=" + correo + ", pass=" + contrasena
                + ", nombre=" + nombre + ", apellidos=" + apellidos);

        firebaseAuth.createUserWithEmailAndPassword(correo, contrasena)
                .addOnSuccessListener(authResult -> {
                    Log.d("RegistroDebug", "Usuario creado con éxito en Auth. UID="
                            + authResult.getUser().getUid());
                    Toast.makeText(this, "Usuario creado con éxito", Toast.LENGTH_SHORT).show();

                    FirebaseUser user = firebaseAuth.getCurrentUser();
                    if (user != null) {
                        userIdCreado = user.getUid();
                        Log.d("RegistroDebug", "userIdCreado=" + userIdCreado);

                        // Guardar con ciudad = "" y lat/lon = null
                        guardarDatosExtra(userIdCreado, nombre, apellidos, correo, "");

                        // Si permiso YA => actualizamos localización
                        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                            Log.d("RegistroDebug", "Permiso YA concedido, actualizarLocalizacionSiYaConcedido()");
                            actualizarLocalizacionSiYaConcedido();
                        } else {
                            Log.d("RegistroDebug", "Permiso NO concedido, solicitamos permisoUbicacion()");
                            locationHelper.solicitarPermisoUbicacion();
                        }
                    } else {
                        Log.w("RegistroDebug", "Usuario es null después de createUser, esto es raro");
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("RegistroDebug", "Fallo al crear usuario: " + e.getMessage());
                    Toast.makeText(this, "Error al crear usuario: " + e.getMessage(), Toast.LENGTH_SHORT).show();
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
                            // 1) Guardar la ciudad
                            actualizarSoloCiudad(userIdCreado, cityName);

                            // 2) Guardar lat/lon
                            firestore.collection("usuarios")
                                    .document(userIdCreado)
                                    .update("lat", lat, "lon", lon)
                                    .addOnSuccessListener(aVoid -> {
                                        Log.d("RegistroDebug", "Lat/Lon guardados en Firestore (permiso YA concedido)");
                                    })
                                    .addOnFailureListener(e -> {
                                        Log.e("RegistroDebug", "Error guardando lat/lon: " + e.getMessage());
                                    });
                        },
                        error -> {
                            Log.e("RegistroDebug", "Error locationIQ => " + error.getMessage());
                            // Si falla, asigna "Desconocido" pero igual guarda lat/lon
                            actualizarSoloCiudad(userIdCreado, "Desconocido");

                            firestore.collection("usuarios")
                                    .document(userIdCreado)
                                    .update("lat", lat, "lon", lon)
                                    .addOnSuccessListener(aVoid -> {
                                        Log.d("RegistroDebug", "Lat/Lon guardados, city=Desconocido (permiso YA)");
                                    });
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

        FirebaseFirestore.getInstance()
                .collection("usuarios")
                .document(userId)
                .update("localizacion", ciudad)
                .addOnSuccessListener(aVoid -> {
                    Log.d("RegistroDebug", "Ciudad actualizada con éxito a " + ciudad);
                    Toast.makeText(Registro.this, "Ciudad actualizada a " + ciudad, Toast.LENGTH_SHORT).show();
                    // Ir a Main si quieres
                    startActivity(new Intent(this, MainActivity.class));
                    finish();
                })
                .addOnFailureListener(e -> {
                    Log.e("RegistroDebug", "Error al actualizar ciudad: " + e.getMessage());
                    Toast.makeText(Registro.this, "Error al actualizar ciudad: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    /**
     * Guardar datos básicos con ciudad vacía
     */
    private void guardarDatosExtra(String userId, String nombre, String apellidos, String correo, String ciudad) {

        String fotoPerfil = "default";
        // lat/lon = null al principio
        Usuario usuario = new Usuario(
                userId, nombre, apellidos, correo, ciudad, fotoPerfil, null, null
        );

        FirebaseFirestore.getInstance().collection("usuarios")
                .document(userId)
                .set(usuario)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(Registro.this, "Datos guardados correctamente", Toast.LENGTH_SHORT).show();

                    // (A) Guardar en SharedPreferences para que MainActivity pueda leerlos
                    guardarDatosEnPrefs(
                            nombre, // userName
                            correo, // userEmail
                            fotoPerfil // userPhoto (ahora "default")
                    );

                    // (B) Mantienes la lógica de registroCompleto
                    if (!registroCompleto) {
                        registroCompleto = true;
                        startActivity(new Intent(this, MainActivity.class));
                        finish();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(Registro.this, "Error al guardar datos: " + e.getMessage(), Toast.LENGTH_SHORT).show();

                    if (!registroCompleto) {
                        registroCompleto = true;
                        startActivity(new Intent(this, MainActivity.class));
                        finish();
                    }
                });
    }


    private void guardarDatosEnPrefs(String nombre, String correo, String foto) {
        SharedPreferences.Editor editor = getSharedPreferences("MyAppPrefs", MODE_PRIVATE).edit();
        editor.putString("userName", nombre);
        editor.putString("userEmail", correo);
        editor.putString("userPhoto", foto);
        editor.apply();
    }

}
