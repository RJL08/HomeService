package com.example.homeservice;

import android.os.Bundle;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.homeservice.utils.ValidacionUtils;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;

public class ResetPasswordActivity extends AppCompatActivity {

    private TextInputEditText etCorreo;
    private MaterialButton btnEnviar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reset_password);

        etCorreo = findViewById(R.id.etCorreoReset);
        btnEnviar = findViewById(R.id.btnEnviarReset);

        btnEnviar.setOnClickListener(v -> {
            final String email = etCorreo.getText().toString().trim();

            if (email.isEmpty()) {
                etCorreo.setError("Introduce tu correo");
                etCorreo.requestFocus();
                return;
            }
            // 1) ---- Validación local (tu helper) ----
            if (!ValidacionUtils.validarCorreo(email)) {
                etCorreo.setError("Correo no válido");
                etCorreo.requestFocus();
                return;
            }

            btnEnviar.setEnabled(false);

            FirebaseAuth auth = FirebaseAuth.getInstance();

            // 1) ¿Existe el correo?
            auth.fetchSignInMethodsForEmail(email)
                    .addOnCompleteListener(task -> {
                        if (!task.isSuccessful()) {
                            Toast.makeText(this,
                                    "Error: " + task.getException().getMessage(),
                                    Toast.LENGTH_LONG).show();
                            btnEnviar.setEnabled(true);
                            return;
                        }

                        boolean existe = task.getResult() != null &&
                                !task.getResult()
                                        .getSignInMethods()
                                        .isEmpty();

                        if (!existe) {
                            Toast.makeText(this,
                                    "Ese correo no está registrado",
                                    Toast.LENGTH_LONG).show();
                            btnEnviar.setEnabled(true);
                            return;
                        }

                        // 2) Sí existe ⇒ enviamos el mail de reseteo
                        auth.sendPasswordResetEmail(email)
                                .addOnSuccessListener(v2 -> {
                                    Toast.makeText(this,
                                            "Se envió un enlace a tu correo",
                                            Toast.LENGTH_LONG).show();
                                    finish();                       // volvemos al login
                                })
                                .addOnFailureListener(e -> {
                                    Toast.makeText(this,
                                            "Error: " + e.getMessage(),
                                            Toast.LENGTH_LONG).show();
                                    btnEnviar.setEnabled(true);
                                });
                    });
        });
    }
}