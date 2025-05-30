package com.example.homeservice.ui.perfil;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.*;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import com.bumptech.glide.Glide;
import com.example.homeservice.R;
import com.example.homeservice.database.FirestoreHelper;
import com.example.homeservice.model.Usuario;
import com.example.homeservice.utils.ValidacionUtils;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.storage.*;
import com.squareup.picasso.Picasso;
import java.io.ByteArrayOutputStream;
import java.io.File;


public class EditarPerfilActivity extends AppCompatActivity {

    // ────────── Vistas ──────────
    private EditText etNombre, etApellidos, etCorreo, etCiudad;
    private ImageView ivProfile;
    private ProgressBar progress;
    private Uri nuevaFotoUri;                   // solo si el usuario selecciona otra imagen
    private final FirebaseAuth auth = FirebaseAuth.getInstance();

    // ────────── Launchers de actividad/permiso ──────────
    private ActivityResultLauncher<String> galeriaLauncher;
    private ActivityResultLauncher<String> permisoGaleriaLauncher;

    // URI temporal para la foto de cámara
    private Uri pendingCameraUri;

    // Launchers adicionales:
    private ActivityResultLauncher<Intent> cameraLauncher;
    private ActivityResultLauncher<String> cameraPermLauncher;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_editar_perfil);

        /* ——— referencias ——— */
        etNombre   = findViewById(R.id.etNombreEdit);
        etApellidos= findViewById(R.id.etApellidosEdit);
        etCorreo   = findViewById(R.id.etCorreoEdit);
        etCiudad   = findViewById(R.id.etCiudadEdit);
        ivProfile  = findViewById(R.id.ivFotoPerfilEdit);
        progress   = findViewById(R.id.pr);

        /* ——— Botones ——— */
        findViewById(R.id.btnCambiarFoto).setOnClickListener(v -> mostrarDialogoSeleccionFoto());
        findViewById(R.id.btnGuardarCambios).setOnClickListener(v -> guardarCambios());

        /* ——— Launchers ——— */
        galeriaLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null) {
                        nuevaFotoUri = uri;
                        ivProfile.setImageURI(uri);
                    }
                });

        permisoGaleriaLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                concedido -> {
                    if (concedido) galeriaLauncher.launch("image/*");
                    else Toast.makeText(this,"Permiso denegado",Toast.LENGTH_SHORT).show();
                });

        // ① Permiso CÁMARA
        cameraPermLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                granted -> {
                    if (granted) abrirCamara();
                    else Toast.makeText(this, "Permiso de cámara denegado", Toast.LENGTH_SHORT).show();
                }
        );

// ② Lanzar CÁMARA
        cameraLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && pendingCameraUri != null) {
                        nuevaFotoUri = pendingCameraUri;
                        ivProfile.setImageURI(pendingCameraUri);
                    }
                }
        );

        /* ——— Cargar datos de Firestore ——— */
        cargarDatosUsuario();
    }

    private void mostrarDialogoSeleccionFoto() {
        new MaterialAlertDialogBuilder(this, R.style.ThemeOverlay_HomeService_Dialog)
                .setTitle("Seleccionar imagen")
                .setItems(new String[]{"Cámara", "Galería"}, (dialog, which) -> {
                    if (which == 0) {
                        // CÁMARA
                        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                                == PackageManager.PERMISSION_GRANTED) {
                            abrirCamara();
                        } else {
                            cameraPermLauncher.launch(Manifest.permission.CAMERA);
                        }
                    } else {
                        // GALERÍA
                        String permiso = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                                ? Manifest.permission.READ_MEDIA_IMAGES
                                : Manifest.permission.READ_EXTERNAL_STORAGE;
                        if (ContextCompat.checkSelfPermission(this, permiso)
                                == PackageManager.PERMISSION_GRANTED) {
                            galeriaLauncher.launch("image/*");
                        } else {
                            permisoGaleriaLauncher.launch(permiso);
                        }
                    }
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void abrirCamara() {
        File dir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File file = new File(dir, "perfil_" + System.currentTimeMillis() + ".jpg");
        pendingCameraUri = FileProvider.getUriForFile(
                this,
                "com.example.homeservice.fileprovider",  // debe coincidir con tu <provider> en el Manifest
                file
        );
        Intent i = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        i.putExtra(MediaStore.EXTRA_OUTPUT, pendingCameraUri);
        cameraLauncher.launch(i);
    }



    /*                CARGAR DATOS             */

    private void cargarDatosUsuario() {
        String uid = auth.getUid();
        if (uid == null) { finish(); return; }

        progress.setVisibility(View.VISIBLE);

        new FirestoreHelper().leerUsuarioDescifrado(
                uid,
                usuario -> {
                    progress.setVisibility(View.GONE);
                    if (usuario == null) { finish(); return; }

                    etNombre.setText     (usuario.getNombre());
                    etApellidos.setText  (usuario.getApellidos());
                    etCorreo.setText     (usuario.getCorreo());      // des-habilitado en XML
                    etCiudad.setText     (usuario.getLocalizacion());

                    String foto = usuario.getFotoPerfil();
                    if (foto != null && !foto.equals("default"))
                        Picasso.get().load(foto)
                                .placeholder(R.drawable.foto_reg)
                                .error(R.drawable.ic_launcher_foreground)
                                .into(ivProfile);
                },
                e -> {
                    progress.setVisibility(View.GONE);
                    Log.e("EditarPerfil","leerUsuario",e);
                    Toast.makeText(this,"Error al leer perfil",Toast.LENGTH_SHORT).show();
                    finish();
                });
    }



    /*              GUARDAR CAMBIOS            */

    private void guardarCambios() {
        String uid = auth.getUid();
        if (uid == null) return;

        String nombre    = etNombre.getText().toString().trim();
        String apellidos = etApellidos.getText().toString().trim();
        String ciudad    = etCiudad.getText().toString().trim();

        if (nombre.isEmpty())          { etNombre.setError("Obligatorio");    return; }
        if (!ValidacionUtils.validarNombre(nombre)) {
            etNombre.setError("Máx. 20 caracteres");
            return;
        }
        if (apellidos.isEmpty())       { etApellidos.setError("Obligatorio"); return; }
        if (!ValidacionUtils.validarApellidos(apellidos)) {
            etApellidos.setError("Máx. 30 caracteres");
            return;
        }

        progress.setVisibility(View.VISIBLE);

        FirestoreHelper helper = new FirestoreHelper();
        helper.leerUsuarioDescifrado(
                uid,
                usuario -> {
                    if (usuario == null) usuario = new Usuario();

                    usuario.setNombre(nombre);
                    usuario.setApellidos(apellidos);
                    usuario.setLocalizacion(ciudad);

                    if (nuevaFotoUri == null) {
                        // ─── 1· Sin cambiar foto ───
                        subirPerfil(usuario, helper, uid);
                    } else {
                        // ─── 2· Subir foto a Storage primero ───
                        Usuario finalUsuario = usuario;
                        subirImagenAFirebase(nuevaFotoUri, uid, url ->
                        { finalUsuario.setFotoPerfil(url); subirPerfil(finalUsuario, helper, uid); });
                    }
                },
                e -> {
                    progress.setVisibility(View.GONE);
                    Toast.makeText(this,"Error al leer",Toast.LENGTH_SHORT).show();
                });
    }

    /* ---------- subir usuario cifrado ---------- */
    private void subirPerfil(Usuario u, FirestoreHelper helper, String uid) {
        helper.guardarUsuarioCifrado(
                uid, u,
                aVoid -> {
                    progress.setVisibility(View.GONE);
                    actualizarPrefs(u);
                    Toast.makeText(this,"Perfil actualizado",Toast.LENGTH_SHORT).show();
                    finish();
                },
                e -> {
                    progress.setVisibility(View.GONE);
                    Toast.makeText(this,"Error al guardar: "+e.getMessage(),Toast.LENGTH_SHORT).show();
                });
    }

    /* ---------- subir imagen a Firebase Storage ---------- */
    private void subirImagenAFirebase(Uri uri, String uid, OnSuccessListener<String> cb) {
        StorageReference ref = FirebaseStorage.getInstance()
                .getReference("perfiles/" + uid + ".jpg");

        // Opcional: comprimir a JPG:
        try {
            Bitmap bmp = MediaStore.Images.Media.getBitmap(getContentResolver(), uri);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            bmp.compress(Bitmap.CompressFormat.JPEG, 85, baos);
            byte[] datos = baos.toByteArray();

            ref.putBytes(datos)
                    .continueWithTask(t -> {
                        if (!t.isSuccessful()) throw t.getException();
                        return ref.getDownloadUrl();
                    })
                    .addOnSuccessListener(url -> cb.onSuccess(url.toString()))
                    .addOnFailureListener(e -> {
                        progress.setVisibility(View.GONE);
                        Toast.makeText(this,"Error subiendo foto",Toast.LENGTH_SHORT).show();
                    });
        } catch (Exception e) {
            progress.setVisibility(View.GONE);
            Toast.makeText(this,"No se pudo procesar la imagen",Toast.LENGTH_SHORT).show();
        }
    }

    /* ---------- actualizar SharedPreferences ---------- */
    private void actualizarPrefs(Usuario u) {
        SharedPreferences.Editor ed =
                getSharedPreferences("MyAppPrefs", MODE_PRIVATE).edit();
        ed.putString("userName",  u.getNombre());
        ed.putString("userEmail", u.getCorreo());
        ed.putString("userPhoto", u.getFotoPerfil());
        ed.apply();
    }



}
