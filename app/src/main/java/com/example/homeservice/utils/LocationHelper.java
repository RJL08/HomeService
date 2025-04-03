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
import com.google.android.gms.tasks.OnSuccessListener;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

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

    public void solicitarPermisoUbicacion() {
        if (ContextCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(activity,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    REQUEST_CODE_LOCATION);
        } else {
            obtenerLocalizacion();  // Ya concedido
        }
    }

    /**
     * Maneja el resultado del permiso de ubicación.
     * @param requestCode Código de solicitud
     * @param permissions Lista de permisos
     * @param grantResults Resultados de los permisos
     * @param onCiudadObtenida Callback para devolver la ciudad
     * @param onFailure Callback en caso de error
     */
    public void handleRequestPermissionsResult(int requestCode,
                                               @NonNull String[] permissions,
                                               @NonNull int[] grantResults,
                                               OnSuccessListener<String> onCiudadObtenida,
                                               OnFailureListener onFailure) {
        if (requestCode == REQUEST_CODE_LOCATION) {
            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                obtenerLocalizacionInterna(onCiudadObtenida, onFailure);
            } else {
                onFailure.onFailure(new Exception("Permiso de ubicación denegado"));
            }
        }
    }

    public void obtenerLocalizacion() {
        obtenerLocalizacionInterna(
                ciudad -> {
                    Toast.makeText(activity, "Ciudad: " + ciudad, Toast.LENGTH_SHORT).show();
                },
                e -> {
                    Toast.makeText(activity, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
        );
    }

    private void obtenerLocalizacionInterna(OnSuccessListener<String> onCiudadObtenida,
                                            OnFailureListener onFailure) {
        if (ActivityCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            onFailure.onFailure(new Exception("Sin permiso de ubicación"));
            return;
        }

        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(location -> {
                    if (location != null) {
                        String ciudad = geocodificarCiudad(location);
                        if (ciudad == null) ciudad = "Desconocido";
                        onCiudadObtenida.onSuccess(ciudad);

                        // ❌ Eliminar esta parte si la tenías antes:
                        // abrir Google Maps con geo:lat,lng
                    } else {
                        onFailure.onFailure(new Exception("No se pudo obtener ubicación"));
                    }
                })
                .addOnFailureListener(onFailure);
    }



    private String geocodificarCiudad(Location location) {
        Geocoder geocoder = new Geocoder(activity, Locale.getDefault());
        try {
            List<Address> direcciones = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1);
            if (!direcciones.isEmpty()) {
                Address address = direcciones.get(0);
                String city = address.getLocality();
                return (city != null) ? city : address.getAdminArea();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
}

