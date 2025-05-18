package com.example.homeservice;


import android.app.Application;
import android.util.Log;

import com.example.homeservice.seguridad.CommonCrypto;
import com.example.homeservice.seguridad.CommonKeyProvider;

import javax.crypto.SecretKey;

public class MyApp extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        CommonKeyProvider.get(new CommonKeyProvider.Callback() {
            @Override public void onReady(SecretKey k) {
                CommonCrypto.init(k);          // ← clave lista
            }
            @Override public void onError(Exception e) {
                Log.e("MyApp",
                        "Sin clave común – la app no funcionará", e);
            }
        });
    }
}