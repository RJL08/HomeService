package com.example.homeservice.utils;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class LocationIQHelper {

private static final String LOCATIONIQ_API_KEY = "TU_API_KEY"; // pon aquí tu key

/**
 * Dado lat/lon => llama a la API de LocationIQ => te devuelve el nombre de la ciudad
 * en onSuccess. O llama onFailure si falla.
 */
public static void reverseGeocode(double lat, double lon,
                                  OnSuccessListener<String> onSuccess,
                                  OnFailureListener onFailure) {

    // Construir la URL
    String url = "https://us1.locationiq.com/v1/reverse.php?"
            + "key=" + LOCATIONIQ_API_KEY
            + "&lat=" + lat
            + "&lon=" + lon
            + "&format=json";

    // Ejemplo: OkHttp para la petición
    OkHttpClient client = new OkHttpClient();
    Request request = new Request.Builder().url(url).build();

    client.newCall(request).enqueue(new Callback() {
        @Override
        public void onFailure(@NonNull Call call, @NonNull IOException e) {
            onFailure.onFailure(e);
        }

        @Override
        public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
            if (!response.isSuccessful()) {
                onFailure.onFailure(new Exception("Error " + response.code()));
                return;
            }
            String body = response.body().string();
            try {
                JSONObject json = new JSONObject(body);
                JSONObject address = json.getJSONObject("address");

                // city/town/village
                String cityName = address.optString("city",
                        address.optString("town",
                                address.optString("village", "Desconocido")));

                onSuccess.onSuccess(cityName);

            } catch (JSONException e) {
                onFailure.onFailure(e);
            }
        }
    });
}
}
