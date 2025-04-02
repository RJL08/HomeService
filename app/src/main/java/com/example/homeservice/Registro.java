package com.example.homeservice;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.util.Patterns;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;


import com.example.homeservice.model.Usuario;
import com.example.homeservice.utils.LocationHelper;
import com.example.homeservice.utils.ValidacionUtils;


import com.google.firebase.auth.*;
import com.google.firebase.firestore.FirebaseFirestore;

import static android.Manifest.permission.ACCESS_FINE_LOCATION;
import static android.content.pm.PackageManager.PERMISSION_GRANTED;

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
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_registro);

        firebaseAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();

        etNombre = findViewById(R.id.etNombre);
        etApellidos = findViewById(R.id.etApellidos);
        etCorreoRegistro = findViewById(R.id.etCorreoRegistro);
        etContrasenaRegistro = findViewById(R.id.etContrasenaRegistro);
        btnRegistrar = findViewById(R.id.btnRegistrar);

        btnRegistrar.setOnClickListener(v -> registrarUsuario());

        locationHelper = new LocationHelper(this);
    }

    /**
     * 1) Validar campos (nombre, apellidos, correo, contrasena).
     * 2) Comprobar si el correo ya existe en FirebaseAuth.
     * 3) Si no existe, crear el usuario.
     */
    private void registrarUsuario() {
        String nombre = etNombre.getText().toString().trim();
        String apellidos = etApellidos.getText().toString().trim();
        String correo = etCorreoRegistro.getText().toString().trim();
        String contrasena = etContrasenaRegistro.getText().toString().trim();

        // *** Validación con tus chequeos + ValidationUtils *** //

        // Validación de nombre
        if (nombre.isEmpty() || !ValidacionUtils.validarNombre(nombre)) {
            etNombre.setError("Nombre inválido (máx 20 chars)");
            etNombre.requestFocus();
            return;
        }

        // Validación de apellidos (opcional, sólo si te interesa un máximo):
        if (!ValidacionUtils.validarApellidos(apellidos)) {
            etApellidos.setError("Apellidos muy largos (máx 30 chars)");
            etApellidos.requestFocus();
            return;
        }

        // Tu chequeo de correo con Patterns
        if (!Patterns.EMAIL_ADDRESS.matcher(correo).matches()) {
            etCorreoRegistro.setError("Correo inválido");
            etCorreoRegistro.requestFocus();
            return;
        }
        // Contraseña: mínimo 8 (tú) y máximo 12 (ValidationUtils)
        if (!ValidacionUtils.validarContrasena(contrasena)) {
            etContrasenaRegistro.setError("Contraseña 8-12 chars");
            etContrasenaRegistro.requestFocus();
            return;
        }

        // *** Comprobar si existe en FirebaseAuth ***
        firebaseAuth.fetchSignInMethodsForEmail(correo)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        if (task.getResult() != null) {
                            if (!task.getResult().getSignInMethods().isEmpty()) {
                                Toast.makeText(this, "El correo ya está registrado", Toast.LENGTH_SHORT).show();
                            } else {
                                // Todo OK -> crear usuario
                                crearUsuario(correo, contrasena, nombre, apellidos);
                            }
                        }
                    } else {
                        Toast.makeText(this, "Error: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    /**
     * Crear el usuario en FirebaseAuth. Si se crea con éxito:
     * - Obtenemos su UID
     * - Revisamos si el permiso de localización YA está concedido
     *   -> Si está concedido => guardamos datos + ciudad
     *   -> Si no está => solicitamos el permiso => onRequestPermissionsResult
     */
    private void crearUsuario(String correo, String contrasena, String nombre, String apellidos) {
        firebaseAuth.createUserWithEmailAndPassword(correo, contrasena)
                .addOnSuccessListener(authResult -> {
                    Toast.makeText(this, "Usuario creado con éxito", Toast.LENGTH_SHORT).show();
                    FirebaseUser user = firebaseAuth.getCurrentUser();
                    if (user != null) {
                        userIdCreado = user.getUid();
                    }

                    // *** Si YA tenemos el permiso, no dependemos de onRequestPermissionsResult ***
                    if (checkSelfPermission(ACCESS_FINE_LOCATION) == PERMISSION_GRANTED) {
                        // Obtenemos la localización directo y luego guardamos
                        obtenerLocalizacionYGuardar(nombre, apellidos, correo);
                    } else {
                        // Si no está concedido, lo pedimos -> se maneja en onRequestPermissionsResult
                        locationHelper.solicitarPermisoUbicacion();
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Error al crear usuario: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
    }

    /**
     * Obtiene la localización directamente y guarda en Firestore (sin onRequestPermissionsResult).
     * Sirve cuando YA está el permiso.
     */
    private void obtenerLocalizacionYGuardar(String nombre, String apellidos, String correo) {
        locationHelper.handleRequestPermissionsResult(
                LocationHelper.REQUEST_CODE_LOCATION,
                new String[]{ACCESS_FINE_LOCATION},
                new int[]{PERMISSION_GRANTED}, // simulamos que se concedió
                ciudad -> {
                    if (userIdCreado != null) {
                        guardarDatosExtra(
                                userIdCreado,
                                nombre,
                                apellidos,
                                correo,
                                ciudad
                        );
                    }
                },
                e -> {
                    Toast.makeText(this, "No se obtuvo la localización: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    if (userIdCreado != null) {
                        guardarDatosExtra(
                                userIdCreado,
                                nombre,
                                apellidos,
                                correo,
                                ""
                        );
                    }
                }
        );
    }

    /**
     * onRequestPermissionsResult se llama SOLO si no teníamos el permiso antes.
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
                ciudad -> {
                    if (userIdCreado != null) {
                        guardarDatosExtra(
                                userIdCreado,
                                etNombre.getText().toString().trim(),
                                etApellidos.getText().toString().trim(),
                                etCorreoRegistro.getText().toString().trim(),
                                ciudad
                        );
                    }
                },
                e -> {
                    Toast.makeText(this, "No se obtuvo la localización: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    if (userIdCreado != null) {
                        guardarDatosExtra(
                                userIdCreado,
                                etNombre.getText().toString().trim(),
                                etApellidos.getText().toString().trim(),
                                etCorreoRegistro.getText().toString().trim(),
                                ""
                        );
                    }
                }
        );
    }

    private void guardarDatosExtra(String userId, String nombre, String apellidos, String correo, String ciudad) {
        String fotoPerfil = "default";

        Usuario usuario = new Usuario(
                userId,
                nombre,
                apellidos,
                correo,
                ciudad,
                fotoPerfil
        );

        FirebaseFirestore.getInstance().collection("usuarios")
                .document(userId)
                .set(usuario)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(Registro.this, "Datos guardados correctamente", Toast.LENGTH_SHORT).show();
                    if (!registroCompleto) {
                        registroCompleto = true;
                        // Ir a MainActivity
                        startActivity(new Intent(this, MainActivity.class));
                        finish();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(Registro.this, "Error al guardar datos: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    if (!registroCompleto) {
                        registroCompleto = true;
                        // Ir a MainActivity aunque falle
                        startActivity(new Intent(this, MainActivity.class));
                        finish();
                    }
                });
    }
}
