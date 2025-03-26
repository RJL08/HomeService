package com.example.homeservice;



import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

/**
 * Actividad para registrar nuevos usuarios en Firebase Authentication
 * y guardar datos extra (nombre, apellidos) en Firestore.
 */
public class Registro extends AppCompatActivity {

    // Vistas
    private EditText etNombre, etApellidos;
    private EditText etCorreoRegistro, etContrasenaRegistro;
    private Button btnRegistrar;

    // Firebase
    private FirebaseAuth firebaseAuth;
    private FirebaseFirestore firestore; // Para almacenar datos extra

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_registro);

        // Inicializar Firebase
        firebaseAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();

        // Referenciar vistas
        etNombre = findViewById(R.id.etNombre);
        etApellidos = findViewById(R.id.etApellidos);
        etCorreoRegistro = findViewById(R.id.etCorreoRegistro);
        etContrasenaRegistro = findViewById(R.id.etContrasenaRegistro);
        btnRegistrar = findViewById(R.id.btnRegistrar);

        // Botón “Registrar”
        btnRegistrar.setOnClickListener(v -> registrarUsuario());
    }

    /**
     * Registra un nuevo usuario con correo y contraseña en Firebase Auth
     * y almacena sus datos (nombre, apellidos) en Firestore.
     */
    private void registrarUsuario() {
        String nombre = etNombre.getText().toString().trim();
        String apellidos = etApellidos.getText().toString().trim();
        String correo = etCorreoRegistro.getText().toString().trim();
        String contrasena = etContrasenaRegistro.getText().toString().trim();

        // Validar que no estén vacíos, formato correo, etc.
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

        // Verificar si existe
        firebaseAuth.fetchSignInMethodsForEmail(correo)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        if (task.getResult() != null) {
                            // Lista de métodos
                            if (!task.getResult().getSignInMethods().isEmpty()) {
                                // Si NO está vacía, el correo ya está registrado
                                Toast.makeText(this, "El correo ya está registrado", Toast.LENGTH_SHORT).show();
                            } else {
                                // Si está vacío -> NO existe -> procedemos a crear
                                crearUsuario(correo, contrasena, nombre, apellidos);
                            }
                        }
                    } else {
                        Toast.makeText(this, "Error: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    /**
     * Crear el usuario en FirebaseAuth y guardar datos extra
     */
    private void crearUsuario(String correo, String contrasena, String nombre, String apellidos) {
        firebaseAuth.createUserWithEmailAndPassword(correo, contrasena)
                .addOnSuccessListener(authResult -> {
                    Toast.makeText(this, "Usuario creado con éxito", Toast.LENGTH_SHORT).show();
                    FirebaseUser user = firebaseAuth.getCurrentUser();
                    if (user != null) {
                        String userId = user.getUid();
                        // guardarDatosExtra(userId, nombre, apellidos, correo);
                    }
                    startActivity(new Intent(this, MainActivity.class));
                    finish();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Error al crear usuario: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
    }


    /**
     * Guarda nombre, apellidos y correo en Firestore usando el userId

    private void guardarDatosExtra(String userId, String nombre, String apellidos, String correo) {
        // Estructura de datos
        Usuario usuario = new Usuario(nombre, apellidos, correo);

        firestore.collection("usuarios")
                .document(userId)
                .set(usuario)
                .addOnSuccessListener(aVoid ->
                        Toast.makeText(RegistroActivity.this, "Datos adicionales guardados", Toast.LENGTH_SHORT).show()
                )
                .addOnFailureListener(e ->
                        Toast.makeText(RegistroActivity.this, "Error al guardar datos: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
    }*/
}

