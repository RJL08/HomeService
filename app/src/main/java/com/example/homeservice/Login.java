package com.example.homeservice;

import static androidx.core.content.ContextCompat.startActivity;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.util.Patterns;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import android.Manifest;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.homeservice.database.FirestoreHelper;
import com.example.homeservice.model.Usuario;
import com.example.homeservice.utils.LocationHelper;
import com.google.android.gms.auth.api.signin.*;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.*;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

@SuppressWarnings("deprecation")


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

        locationHelper = new LocationHelper(this);

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
            etContrasena.setError("La contraseña debe tener entre 8 y 12 caracteres");
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

                    // 1) Obtenemos el usuario de Firebase
                    FirebaseUser user = authResult.getUser();
                    if (user != null) {
                        String userId = user.getUid();

                        // 2) Creamos un objeto Usuario con valores por defecto (o los que quieras)
                        //    En un proyecto real, podrías obtener datos del usuario en otra pantalla,
                        //    o leerlos de la base de datos si ya existen.
                        Usuario usuario = new Usuario(
                                userId,
                                "NombreDesconocido",       // O "SinNombre"
                                "ApellidosDesconocidos",   // O "SinApellidos"
                                user.getEmail(),           // Tomamos correo real
                                "",                        // Ciudad vacía (porque aún no hay localización)
                                "fotoPerfilDefault"        // Valor por defecto
                        );

                        // 3) Guardamos/actualizamos en Firestore
                        FirestoreHelper firestoreHelper = new FirestoreHelper();
                        firestoreHelper.guardarUsuario(
                                userId,
                                usuario,
                                aVoid -> {
                                    // Éxito guardando en Firestore
                                    // (No hace falta hacer nada concreto aquí)
                                },
                                e -> {
                                    // Error al guardar
                                    Toast.makeText(this, "Error al guardar usuario: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                }
                        );
                    }

                    // 4) Pedir permiso de ubicación (opcional):
                    //    Si el usuario concede, lo recogemos en onRequestPermissionsResult
                    locationHelper.solicitarPermisoUbicacion();

                    // 5) Por último, navegamos a la pantalla principal
                    startActivity(new Intent(this, MainActivity.class));
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    /**
     * Lanza el intent para iniciar sesión con Google.
     */
    private void iniciarSesionGoogle() {


        Intent signInIntent = googleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, RC_SIGN_IN);
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
                    FirebaseUser user = firebaseAuth.getCurrentUser();
                    if (user != null) {
                        String userId = user.getUid();

                        FirestoreHelper firestoreHelper = new FirestoreHelper();
                        firestoreHelper.leerUsuario(
                                userId,
                                usuarioLeido -> {
                                    if (usuarioLeido != null) {
                                        usuarioLeido.setLocalizacion(ciudad);
                                        firestoreHelper.guardarUsuario(
                                                userId,
                                                usuarioLeido,
                                                aVoid -> {
                                                    Log.d("Login", "Localización actualizada: " + ciudad);

                                                    // ✅ Aquí sí te lleva a MainActivity
                                                    startActivity(new Intent(this, MainActivity.class));
                                                    finish();
                                                },
                                                error -> Log.e("Login", "Error al guardar localización: " + error.getMessage())
                                        );
                                    }
                                },
                                error -> Log.e("Login", "Error al leer usuario: " + error.getMessage())
                        );
                    }
                },
                e -> {
                    Log.e("Login", "Error al obtener localización: " + e.getMessage());

                    // Incluso si no se obtiene ciudad, puedes seguir
                    startActivity(new Intent(this, MainActivity.class));
                    finish();
                }
        );
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

                    FirebaseUser user = firebaseAuth.getCurrentUser();
                    if (user != null) {
                        String userId = user.getUid();

                        // 1) Guardamos ya el usuario (sin ciudad)
                        String nombre = cuenta.getGivenName() != null ? cuenta.getGivenName() : "SinNombre";
                        String apellidos = cuenta.getFamilyName() != null ? cuenta.getFamilyName() : "SinApellidos";
                        String correo = cuenta.getEmail();
                        String foto = (cuenta.getPhotoUrl() != null) ? cuenta.getPhotoUrl().toString() : "fotoPerfilDefault";

                        Usuario usuario = new Usuario(userId, nombre, apellidos, correo, "", foto);

                        FirestoreHelper firestoreHelper = new FirestoreHelper();
                        firestoreHelper.guardarUsuario(
                                userId,
                                usuario,
                                aVoid -> Log.d("Login", "Usuario guardado sin ciudad"),
                                error -> Log.e("Login", "Error al guardar usuario: " + error.getMessage())
                        );

                        // 2) Comprobar si tenemos permiso de ubicación
                        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                                == PackageManager.PERMISSION_GRANTED) {
                            // Ya concedido => obtener localización directamente
                            obtenerLocalizacionYActualizar(userId);
                        } else {
                            // No está concedido => pedirlo => onRequestPermissionsResult se encargará
                            locationHelper.solicitarPermisoUbicacion();
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void obtenerLocalizacionYActualizar( String userId) {
        // Versión resumida. Podrías llamar locationHelper.obtenerLocalizacionInterna(...) para no duplicar
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {

            FusedLocationProviderClient client = LocationServices.getFusedLocationProviderClient(this);
            client.getLastLocation().addOnSuccessListener(location -> {
                if (location != null) {
                    // Geocodificar
                    Geocoder geocoder = new Geocoder(this, Locale.getDefault());
                    String ciudad = "Desconocido";
                    try {
                        List<Address> direcciones = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1);
                        if (!direcciones.isEmpty()) {
                            Address address = direcciones.get(0);
                            String city = address.getLocality();
                            ciudad = (city != null) ? city : address.getAdminArea();
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    // Leer y actualizar en Firestore
                    FirestoreHelper firestoreHelper = new FirestoreHelper();
                    String finalCiudad = ciudad;
                    String finalCiudad1 = ciudad;
                    firestoreHelper.leerUsuario(
                            userId,
                            usuario -> {
                                if (usuario != null) {
                                    usuario.setLocalizacion(finalCiudad1);
                                    firestoreHelper.guardarUsuario(
                                            userId,
                                            usuario,
                                            aVoid -> {
                                                Log.d("Login", "Localización actualizada: " + finalCiudad);
                                                // Después de actualizar => ir a Main
                                                startActivity(new Intent(this, MainActivity.class));
                                                finish();
                                            },
                                            error -> {
                                                Log.e("Login", "Error al guardar localización: " + error.getMessage());
                                                // Igual vamos a Main
                                                startActivity(new Intent(this, MainActivity.class));
                                                finish();
                                            }
                                    );
                                } else {
                                    // Si no existe, igual vamos a Main
                                    startActivity(new Intent(this, MainActivity.class));
                                    finish();
                                }
                            },
                            error -> {
                                Log.e("Login", "Error al leer usuario: " + error.getMessage());
                                startActivity(new Intent(this, MainActivity.class));
                                finish();
                            }
                    );
                } else {
                    // location == null
                    // Ir a Main
                    startActivity(new Intent(this, MainActivity.class));
                    finish();
                }
            });
        } else {
            // Si por alguna razón ya no tenemos permiso
            startActivity(new Intent(this, MainActivity.class));
            finish();
        }
    }



    private void solicitarUbicacionTrasLogin() {
        // Llamas al helper
        locationHelper.solicitarPermisoUbicacion();
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

