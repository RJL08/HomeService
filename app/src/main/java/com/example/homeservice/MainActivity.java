package com.example.homeservice;


import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.example.homeservice.databinding.ActivityMainBinding;
import com.example.homeservice.ui.Publicar.PublicarAnuncio;
import com.example.homeservice.ui.chat.NotificacionesActivity;
import com.example.homeservice.ui.perfil.EditarPerfilActivity;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.squareup.picasso.Picasso;

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
        cargarDatosUsuarioEnCabecera(); // ★
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
    } //

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
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onSupportNavigateUp() {
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        return NavigationUI.navigateUp(navController, mAppBarConfiguration)
                || super.onSupportNavigateUp();
    }

    private void realizarLogout() {
        // 1) Limpias SharedPreferences
        SharedPreferences.Editor editor = getSharedPreferences("MyAppPrefs", MODE_PRIVATE).edit();
        editor.clear();
        editor.apply();

        // 2) signOut de Firebase
        FirebaseAuth.getInstance().signOut();

        // 3) signOut de Google
        GoogleSignIn.getClient(this, GoogleSignInOptions.DEFAULT_SIGN_IN).signOut()
                .addOnCompleteListener(task -> {
                    Intent signOutIntent = new Intent(MainActivity.this, Login.class);
                    signOutIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(signOutIntent);
                    finish();
                });
    }
}
