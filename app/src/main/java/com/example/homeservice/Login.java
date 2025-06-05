package com.example.homeservice;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Address;
import android.location.Geocoder;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.WindowCompat;

import android.Manifest;
import com.example.homeservice.database.FirestoreHelper;
import com.example.homeservice.model.Usuario;
import com.example.homeservice.notificaciones.GestorNotificaciones;
import com.example.homeservice.seguridad.CommonCrypto;
import com.example.homeservice.seguridad.CommonKeyProvider;
import com.example.homeservice.utils.KeystoreManager;
import com.example.homeservice.utils.LocationHelper;
import com.example.homeservice.utils.LocationIQHelper;
import com.example.homeservice.utils.ValidacionUtils;
import com.google.android.gms.auth.api.signin.*;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.location.*;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.*;
import com.google.firebase.messaging.FirebaseMessaging;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

import javax.crypto.SecretKey;

@SuppressWarnings("deprecation")


/**
 * clase en la que se encuentra el login de usuario con correo y contraseña o con google.
 * ademas
 */
public class Login extends AppCompatActivity {

    private EditText etCorreo, etContrasena;
    private Button btnLoginCorreo;
    private SignInButton btnLoginGoogle;
    private TextView tvRegistrar;

    private FirebaseAuth firebaseAuth;
    private static final int RC_SIGN_IN = 1;
    private GoogleSignInClient googleSignInClient;
    private LocationHelper locationHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d("LoginDebug", "onCreate: Iniciando Login activity");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            // Importa androidx.core.content.ContextCompat
            int semi = ContextCompat.getColor(this, R.color.logo_background);
            getWindow().setStatusBarColor(semi);
        }

        // Inicialización de Firebase
        firebaseAuth = FirebaseAuth.getInstance();
        Log.d("LoginDebug", "FirebaseAuth instance obtained");

        // Referencias UI
        etCorreo = findViewById(R.id.etCorreo);
        etContrasena = findViewById(R.id.etContrasena);
        btnLoginCorreo = findViewById(R.id.btnLoginCorreo);
        btnLoginGoogle = findViewById(R.id.btnLoginGoogle);
        tvRegistrar = findViewById(R.id.tvRegistrar);
        // Botón Olvide contraseña (redirecciona a ResetPasswordActivity)
        findViewById(R.id.tvOlvidePassword).setOnClickListener(v ->
                startActivity(new Intent(this, ResetPasswordActivity.class)));


        // Botón Google
        btnLoginGoogle.setSize(SignInButton.SIZE_WIDE);
        btnLoginGoogle.setColorScheme(SignInButton.COLOR_LIGHT);
        setGoogleSignInButtonText(btnLoginGoogle, "Signin with in Google");
        Log.d("LoginDebug", "Google SignInButton configurado");

        // Configuración de Google Sign-In
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        googleSignInClient = GoogleSignIn.getClient(this, gso);
        Log.d("LoginDebug", "GoogleSignInClient obtenido");

        // Listeners
        btnLoginCorreo.setOnClickListener(v -> {
            Log.d("LoginDebug", "btnLoginCorreo onClick");
            loginConCorreo();
        });
        btnLoginGoogle.setOnClickListener(v -> {
            Log.d("LoginDebug", "btnLoginGoogle onClick");
            iniciarSesionGoogle();
        });
        tvRegistrar.setOnClickListener(v -> {
            Log.d("LoginDebug", "tvRegistrar onClick -> Registro");
            startActivity(new Intent(this, Registro.class));
        });

        locationHelper = new LocationHelper(this);
        Log.d("LoginDebug", "onCreate: finalizado");
    }

    /**
     * LOGIN con correo + pass:
     * 1) Verifica si el correo existe en FirebaseAuth (fetchSignInMethodsForEmail).
     * 2) Si no existe => Toast
     * 3) Si existe => signInConCorreo
     */
    private void loginConCorreo() {
        String correo      = etCorreo.getText().toString().trim();
        String contrasena  = etContrasena.getText().toString().trim();

        if (!ValidacionUtils.validarCorreo(correo)) {
            etCorreo.setError("Correo inválido"); etCorreo.requestFocus(); return;
        }
        if (!ValidacionUtils.validarContrasena(contrasena)) {
            etContrasena.setError("Contraseña inválida"); etContrasena.requestFocus(); return;
        }

        firebaseAuth.signInWithEmailAndPassword(correo, contrasena)
                .addOnSuccessListener(authResult -> {

                    /* ── 1) Descarga la clave común ─────────────────────────── */
                    CommonKeyProvider.get(new CommonKeyProvider.Callback() {
                        @Override public void onReady(SecretKey key) {
                            CommonCrypto.init(key);
                            // Generar y guardar token FCM
                            FirebaseMessaging.getInstance().getToken()
                                    .addOnSuccessListener(token -> {
                                        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
                                            GestorNotificaciones.subirTokenAFirestore(token);
                                        }
                                    })
                                    .addOnFailureListener(e ->
                                            Log.e("FCM", "Error al obtener token", e)
                                    );// AES lista
                            continuarConUsuario( authResult.getUser().getUid() );
                        }
                        @Override public void onError(Exception e) {
                            Log.e("Login","commonKey",e);
                            Toast.makeText(Login.this,
                                    "Error al obtener clave. Intenta más tarde",
                                    Toast.LENGTH_LONG).show();
                        }
                    });

                })
                .addOnFailureListener(e -> {
                    String msg = "Error al iniciar sesión";
                    if (e instanceof FirebaseAuthInvalidUserException)       msg = "Correo no registrado";
                    else if (e instanceof FirebaseAuthInvalidCredentialsException) msg = "Contraseña incorrecta";
                    Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
                });
    }



    /**
     * Login con Google
     */
    private void iniciarSesionGoogle() {
        Log.d("LoginDebug", "iniciarSesionGoogle");
        Intent signInIntent = googleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    /**
     * onActivityResult para Google
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.d("LoginDebug", "onActivityResult: requestCode=" + requestCode + ", resultCode=" + resultCode);
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                GoogleSignInAccount cuenta = task.getResult(ApiException.class);
                Log.d("LoginDebug", "GoogleSignInAccount obtenido con éxito. autenticarConFirebase()");
                autenticarConFirebase(cuenta);
            } catch (ApiException e) {
                Log.e("LoginDebug", "Fallo en Google Sign-In: " + e.getMessage());
                Toast.makeText(this, "Fallo en Google Sign-In: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }
    }

    /**
     * Auth con Google en Firebase
     */
    private void autenticarConFirebase(GoogleSignInAccount cuenta) {
        AuthCredential credential = GoogleAuthProvider.getCredential(cuenta.getIdToken(), null);

        firebaseAuth.signInWithCredential(credential)
                .addOnSuccessListener(authResult -> {
                    String uid = authResult.getUser().getUid();

                    CommonKeyProvider.get(new CommonKeyProvider.Callback() {
                        @Override public void onReady(SecretKey key) {
                            CommonCrypto.init(key);
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
                            continuarConUsuario( authResult.getUser().getUid() );
                        }
                        @Override public void onError(Exception e) {
                            Log.e("Login","commonKey",e);
                            Toast.makeText(Login.this,
                                    "Error al obtener clave. Intenta más tarde",
                                    Toast.LENGTH_LONG).show();
                        }
                    });

                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    /**
     * Continuar con usuario descifrado y guardar datos en SharedPreferences
     * @param uid
     */
    private void continuarConUsuario(String uid) {

        FirestoreHelper firestoreHelper = new FirestoreHelper();

        firestoreHelper.leerUsuarioDescifrado(
                uid,
                usuario -> {
                    if (usuario != null) {
                        guardarDatosEnPrefs(
                                usuario.getNombre() ,
                                usuario.getCorreo(),
                                usuario.getFotoPerfil());
                    } else {
                        // Crear perfil por defecto si no existiera
                        Usuario nuevo = new Usuario(
                                uid, "NombreDesconocido", "ApellidosDesconocidos",
                                firebaseAuth.getCurrentUser().getEmail(),
                                "", "fotoPerfilDefault", 0.0, 0.0);
                        firestoreHelper.guardarUsuarioCifrado(uid, nuevo,
                                a -> {}, e -> Log.e("Login","Crear por defecto: "+e));
                        guardarDatosEnPrefs("NombreDesconocido",
                                firebaseAuth.getCurrentUser().getEmail(),
                                "fotoPerfilDefault");
                    }

                    Toast.makeText(Login.this,
                            "Inicio de sesión exitoso",  // ← mensaje
                            Toast.LENGTH_LONG).show();
                    // Lanzar MainActivity
                    startActivity(new Intent(this, MainActivity.class));
                    finish();
                },
                error -> {
                    Log.e("Login","Error leer usuario: "+error.getMessage());
                    startActivity(new Intent(this, MainActivity.class));
                    finish();
                });
    }



    /**
     * Guardar datos en SharedPreferences
     */
    private void guardarDatosEnPrefs(String nombre, String correo, String foto) {
        Log.d("LoginDebug", "guardarDatosEnPrefs -> nombre=" + nombre + ", correo=" + correo + ", foto=" + foto);
        SharedPreferences.Editor editor = getSharedPreferences("MyAppPrefs", MODE_PRIVATE).edit();
        editor.putString("userName", nombre);
        editor.putString("userEmail", correo);
        editor.putString("userPhoto", foto);
        editor.apply();
    }

    private void setGoogleSignInButtonText(SignInButton signInButton, String buttonText) {
        for (int i = 0; i < signInButton.getChildCount(); i++) {
            if (signInButton.getChildAt(i) instanceof TextView) {
                ((TextView) signInButton.getChildAt(i)).setText(buttonText);
                return;
            }
        }
    }

    @Override
    protected void onStart() {
        super.onStart();

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {                 // sesión todavía válida
            Intent i = new Intent(this, MainActivity.class);
            i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP
                    | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(i);
            finish();                       // no volvemos a Login
        }
    }


}
