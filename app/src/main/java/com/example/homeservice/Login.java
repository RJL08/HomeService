package com.example.homeservice;



import static androidx.core.content.ContextCompat.startActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.gms.auth.api.signin.*;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.*;
@SuppressWarnings("deprecation")
    public class Login extends AppCompatActivity {

        private EditText etCorreo, etContrasena;
        private Button btnLoginCorreo;
        private com.google.android.gms.common.SignInButton btnLoginGoogle;
        private TextView tvRegistrar;

        private FirebaseAuth firebaseAuth;

        private static final int RC_SIGN_IN = 1;
        private GoogleSignInClient googleSignInClient;

        @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setContentView(R.layout.activity_login);

            // Inicialización de Firebase
            firebaseAuth = FirebaseAuth.getInstance();

            // Referencias a los elementos de la interfaz
            etCorreo = findViewById(R.id.etCorreo);
            etContrasena = findViewById(R.id.etContrasena);
            btnLoginCorreo = findViewById(R.id.btnLoginCorreo);
            btnLoginGoogle = findViewById(R.id.btnLoginGoogle);
            tvRegistrar = findViewById(R.id.tvRegistrar);
            SignInButton btnLoginGoogle = findViewById(R.id.btnLoginGoogle);
            btnLoginGoogle.setSize(SignInButton.SIZE_WIDE); // Tamaño más grande
            btnLoginGoogle.setColorScheme(SignInButton.COLOR_LIGHT);
            setGoogleSignInButtonText(btnLoginGoogle, "Signin with in Google");

            // Configuración de Google Sign-In
            GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                    .requestIdToken(getString(R.string.default_web_client_id)) // Coloca tu client ID aquí
                    .requestEmail()
                    .build();

            googleSignInClient = GoogleSignIn.getClient(this, gso);

            btnLoginCorreo.setOnClickListener(v -> loginConCorreo());
            btnLoginGoogle.setOnClickListener(v -> iniciarSesionGoogle());
            tvRegistrar.setOnClickListener(v -> startActivity(new Intent(this, Registro.class))); // Activity de registro si tienes una
        }

    /**
     * Inicia sesión con correo y contraseña usando Firebase.
     */
    private void loginConCorreo() {
        String correo = etCorreo.getText().toString().trim();
        String contrasena = etContrasena.getText().toString().trim();

        // Validaciones de formato y longitud (email y contraseña)
        if (!Patterns.EMAIL_ADDRESS.matcher(correo).matches()) {
            etCorreo.setError("Formato de correo inválido");
            etCorreo.requestFocus();
            return;
        }
        if (contrasena.length() < 8) {
            etContrasena.setError("La contraseña debe tener al menos 8 caracteres");
            etContrasena.requestFocus();
            return;
        }

        // Antes de signIn, consultamos si existe en Auth
        firebaseAuth.fetchSignInMethodsForEmail(correo)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        // Lista de métodos asociados a este correo
                        if (task.getResult() != null) {
                            // Si la lista está vacía, NO existe
                            if (task.getResult().getSignInMethods().isEmpty()) {
                                Toast.makeText(this, "Este correo no está registrado", Toast.LENGTH_SHORT).show();
                            } else {
                                // Sí existe -> signIn
                                signInConCorreo(correo, contrasena);
                            }
                        }
                    } else {
                        // Error al consultar
                        Toast.makeText(this, "Error: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    /**
     * signIn final (ya sabemos que el correo existe)
     */
    private void signInConCorreo(String correo, String contrasena) {
        firebaseAuth.signInWithEmailAndPassword(correo, contrasena)
                .addOnSuccessListener(authResult -> {
                    Toast.makeText(this, "Inicio de sesión exitoso", Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(this, MainActivity.class));
                    finish();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
    }



    /**
         * Lanza el intent para iniciar sesión con Google.
         */
        private void iniciarSesionGoogle() {
            Intent signInIntent = googleSignInClient.getSignInIntent();
            startActivityForResult(signInIntent, RC_SIGN_IN);
        }

        /**
         * Recoge el resultado del intento de iniciar sesión con Google.
         */
        @Override
        protected void onActivityResult(int requestCode, int resultCode, Intent data) {
            super.onActivityResult(requestCode, resultCode, data);

            if (requestCode == RC_SIGN_IN) {
                Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
                try {
                    GoogleSignInAccount cuenta = task.getResult(ApiException.class);
                    autenticarConFirebase(cuenta);
                } catch (ApiException e) {
                    Toast.makeText(this, "Fallo en Google Sign-In: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
        }

        /**
         * Usa el token de Google para autenticarse con Firebase.
         */
        private void autenticarConFirebase(GoogleSignInAccount cuenta) {
            AuthCredential credential = GoogleAuthProvider.getCredential(cuenta.getIdToken(), null);
            firebaseAuth.signInWithCredential(credential)
                    .addOnSuccessListener(authResult -> {
                        Toast.makeText(this, "Inicio de sesión con Google exitoso", Toast.LENGTH_SHORT).show();
                        startActivity(new Intent(this, MainActivity.class));
                        finish();
                    })
                    .addOnFailureListener(e -> Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
        }

        /**
         * Cambiamos el texto predeterminado del botón de Google Sign-In.
         * De esta manera personalizamos el texto del botón sin afectar su estilo nativo.
         *
         * @param signInButton botón de Google Sign-In
         * @param buttonText texto que queremos mostrar en el botón
         */
        private void setGoogleSignInButtonText(SignInButton signInButton, String buttonText) {
            for (int i = 0; i < signInButton.getChildCount(); i++) {
                if (signInButton.getChildAt(i) instanceof TextView) {
                    ((TextView) signInButton.getChildAt(i)).setText(buttonText);
                    return;
                }
            }
        }

    }

