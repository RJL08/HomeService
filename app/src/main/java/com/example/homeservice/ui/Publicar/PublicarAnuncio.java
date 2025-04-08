package com.example.homeservice.ui.Publicar;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.example.homeservice.R;
import com.example.homeservice.database.FirestoreHelper;
import com.example.homeservice.model.Anuncio;
import com.example.homeservice.ui.Anuncios.HomeFragment;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import java.util.ArrayList;

public class PublicarAnuncio extends AppCompatActivity {

    private static final int REQUEST_IMAGE_CAPTURE = 100;
    private static final int REQUEST_IMAGE_PICK = 101;
    private static final int PERMISSION_CAMERA_REQUEST_CODE = 200;
    private static final int PERMISSION_READ_STORAGE_REQUEST_CODE = 201;

    private LinearLayout contenedorImagenes;
    private ArrayList<Uri> imagenesSeleccionadas = new ArrayList<>();
    private static final int LIMITE_IMAGENES = 5;

    private ActivityResultLauncher<Intent> cameraLauncher;
    private ActivityResultLauncher<String> requestCameraPermissionLauncher;

    private ActivityResultLauncher<String> galleryLauncher;
    private ActivityResultLauncher<String> requestGalleryPermissionLauncher;

    private EditText etTitulo, etDescripcion, etCiudad;
    private AutoCompleteTextView dropdownCategoria;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.publicar_anuncio);

        contenedorImagenes = findViewById(R.id.contenedorImagenes);
        ImageView btnAgregarImagen = findViewById(R.id.btnAgregarImagen);

        etTitulo = findViewById(R.id.etTitulo);
        etDescripcion = findViewById(R.id.etDescripcion);
        etCiudad = findViewById(R.id.etCiudad);

        dropdownCategoria = findViewById(R.id.dropdownCategoria);
        // Adaptador con el array de strings definido en strings.xml
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_dropdown_item_1line,
                getResources().getStringArray(R.array.categorias_array)
        );
        dropdownCategoria.setAdapter(adapter);
        dropdownCategoria.setKeyListener(null); // Desactiva la escritura manual
        dropdownCategoria.setFocusable(false);  // Desactiva el foco manual
        dropdownCategoria.setOnClickListener(v -> dropdownCategoria.showDropDown()); // Muestra opciones al tocar

        // Botón para publicar anuncio
        Button btnPublicar = findViewById(R.id.btnPublicar);
        // Evento click del botón para publicar
        btnPublicar.setOnClickListener(v -> {
            publicarAnuncio(); // 🔥 Aquí se llama al método
        });

        // Lanzador para la cámara
        cameraLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Bundle extras = result.getData().getExtras();
                        Bitmap imageBitmap = (Bitmap) extras.get("data");
                        if (imageBitmap != null && imagenesSeleccionadas.size() < LIMITE_IMAGENES) {
                            ImageView imgView = new ImageView(this);
                            imgView.setLayoutParams(new LinearLayout.LayoutParams(200, 200));
                            imgView.setScaleType(ImageView.ScaleType.CENTER_CROP);
                            imgView.setImageBitmap(imageBitmap);
                            imgView.setPadding(8, 8, 8, 8);
                            contenedorImagenes.addView(imgView, contenedorImagenes.getChildCount() - 1);
                        }
                    }
                }
        );

// Permiso de cámara
        requestCameraPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    if (isGranted) openCamera();
                    else Toast.makeText(this, "Permiso de cámara denegado", Toast.LENGTH_SHORT).show();
                }
        );


        // Lanzador para galería
        galleryLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null && imagenesSeleccionadas.size() < LIMITE_IMAGENES) {
                        agregarImagenAlContenedor(uri);
                        imagenesSeleccionadas.add(uri);
                    } else {
                        Toast.makeText(this, "Máximo 5 imágenes", Toast.LENGTH_SHORT).show();
                    }
                });

        // Lanzador para permiso
        requestGalleryPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    if (isGranted) openGallery();
                    else Toast.makeText(this, "Permiso de galería denegado", Toast.LENGTH_SHORT).show();
                });

        // Evento botón agregar imagen
        btnAgregarImagen.setOnClickListener(v -> mostrarDialogoImagen());
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == PERMISSION_CAMERA_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openCamera();
            } else {
                Toast.makeText(this, "Permiso de cámara denegado", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void mostrarDialogoImagen() {
        if (imagenesSeleccionadas.size() >= LIMITE_IMAGENES) {
            Toast.makeText(this, "Máximo 5 imágenes permitidas", Toast.LENGTH_SHORT).show();
            return;
        }

        String[] opciones = {"Hacer foto", "Elegir de galería"};
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Seleccionar imagen")
                .setItems(opciones, (dialog, which) -> {
                    if (which == 0) {
                        if (checkCameraPermission()) openCamera();
                        else requestCameraPermission();
                    } else {
                        if (checkReadImagesPermission()) openGallery();
                        else requestReadImagesPermission();
                    }
                })
                .create()
                .show();
    }

    private void agregarImagenAlContenedor(Uri uri) {
        ImageView nuevaImagen = new ImageView(this);
        nuevaImagen.setLayoutParams(new LinearLayout.LayoutParams(200, 200));
        nuevaImagen.setImageURI(uri);
        nuevaImagen.setScaleType(ImageView.ScaleType.CENTER_CROP);
        nuevaImagen.setPadding(8, 8, 8, 8);
        contenedorImagenes.addView(nuevaImagen, contenedorImagenes.getChildCount() - 1);
    }

    private void openGallery() {
        galleryLauncher.launch("image/*");
    }

    private boolean checkReadImagesPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES)
                    == PackageManager.PERMISSION_GRANTED;
        } else {
            return ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_GRANTED;
        }
    }

    private void requestReadImagesPermission() {
        String permiso = (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
                ? Manifest.permission.READ_MEDIA_IMAGES
                : Manifest.permission.READ_EXTERNAL_STORAGE;

        requestGalleryPermissionLauncher.launch(permiso);
    }

    private boolean checkCameraPermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED;
    }

    private void requestCameraPermission() {
        ActivityCompat.requestPermissions(
                this,
                new String[]{Manifest.permission.CAMERA},
                PERMISSION_CAMERA_REQUEST_CODE
        );
    }


    private void openCamera() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        cameraLauncher.launch(intent);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK) {
            if (requestCode == REQUEST_IMAGE_CAPTURE && data != null) {
                Bundle extras = data.getExtras();
                Bitmap imageBitmap = (Bitmap) extras.get("data");
                if (imageBitmap != null && imagenesSeleccionadas.size() < LIMITE_IMAGENES) {
                    ImageView imgView = new ImageView(this);
                    imgView.setLayoutParams(new LinearLayout.LayoutParams(200, 200));
                    imgView.setScaleType(ImageView.ScaleType.CENTER_CROP);
                    imgView.setImageBitmap(imageBitmap);
                    imgView.setPadding(8, 8, 8, 8);
                    contenedorImagenes.addView(imgView, contenedorImagenes.getChildCount() - 1);
                }
            }
        }
    }

    private void publicarAnuncio() {
        String titulo = etTitulo.getText().toString().trim();
        String descripcion = etDescripcion.getText().toString().trim();
        String oficio = dropdownCategoria.getText().toString().trim();
        String ciudad = etCiudad.getText().toString().trim();
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        // Validación básica
        if (titulo.isEmpty()) {
            etTitulo.setError("Título requerido");
            etTitulo.requestFocus();
            return;
        }
        if (oficio.isEmpty()) {
            dropdownCategoria.setError("Selecciona una categoría");
            dropdownCategoria.requestFocus();
            return;
        }
        if (ciudad.isEmpty()) {
            etCiudad.setError("Ciudad requerida");
            etCiudad.requestFocus();
            return;
        }
        if (descripcion.isEmpty()) {
            etDescripcion.setError("Descripción requerida");
            etDescripcion.requestFocus();
            return;
        }
        if (user == null) {
            Toast.makeText(this, "Usuario no autenticado", Toast.LENGTH_SHORT).show();
            return;
        }

        // Crear objeto Anuncio
        Anuncio anuncio = new Anuncio(
                titulo,
                descripcion,
                oficio,
                ciudad,
                user.getUid(),
                System.currentTimeMillis()
        );

        // Usar FirestoreHelper
        FirestoreHelper firestoreHelper = new FirestoreHelper();
        firestoreHelper.crearAnuncio(anuncio,
                id -> {
                    Toast.makeText(this, "Anuncio publicado correctamente", Toast.LENGTH_SHORT).show();
                    // Ir a MainActivity donde se muestra el anuncio
                    Intent intent = new Intent(PublicarAnuncio.this, HomeFragment.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                    finish();
                },
                error -> {
                    Toast.makeText(this, "Error al publicar: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                }
        );
    }


}
