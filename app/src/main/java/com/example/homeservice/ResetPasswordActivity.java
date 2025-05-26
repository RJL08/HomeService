package com.example.homeservice;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.homeservice.utils.ValidacionUtils;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;

/**
 * Actividad para recuperar contraseña de un usuario mediante e-mail.
 * con metodos como sendPasswordResetEmail() y signInWithEmailAndPassword() de FirebaseAuth.
 */
public class ResetPasswordActivity extends AppCompatActivity {

    private EditText etCorreo;
    private Button btnEnviar;
    private FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reset_password);

        etCorreo  = findViewById(R.id.etCorreoReset);
        btnEnviar = findViewById(R.id.btnEnviarReset);
        auth      = FirebaseAuth.getInstance();

        btnEnviar.setOnClickListener(v -> enviarCorreoReset());
    }

    private void enviarCorreoReset() {
        String email = etCorreo.getText().toString().trim();

        if (!ValidacionUtils.validarCorreo(email)) {
            etCorreo.setError("Correo inválido");           // ❶ validación reutilizada
            etCorreo.requestFocus();
            return;
        }

        // ❷ Un solo paso: pedir a Firebase que envíe el e-mail
        auth.sendPasswordResetEmail(email)
                .addOnSuccessListener(aVoid ->
                        Toast.makeText(this,
                                "Correo de recuperación enviado",
                                Toast.LENGTH_LONG).show())

                .addOnFailureListener(e -> {
                    if (e instanceof FirebaseAuthInvalidUserException) {
                        // El e-mail no corresponde a ninguna cuenta
                        Toast.makeText(this,
                                "Ese correo no está registrado",
                                Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(this,
                                "Error: " + e.getMessage(),
                                Toast.LENGTH_LONG).show();
                    }
                });
    }
}
