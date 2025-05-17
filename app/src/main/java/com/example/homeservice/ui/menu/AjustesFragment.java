package com.example.homeservice.ui.menu;

import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatDelegate;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;
import androidx.preference.SwitchPreferenceCompat;

import com.example.homeservice.R;
import com.example.homeservice.notificaciones.GestorNotificaciones;
import com.google.firebase.messaging.FirebaseMessaging;

public class AjustesFragment extends PreferenceFragmentCompat
        implements SharedPreferences.OnSharedPreferenceChangeListener {

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.preferencias, rootKey);

        // 1) Localiza la Switch de notificaciones
        SwitchPreferenceCompat swNotis = findPreference("notificaciones_activadas");
        if (swNotis != null) {
            swNotis.setOnPreferenceChangeListener((preference, newValue) -> {
                boolean activadas = (Boolean) newValue;
                // 2) Guarda en SharedPreferences (opcional si ya lo hace automáticamente)
                SharedPreferences prefs = PreferenceManager
                        .getDefaultSharedPreferences(requireContext());
                prefs.edit()
                        .putBoolean("notificaciones_activadas", activadas)
                        .apply();

                // 3) Sube o elimina el token según el nuevo estado
                FirebaseMessaging.getInstance().getToken()
                        .addOnSuccessListener(token -> {
                            if (activadas) {
                                GestorNotificaciones.subirTokenAFirestore(token);
                            } else {
                                GestorNotificaciones.eliminarTokenDeFirestore(token);
                            }
                        });

                // 4) devuelve true para que el switch cambie su estado visual
                return true;
            });
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        getPreferenceScreen()
                .getSharedPreferences()
                .registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onPause() {
        getPreferenceScreen()
                .getSharedPreferences()
                .unregisterOnSharedPreferenceChangeListener(this);
        super.onPause();
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
        switch (key) {
            case "modo_oscuro":
                boolean oscuro = prefs.getBoolean(key, false);
                AppCompatDelegate.setDefaultNightMode(
                        oscuro
                                ? AppCompatDelegate.MODE_NIGHT_YES
                                : AppCompatDelegate.MODE_NIGHT_NO
                );
                break;
            // sonidos y notis no requieren acción inmediata aquí
        }
    }
}
