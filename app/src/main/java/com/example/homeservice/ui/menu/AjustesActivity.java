package com.example.homeservice.ui.menu;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.example.homeservice.R;

public class AjustesActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ajustes);
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.contenedor_ajustes, new AjustesFragment())
                .commit();
    }
}
