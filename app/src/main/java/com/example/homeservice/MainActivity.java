package com.example.homeservice;

import android.annotation.SuppressLint;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;
import androidx.drawerlayout.widget.DrawerLayout;
import com.example.homeservice.databinding.ActivityMainBinding;
import com.example.homeservice.ui.Publicar.PublicarAnuncio;
import com.example.homeservice.ui.chat.NotificacionesActivity;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.material.navigation.NavigationView;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.squareup.picasso.Picasso;

public class MainActivity extends AppCompatActivity {

    private AppBarConfiguration mAppBarConfiguration;
    private ActivityMainBinding binding;

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

        // *** 1) LEER DATOS DE SharedPreferences
        SharedPreferences prefs = getSharedPreferences("MyAppPrefs", MODE_PRIVATE);
        String userName = prefs.getString("userName", "Invitado");
        String userEmail = prefs.getString("userEmail", "correo@ejemplo.com");
        String userPhoto = prefs.getString("userPhoto", "fotoPerfilDefault");

        // *** 2) CONFIGURAR EL DRAWER
        DrawerLayout drawer = binding.drawerLayout;
        NavigationView navigationView = binding.navView;

        // *** 3) OBTENER EL HEADER
        View headerView = navigationView.getHeaderView(0);

        LinearLayout headerContainer = headerView.findViewById(R.id.nav_header_container);

        // *** 4) CALCULAR Y APLICAR MARGEN TOP
        int statusBarHeight = 0;
        int resourceId = getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            statusBarHeight = getResources().getDimensionPixelSize(resourceId);
        }
        ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) headerContainer.getLayoutParams();
        params.topMargin += statusBarHeight;
        headerContainer.setLayoutParams(params);

        // *** 5) OBTENER TEXTVIEW E IMAGEVIEW
        TextView nameTextView = headerView.findViewById(R.id.user_name);
        TextView emailTextView = headerView.findViewById(R.id.user_email);
        ImageView profileImageView = headerView.findViewById(R.id.imageView);

        // *** 6) ASIGNAR DATOS
        nameTextView.setText(userName);
        emailTextView.setText(userEmail);

        Picasso.get()
                .load(userPhoto)
                .placeholder(R.drawable.ic_launcher_background)
                .error(R.drawable.ic_launcher_foreground)
                .into(profileImageView);

        // *** 7) CONFIGURAR NAVIGATION CONTROLLER
        mAppBarConfiguration = new AppBarConfiguration.Builder(
                R.id.nav_home, R.id.nav_gallery, R.id.nav_slideshow
        ).setOpenableLayout(drawer).build();

        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        NavigationUI.setupActionBarWithNavController(this, navController, mAppBarConfiguration);
        NavigationUI.setupWithNavController(navigationView, navController);


        // *** AÑADIDO: MANEJAR CLIC EN MENÚ PERSONALIZADO ***
        navigationView.setNavigationItemSelectedListener(item -> {
            int id = item.getItemId();

            if (id == R.id.nav_publicar) {
                // Abrir actividad para publicar anuncio
                startActivity(new Intent(this, PublicarAnuncio.class));
                drawer.closeDrawers();
                return true;
            }

            if (id == R.id.nav_logout) {
                // Cerrar sesión
                realizarLogout();
                drawer.closeDrawers();
                return true;
            }

            if (id == R.id.nav_notify) {
                startActivity(new Intent(MainActivity.this, NotificacionesActivity.class));
                drawer.closeDrawers();
                return true;
            }

            // Delega el resto de navegación al NavController
            boolean handled = NavigationUI.onNavDestinationSelected(item, navController);
            if (handled) {
                drawer.closeDrawers();
            }
            return handled;
        });


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
                    // Regresamos a la pantalla de login
                    Intent signOutIntent = new Intent(MainActivity.this, Login.class);
                    signOutIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(signOutIntent);
                    finish();
                });
    }
}
