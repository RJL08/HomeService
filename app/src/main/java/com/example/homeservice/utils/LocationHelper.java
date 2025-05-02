package com.example.homeservice.utils;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.net.Uri;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.*;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.tasks.OnFailureListener;
import java.util.function.BiConsumer;

/**
 * Clase Helper para manejar permisos y obtener la ubicación
 * (lat/lng) y convertirla a ciudad. Se puede requerir obligatoriamente
 * después de login o registro.
 */
public class LocationHelper {

    public static final int REQUEST_CODE_LOCATION = 1001;

    private final Activity activity;
    private final FusedLocationProviderClient fusedLocationClient;

    public LocationHelper(Activity activity) {
        this.activity = activity;
        this.fusedLocationClient = LocationServices.getFusedLocationProviderClient(activity);
    }


    /**
     * Retorna lat/lon sin geocodificar ciudad.
     */
    public void getUbicacionActual(LocationCallback callback) {
        if (ActivityCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            // Manejar error o pedir permiso de nuevo
            callback.onLocationReceived(null, null);
            return;
        }

        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(location -> {
                    if (location != null) {
                        callback.onLocationReceived(location.getLatitude(), location.getLongitude());
                    } else {
                        // fallback
                        callback.onLocationReceived(null, null);
                    }
                })
                .addOnFailureListener(e -> {
                    // fallback
                    callback.onLocationReceived(null, null);
                });
    }

    public interface LocationCallback {
        void onLocationReceived(Double lat, Double lon);
    }



    /**
     * Solicita el permiso de ubicación al usuario.
     */
    public void solicitarPermisoUbicacion() {
        if (ContextCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(activity,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    REQUEST_CODE_LOCATION);
        } else {
              // Ya concedido
        }
    }

    /**
     * Maneja el resultado del permiso de ubicación.
     * @param requestCode Código de solicitud
     * @param permissions Lista de permisos
     * @param grantResults Resultados de los permisos
    */
    public void handleRequestPermissionsResult(
            int requestCode,
            @NonNull String[] permissions,
            @NonNull int[] grantResults,
            BiConsumer<Double, Double> onSuccess,     // Devuelve lat, lon
            OnFailureListener onFailure              // Devuelve Exception
    ) {
        if (requestCode == REQUEST_CODE_LOCATION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Llamar a getUbicacionActual(...)
                getUbicacionActual((lat, lon) -> {
                    // si lat/lon no son nulos
                    if (lat != null && lon != null) {
                        onSuccess.accept(lat, lon);
                    } else {
                        onFailure.onFailure(new Exception("No se pudo obtener lat/lon"));
                    }
                });
            } else {
                onFailure.onFailure(new Exception("Permiso de ubicación denegado"));
            }
        }
    }



}

