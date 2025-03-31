package com.example.homeservice;



import android.content.Intent;
import android.os.Bundle;

import android.util.Patterns;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.homeservice.model.Usuario;
import com.example.homeservice.utils.LocationHelper;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

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

    private void registrarUsuario() {
        String nombre = etNombre.getText().toString().trim();
        String apellidos = etApellidos.getText().toString().trim();
        String correo = etCorreoRegistro.getText().toString().trim();
        String contrasena = etContrasenaRegistro.getText().toString().trim();

        if (!Patterns.EMAIL_ADDRESS.matcher(correo).matches()) {
            etCorreoRegistro.setError("Correo inválido");
            etCorreoRegistro.requestFocus();
            return;
        }
        if (contrasena.length() < 8) {
            etContrasenaRegistro.setError("Contraseña mínimo 8 caracteres");
            etContrasenaRegistro.requestFocus();
            return;
        }

        firebaseAuth.fetchSignInMethodsForEmail(correo)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        if (task.getResult() != null) {
                            if (!task.getResult().getSignInMethods().isEmpty()) {
                                Toast.makeText(this, "El correo ya está registrado", Toast.LENGTH_SHORT).show();
                            } else {
                                crearUsuario(correo, contrasena, nombre, apellidos);
                            }
                        }
                    } else {
                        Toast.makeText(this, "Error: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void crearUsuario(String correo, String contrasena, String nombre, String apellidos) {
        firebaseAuth.createUserWithEmailAndPassword(correo, contrasena)
                .addOnSuccessListener(authResult -> {
                    Toast.makeText(this, "Usuario creado con éxito", Toast.LENGTH_SHORT).show();
                    FirebaseUser user = firebaseAuth.getCurrentUser();
                    if (user != null) {
                        userIdCreado = user.getUid();
                    }
                    locationHelper.solicitarPermisoUbicacion();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Error al crear usuario: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
    }

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
        String telefono = "No especificado";

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
}

