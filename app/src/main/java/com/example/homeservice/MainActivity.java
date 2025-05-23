package com.example.homeservice;


import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;
import androidx.preference.PreferenceManager;

import com.example.homeservice.database.FirestoreHelper;
import com.example.homeservice.databinding.ActivityMainBinding;
import com.example.homeservice.ui.Publicar.PublicarAnuncio;
import com.example.homeservice.ui.chat.NotificacionesActivity;
import com.example.homeservice.ui.menu.AjustesActivity;
import com.example.homeservice.ui.perfil.EditarPerfilActivity;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.auth.UserInfo;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.WriteBatch;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.squareup.picasso.Picasso;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private AppBarConfiguration mAppBarConfiguration;
    private ActivityMainBinding binding;

    // ★ Referencias al header para poder actualizarlas en onResume()
    private TextView nameTextView;      // ★
    private TextView emailTextView;     // ★
    private ImageView profileImageView; // ★

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // ★ Cargar modo oscuro desde SharedPreferences
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        boolean oscuro = prefs.getBoolean("modo_oscuro", false);
        AppCompatDelegate.setDefaultNightMode(
                oscuro ? AppCompatDelegate.MODE_NIGHT_YES
                        : AppCompatDelegate.MODE_NIGHT_NO
        );


        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());



        setSupportActionBar(binding.appBarMain.toolbar);


        // Crear canal **antes** de setContentView(...)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel canal = new NotificationChannel(
                    getString(R.string.default_notification_channel_id),
                    "Notificaciones generales",
                    NotificationManager.IMPORTANCE_HIGH
            );
            canal.setDescription("Canal por defecto para notificaciones de la app");
            NotificationManager nm = getSystemService(NotificationManager.class);
            nm.createNotificationChannel(canal);
        }

        // *** 1) CONFIGURAR EL DRAWER
        DrawerLayout drawer = binding.drawerLayout;
        NavigationView navigationView = binding.navView;

        // *** 2) OBTENER EL HEADER
        View headerView = navigationView.getHeaderView(0);

        LinearLayout headerContainer = headerView.findViewById(R.id.nav_header_container);

        //  3) CALCULAR Y APLICAR MARGEN TOP
        int statusBarHeight = 0;
        int resourceId = getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            statusBarHeight = getResources().getDimensionPixelSize(resourceId);
        }
        ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) headerContainer.getLayoutParams();
        params.topMargin += statusBarHeight;
        headerContainer.setLayoutParams(params);

        // 4) OBTENER TEXTVIEW E IMAGEVIEW (guardamos referencias de campo)
        nameTextView   = headerView.findViewById(R.id.user_name);   //
        emailTextView  = headerView.findViewById(R.id.user_email);  //
        profileImageView = headerView.findViewById(R.id.imageView); //

        // *** 5) PRIMERA CARGA DE DATOS EN CABECERA ★
        cargarDatosUsuarioEnCabecera(); // ★

        // *** 6) CONFIGURAR NAVIGATION CONTROLLER
        mAppBarConfiguration = new AppBarConfiguration.Builder(
                R.id.nav_home, R.id.nav_gallery, R.id.nav_slideshow
        ).setOpenableLayout(drawer).build();

        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        NavigationUI.setupActionBarWithNavController(this, navController, mAppBarConfiguration);
        NavigationUI.setupWithNavController(navigationView, navController);

        // *** MANEJAR CLIC EN MENÚ PERSONALIZADO ***
        navigationView.setNavigationItemSelectedListener(item -> {
            int id = item.getItemId();

            if (id == R.id.nav_publicar) {
                startActivity(new Intent(this, PublicarAnuncio.class));
                drawer.closeDrawers();
                return true;
            }
            if (id == R.id.nav_logout) {
                realizarLogout();
                drawer.closeDrawers();
                return true;
            }
            if (id == R.id.nav_notify) {
                startActivity(new Intent(MainActivity.this, NotificacionesActivity.class));
                drawer.closeDrawers();
                return true;
            }

            if (id == R.id.nav_delete) {
                mostrarDialogoEliminar();
                drawer.closeDrawers();
                return true;
            }

            boolean handled = NavigationUI.onNavDestinationSelected(item, navController);
            if (handled) {
                drawer.closeDrawers();
            }
            return handled;
        });
    }

    // ★ Se vuelve a leer SharedPreferences cada vez que la Activity pasa a primer plano
    @Override
    protected void onResume() {
        super.onResume();
        cargarDatosUsuarioEnCabecera();

    }

    // ★ Método único para leer prefs y colocar datos en la cabecera
    private void cargarDatosUsuarioEnCabecera() { // ★
        SharedPreferences prefs = getSharedPreferences("MyAppPrefs", MODE_PRIVATE);

        String userName  = prefs.getString("userName",  "Invitado");
        String userEmail = prefs.getString("userEmail", "correo@ejemplo.com");
        String userPhoto = prefs.getString("userPhoto", "");

        nameTextView.setText(userName);
        emailTextView.setText(userEmail);

        if (userPhoto != null && !userPhoto.isEmpty()) {
            Picasso.get()
                    .load(userPhoto)
                    .placeholder(R.drawable.ic_launcher_background)
                    .error(R.drawable.ic_launcher_foreground)
                    .into(profileImageView);
        } else {
            profileImageView.setImageResource(R.drawable.ic_launcher_background);
        }
    }



    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_action_bar, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_edit_profile) {
            startActivity(new Intent(this, EditarPerfilActivity.class));
            return true;
        }
        if (item.getItemId() == R.id.accion_ajustes) {
            startActivity(new Intent(this, AjustesActivity.class));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onSupportNavigateUp() {
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        return NavigationUI.navigateUp(navController, mAppBarConfiguration)
                || super.onSupportNavigateUp();
    }

    private void realizarLogout() {
        // 1) prefs
        getSharedPreferences("MyAppPrefs", MODE_PRIVATE)
                .edit().clear().apply();

        // 2) Firebase    → vale para mail, Google, Facebook, etc.
        FirebaseAuth.getInstance().signOut();

        // 3) Si el último login fue con Google, revoca el token
        FirebaseUser u = FirebaseAuth.getInstance().getCurrentUser(); // ya es null
        if (GoogleSignIn.getLastSignedInAccount(this) != null) {
            GoogleSignIn.getClient(this,
                    GoogleSignInOptions.DEFAULT_SIGN_IN).signOut();
        }

        // 4) Vuelve a Login limpiando el back-stack
        Intent i = new Intent(this, Login.class);
        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(i);
        finish();
    }


    // ───────────────────────────────────────────────────────
//  1) Muestra el diálogo “¿Seguro que quieres eliminar…?”
// ───────────────────────────────────────────────────────
    private void mostrarDialogoEliminar() {
        View v = LayoutInflater.from(this)
                .inflate(R.layout.dialog_delete_confirm, null);
        new MaterialAlertDialogBuilder(this)
                .setTitle("Eliminar cuenta")
                .setMessage("¿Seguro que quieres borrar tu cuenta de HomeService?\n"
                        + "Se eliminarán anuncios, favoritos y conversaciones.")
                .setNegativeButton("Cancelar", null)
                .setPositiveButton("Sí, eliminar", (d, w) -> reautenticarYBorrar())
                .show();
    }


    // ─────────────────────────────────────────────────
//  2) Firebase exige re-authentication para borrar
    private void reautenticarYBorrar() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        /* ───── 1· Averigua si el usuario tiene Google como proveedor ───── */
        boolean tieneGoogle = false;
        for (UserInfo info : user.getProviderData()) {
            if (GoogleAuthProvider.PROVIDER_ID.equals(info.getProviderId())) {
                tieneGoogle = true; break;
            }
        }

        /* ───── 2· Si tiene Google, intenta re-autenticar con él ───── */
        if (tieneGoogle) {
            GoogleSignInAccount gsa = GoogleSignIn.getLastSignedInAccount(this);
            if (gsa != null && gsa.getIdToken() != null) {
                AuthCredential cred =
                        GoogleAuthProvider.getCredential(gsa.getIdToken(), null);

                user.reauthenticate(cred)
                        .addOnSuccessListener(a -> borrarTodo(user))
                        .addOnFailureListener(e -> {
                            // Token caducado o sesión antigua → pide contraseña
                            mostrarDialogoPassword(user);
                        });
                return;
            }
            // No hay cuenta Google válida: pasamos a contraseña
        }

        /* ───── 3· Email/Password (o Google sin token reciente) ───── */
        Log.d("DeleteFlow", "Mostrando diálogo contraseña");
        mostrarDialogoPassword(user);
    }


    /* ─────────────────────────────────────────────────────
       2) Pide la contraseña y reautentica con EmailAuth
       ───────────────────────────────────────────────────── */
    private void mostrarDialogoPassword(FirebaseUser user) {
        View vista = LayoutInflater.from(this)
                .inflate(R.layout.dialog_password, null);
        TextInputEditText etEmail = vista.findViewById(R.id.etEmail);
        TextInputEditText etPwd   = vista.findViewById(R.id.etPwd);
        TextInputLayout tilPwd   = vista.findViewById(R.id.tilPwd);

        etEmail.setText(user.getEmail());

        AlertDialog dlg = new MaterialAlertDialogBuilder(this, R.style.ThemeOverlay_HomeService_Dialog)
                .setTitle("Confirmar contraseña")
                .setView(vista)
                .setNegativeButton("Cancelar", null)
                .setPositiveButton("Continuar", null) // lo sobreescribiremos para validar
                .create();

        dlg.setOnShowListener(d -> {
            dlg.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                String pwd = etPwd.getText() != null ? etPwd.getText().toString().trim() : "";
                if (pwd.isEmpty()) {
                    tilPwd.setError("Obligatorio");
                    return;
                }
                tilPwd.setError(null);

                AuthCredential cred =
                        EmailAuthProvider.getCredential(user.getEmail(), pwd);

                dlg.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false); // evita toques dobles
                user.reauthenticate(cred)
                        .addOnSuccessListener(a -> {
                            dlg.dismiss();
                            borrarTodo(user);
                        })
                        .addOnFailureListener(e -> {
                            tilPwd.setError("Contraseña incorrecta");
                            dlg.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(true);
                        });
            });
        });
        dlg.show();
    }



    // ───────────────────────────────────────────────
//  3) Borra documentos, foto de Storage y cuenta
// ───────────────────────────────────────────────
    private void borrarTodo(@NonNull FirebaseUser user) {

        AlertDialog progreso = crearDialogoProgreso();   // rueda indeterminada
        progreso.show();

        String uid = user.getUid();
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        List<Task<Void>> tareas = new ArrayList<>();

        // 3.1  Documento principal  /usuarios/{uid}
        tareas.add(db.collection("usuarios").document(uid).delete());

        // 3.2  Anuncios del usuario
        tareas.add(borrarColeccion(db.collection("anuncios")
                .whereEqualTo("userId", uid)));

        // 3.3  Favoritos del usuario
        tareas.add(borrarColeccion(db.collection("favoritos")
                .whereEqualTo("userId", uid)));

        // 3.4  Conversaciones + subcolección mensajes
        tareas.add(db.collection("conversaciones")
                .whereArrayContains("participants", uid).get()
                .continueWithTask(t -> borrarConversaciones(t.getResult(), uid)));

        // 3.5  Foto de perfil en Storage
        StorageReference foto = FirebaseStorage.getInstance()
                .getReference("perfiles/" + uid + ".jpg");
        tareas.add(foto.delete().addOnFailureListener(e -> {})); // ignora si no existe

        // ——— Espera a que todo termine ———
        Tasks.whenAll(tareas)
                .addOnSuccessListener(r -> {
                    // Solo si TODOS los borrados han tenido éxito
                    user.delete()
                            .addOnSuccessListener(v -> {
                                progreso.dismiss();
                                Toast.makeText(MainActivity.this,
                                        "Cuenta eliminada correctamente.\n¡Gracias por usar HomeService!",
                                        Toast.LENGTH_LONG).show();
                                realizarLogout();
                            })
                            .addOnFailureListener(e -> {
                                // Si falla el user.delete
                                progreso.dismiss();
                                Toast.makeText(this, "Error eliminando la cuenta: " + e.getMessage(), Toast.LENGTH_LONG).show();
                            });
                })
                .addOnFailureListener(e -> {
                    // Si falla CUALQUIER borrado de usuario/anuncios/conversaciones
                    progreso.dismiss();Toast.makeText(this, "Error al borrar datos: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    // Borra todos los docs de un Query (<= 500 docs; si creces usa paginado)
    private Task<Void> borrarColeccion(@NonNull Query q) {
        return q.get().continueWithTask(t -> {
            WriteBatch b = FirebaseFirestore.getInstance().batch();
            for (DocumentSnapshot d : t.getResult()) b.delete(d.getReference());
            return b.commit();
        });
    }

    // Borra conversaciones y su subcolección /mensajes
    private Task<Void> borrarConversaciones(QuerySnapshot qs, String uid) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        List<Task<Void>> subtareas = new ArrayList<>();

        for (DocumentSnapshot conv : qs) {
            // a) mensajes
            subtareas.add(borrarColeccion(conv.getReference().collection("mensajes")));
            // b) conversación
            subtareas.add(conv.getReference().delete());
        }
        return Tasks.whenAll(subtareas);
    }

    // Crea un AlertDialog con CircularProgressIndicator centrado
    private AlertDialog crearDialogoProgreso() {
        CircularProgressIndicator p = new CircularProgressIndicator(this);
        p.setIndeterminate(true);
        p.setPadding(32, 32, 32, 32);

        return new MaterialAlertDialogBuilder(this)
                .setCancelable(false)
                .setView(p)
                .create();
    }


}
