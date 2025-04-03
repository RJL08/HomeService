package com.example.homeservice;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.util.Log;
import android.util.Patterns;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import android.Manifest;
import com.example.homeservice.database.FirestoreHelper;
import com.example.homeservice.model.Usuario;
import com.example.homeservice.utils.LocationHelper;
import com.example.homeservice.utils.ValidacionUtils;
import com.google.android.gms.auth.api.signin.*;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.location.*;
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
        Log.d("LoginDebug", "onCreate: Iniciando Login activity");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Inicialización de Firebase
        firebaseAuth = FirebaseAuth.getInstance();
        Log.d("LoginDebug", "FirebaseAuth instance obtained");

        // Referencias UI
        etCorreo = findViewById(R.id.etCorreo);
        etContrasena = findViewById(R.id.etContrasena);
        btnLoginCorreo = findViewById(R.id.btnLoginCorreo);
        btnLoginGoogle = findViewById(R.id.btnLoginGoogle);
        tvRegistrar = findViewById(R.id.tvRegistrar);

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
        String correo = etCorreo.getText().toString().trim();
        String contrasena = etContrasena.getText().toString().trim();

        // Validación usando ValidacionUtils
        if (!ValidacionUtils.validarCorreo(correo)) {
            etCorreo.setError("Correo inválido. Debe ser .com o .es");
            etCorreo.requestFocus();
            return;
        }

        if (!ValidacionUtils.validarContrasena(contrasena)) {
            etContrasena.setError("Contraseña inválida (8-18 caracteres)");
            etContrasena.requestFocus();
            return;
        }

        Log.d("LoginDebug", "Intentando iniciar sesión con: " + correo);

        firebaseAuth.signInWithEmailAndPassword(correo, contrasena)
                .addOnSuccessListener(authResult -> {
                    Log.d("LoginDebug", "Inicio de sesión exitoso para UID: " + authResult.getUser().getUid());
                    Toast.makeText(this, "Inicio de sesión exitoso", Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(this, MainActivity.class));
                    finish();
                })
                .addOnFailureListener(e -> {
                    String mensaje = "Error al iniciar sesión";
                    if (e instanceof FirebaseAuthInvalidUserException) {
                        mensaje = "Este correo no está registrado";
                    } else if (e instanceof FirebaseAuthInvalidCredentialsException) {
                        mensaje = "Contraseña incorrecta";
                    }

                    Log.e("LoginDebug", "Fallo al iniciar sesión: " + e.getMessage());
                    Toast.makeText(this, mensaje, Toast.LENGTH_LONG).show();
                });
    }



    /**
     * signIn final (correo sí existe)
     */
    private void signInConCorreo(String correo, String contrasena) {
        Log.d("LoginDebug", "signInConCorreo: email=[" + correo + "], pass=[" + contrasena + "]");

        firebaseAuth.signInWithEmailAndPassword(correo, contrasena)
                .addOnSuccessListener(authResult -> {
                    Log.d("LoginDebug", "signInWithEmailAndPassword onSuccess. UID=" + authResult.getUser().getUid());

                    Toast.makeText(this, "Inicio de sesión exitoso", Toast.LENGTH_SHORT).show();

                    FirebaseUser user = authResult.getUser();
                    if (user != null) {
                        String userId = user.getUid();
                        Log.d("LoginDebug", "userId=" + userId);

                        // Guardamos en Firestore con ciudad = "" por si no estaba
                        FirestoreHelper firestoreHelper = new FirestoreHelper();
                        Usuario usuario = new Usuario(
                                userId,
                                "NombreDesconocido",
                                "ApellidosDesconocidos",
                                user.getEmail(),
                                "",
                                "fotoPerfilDefault"
                        );
                        firestoreHelper.guardarUsuario(
                                userId,
                                usuario,
                                aVoid -> {
                                    Log.d("Login", "Usuario guardado sin ciudad (login con correo).");
                                },
                                e -> {
                                    Log.e("LoginDebug", "Error al guardar usuario en Firestore: " + e.getMessage());
                                    Toast.makeText(this, "Error al guardar usuario: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                }
                        );

                        // Guardar en SharedPreferences
                        guardarDatosEnPrefs("NombreDesconocido", user.getEmail(), "fotoPerfilDefault");
                    } else {
                        Log.w("LoginDebug", "user es null tras signIn con correo");
                    }

                    // Comprobar permisos => actualiza localización o pide permiso
                    if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                            == PackageManager.PERMISSION_GRANTED) {
                        Log.d("LoginDebug", "Permiso FINE_LOCATION YA concedido -> obtenerLocalizacionYActualizar()");
                        obtenerLocalizacionYActualizar();
                    } else {
                        Log.d("LoginDebug", "No hay permiso FINE_LOCATION -> solicitarPermisoUbicacion()");
                        locationHelper.solicitarPermisoUbicacion();
                    }

                    // Ir a Main
                    Log.d("LoginDebug", "Iniciando MainActivity");
                    startActivity(new Intent(this, MainActivity.class));
                    finish();
                })
                .addOnFailureListener(e -> {
                    Log.e("LoginDebug", "signInWithEmailAndPassword -> " + e.getMessage());
                    Toast.makeText(this, "Error signInWithEmailAndPassword: " + e.getMessage(), Toast.LENGTH_LONG).show();
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
        Log.d("LoginDebug", "autenticarConFirebase(Google): idToken=" + cuenta.getIdToken());
        AuthCredential credential = GoogleAuthProvider.getCredential(cuenta.getIdToken(), null);
        firebaseAuth.signInWithCredential(credential)
                .addOnSuccessListener(authResult -> {
                    Log.d("LoginDebug", "signInWithCredential(Google) onSuccess. UID=" + authResult.getUser().getUid());
                    Toast.makeText(this, "Inicio de sesión con Google exitoso", Toast.LENGTH_SHORT).show();

                    FirebaseUser user = firebaseAuth.getCurrentUser();
                    if (user != null) {
                        String userId = user.getUid();
                        Log.d("LoginDebug", "userId (Google)=" + userId);

                        String nombre = (cuenta.getGivenName() != null) ? cuenta.getGivenName() : "SinNombre";
                        String apellidos = (cuenta.getFamilyName() != null) ? cuenta.getFamilyName() : "SinApellidos";
                        String correo = cuenta.getEmail();
                        String foto = (cuenta.getPhotoUrl() != null) ? cuenta.getPhotoUrl().toString() : "fotoPerfilDefault";

                        // Guardar en Firestore sin ciudad
                        FirestoreHelper firestoreHelper = new FirestoreHelper();
                        Usuario usuario = new Usuario(userId, nombre, apellidos, correo, "", foto);
                        firestoreHelper.guardarUsuario(
                                userId,
                                usuario,
                                aVoid -> Log.d("Login", "Usuario sin ciudad guardado (Google)"),
                                error -> Log.e("Login", "Error al guardar usuario: " + error.getMessage())
                        );

                        // SharedPreferences
                        guardarDatosEnPrefs(nombre + " " + apellidos, correo, foto);

                        // Permisos localización
                        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                                == PackageManager.PERMISSION_GRANTED) {
                            Log.d("LoginDebug", "Permiso FINE_LOCATION YA concedido (Google).");
                            obtenerLocalizacionYActualizar();
                        } else {
                            Log.d("LoginDebug", "No hay permiso FINE_LOCATION -> solicitarPermisoUbicacion() (Google).");
                            locationHelper.solicitarPermisoUbicacion();
                        }
                    }
                    // MainActivity
                    Log.d("LoginDebug", "Ir a MainActivity tras Google signIn");
                    startActivity(new Intent(this, MainActivity.class));
                    finish();
                })
                .addOnFailureListener(e -> {
                    Log.e("LoginDebug", "signInWithCredential(Google) -> " + e.getMessage());
                    Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    /**
     * Si ya hay permiso => obtener lat/lng => geocodificar => actualizar Firestore
     */
    private void obtenerLocalizacionYActualizar() {
        Log.d("LoginDebug", "obtenerLocalizacionYActualizar: entrando");

        FusedLocationProviderClient client = LocationServices.getFusedLocationProviderClient(this);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            Log.d("LoginDebug", "obtenerLocalizacionYActualizar: sin permiso, return");
            return; // no hay permiso
        }
        client.getLastLocation()
                .addOnSuccessListener(location -> {
                    if (location != null) {
                        Log.d("LoginDebug", "getLastLocation onSuccess. Lat=" + location.getLatitude() + " Lng=" + location.getLongitude());
                        String ciudad = "Desconocido";
                        try {
                            List<Address> direcciones = new Geocoder(this, Locale.getDefault())
                                    .getFromLocation(location.getLatitude(), location.getLongitude(), 1);
                            if (direcciones != null && !direcciones.isEmpty()) {
                                Address address = direcciones.get(0);
                                String city = address.getLocality();
                                ciudad = (city != null) ? city : address.getAdminArea();
                                Log.d("LoginDebug", "Geocodificado ciudad=" + ciudad);
                            }
                        } catch (IOException e) {
                            Log.e("LoginDebug", "Error en geocodificar ciudad: " + e.getMessage());
                        }

                        FirebaseUser user = firebaseAuth.getCurrentUser();
                        if (user != null) {
                            String userId = user.getUid();
                            FirestoreHelper firestoreHelper = new FirestoreHelper();
                            String finalCiudad = ciudad;
                            firestoreHelper.leerUsuario(
                                    userId,
                                    usuarioLeido -> {
                                        if (usuarioLeido != null) {
                                            usuarioLeido.setLocalizacion(finalCiudad);
                                            firestoreHelper.guardarUsuario(
                                                    userId,
                                                    usuarioLeido,
                                                    aVoid -> Log.d("Login", "Localización actualizada: " + finalCiudad),
                                                    error -> Log.e("Login", "Error guardando localización: " + error.getMessage())
                                            );
                                        } else {
                                            Log.w("LoginDebug", "leerUsuario devolvió null");
                                        }
                                    },
                                    error -> Log.e("Login", "Error leyendo usuario: " + error.getMessage())
                            );
                        } else {
                            Log.w("LoginDebug", "user es null en obtenerLocalizacionYActualizar");
                        }
                    } else {
                        Log.d("LoginDebug", "location == null en obtenerLastLocation");
                    }
                })
                .addOnFailureListener(e -> Log.e("Login", "Error al obtener localización: " + e.getMessage()));
    }

    /**
     * onRequestPermissionsResult => si se concede => update localización
     */
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        Log.d("LoginDebug", "onRequestPermissionsResult: requestCode=" + requestCode);
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        locationHelper.handleRequestPermissionsResult(
                requestCode,
                permissions,
                grantResults,
                ciudad -> {
                    Log.d("LoginDebug", "Permiso concedido, ciudad=" + ciudad);
                    // Ciudad obtenida => actualizamos Firestore
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
                                                    Log.d("Login", "Localización updated: " + ciudad);
                                                    startActivity(new Intent(this, MainActivity.class));
                                                    finish();
                                                },
                                                error -> Log.e("Login", "Error al guardar localización: " + error.getMessage())
                                        );
                                    } else {
                                        Log.w("LoginDebug", "leerUsuario devolvió null en onRequestPermissionsResult");
                                    }
                                },
                                error -> Log.e("Login", "Error al leer user: " + error.getMessage())
                        );
                    } else {
                        Log.w("LoginDebug", "user es null en onRequestPermissionsResult");
                    }
                },
                e -> {
                    Log.e("LoginDebug", "Error perms localización: " + e.getMessage());
                    // Ir a Main
                    startActivity(new Intent(this, MainActivity.class));
                    finish();
                }
        );
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
}
