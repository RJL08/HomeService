package com.example.homeservice.ui.Publicar;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
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

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import com.example.homeservice.MainActivity;
import com.example.homeservice.R;
import com.example.homeservice.database.FirestoreHelper;
import com.example.homeservice.model.Anuncio;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.File;
import java.util.ArrayList;
import java.util.UUID;

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

    // Uri para la última foto tomada con la cámara
    private Uri uriFotoCamara ;

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
        btnPublicar.setOnClickListener(v -> {
            publicarAnuncio();
        });

        // ================
        // L A U N C H E R S
        // ================

        // (1) Lanzador para la cámara (usa FileProvider)
        cameraLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK) {
                        if (uriFotoCamara != null && imagenesSeleccionadas.size() < LIMITE_IMAGENES) {
                            Log.d("CamDebug", "OK: Añadiendo foto al contenedor. uriFotoCamara=" + uriFotoCamara);
                            imagenesSeleccionadas.add(uriFotoCamara);
                            agregarImagenAlContenedor(uriFotoCamara);
                        } else {
                            Log.d("CamDebug", "uriFotoCamara es null o límite superado");
                        }
                    } else {
                        // El usuario canceló
                        if (uriFotoCamara != null) {
                            Log.d("CamDebug", "Se canceló, borrando " + uriFotoCamara);
                            getContentResolver().delete(uriFotoCamara, null, null);
                            uriFotoCamara = null;
                        }
                    }
                }
        );


        // Permiso de cámara
        requestCameraPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    if (isGranted) {
                        // Si el permiso se concedió => abrimos cámara
                        lanzarCamaraConFileProvider();
                    } else {
                        Toast.makeText(this, "Permiso de cámara denegado", Toast.LENGTH_SHORT).show();
                    }
                }
        );

        // (2) Lanzador para galería
        galleryLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null && imagenesSeleccionadas.size() < LIMITE_IMAGENES) {
                        imagenesSeleccionadas.add(uri);
                        agregarImagenAlContenedor(uri);
                    } else {
                        Toast.makeText(this, "Máximo 5 imágenes", Toast.LENGTH_SHORT).show();
                    }
                }
        );

        // Permiso para la galería
        requestGalleryPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    if (isGranted) {
                        openGallery();
                    } else {
                        Toast.makeText(this, "Permiso de galería denegado", Toast.LENGTH_SHORT).show();
                    }
                }
        );

        // Botón para agregar imagen (diálogo de opciones)
        btnAgregarImagen.setOnClickListener(v -> mostrarDialogoImagen());
    }

    // ===========================
    // M A N E J O   D E   P E R M I S O S
    // ===========================

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == PERMISSION_CAMERA_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                lanzarCamaraConFileProvider();
            } else {
                Toast.makeText(this, "Permiso de cámara denegado", Toast.LENGTH_SHORT).show();
            }
        }
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

    // =========================================
    //  D I Á L O G O   P A R A   E L E G I R
    // =========================================

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
                        // Opción Cámara
                        if (checkCameraPermission()) {
                            lanzarCamaraConFileProvider();
                        } else {
                            requestCameraPermission();
                        }
                    } else {
                        // Opción Galería
                        if (checkReadImagesPermission()) {
                            openGallery();
                        } else {
                            requestReadImagesPermission();
                        }
                    }
                })
                .create()
                .show();
    }

    // ===========================
    // C Á M A R A   Y   G A L E R Í A
    // ===========================

    /**
     * Abre la galería usando el launcher de getContent().
     */
    private void openGallery() {
        galleryLauncher.launch("image/*");
    }

    /**
     * Usa FileProvider para crear un Uri donde la cámara guardará la foto.
     */
    private void lanzarCamaraConFileProvider() {
        // 1) Creamos un archivo en la carpeta "Pictures" privada de la app
        File directorioFotos = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        String nombreFoto = "foto_" + System.currentTimeMillis() + ".jpg";
        File archivoFoto = new File(directorioFotos, nombreFoto);

        // 2) Uri con la autoridad en duro
         uriFotoCamara = FileProvider.getUriForFile(
                this,
                "com.example.homeservice.fileprovider", // EN DURO, mismo que en Manifest
                archivoFoto
        );

        // 2) Intent de la cámara
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        // Decimos a la cámara que guarde el resultado en uriFotoCamara
        intent.putExtra(MediaStore.EXTRA_OUTPUT, uriFotoCamara);

        // Lanzamos con cameraLauncher
        cameraLauncher.launch(intent);
    }

    // =====================================
    // A G R E G A R   I M Á G E N   A   U I
    // =====================================

    /**
     * Muestra la imagen (Uri) en nuestro contenedorImagenes.
     */
    private void agregarImagenAlContenedor(Uri uri) {
        Log.d("CamDebug", "agregarImagenAlContenedor => " + uri);

        ImageView nuevaImagen = new ImageView(this);
        nuevaImagen.setLayoutParams(new LinearLayout.LayoutParams(200, 200));
        nuevaImagen.setScaleType(ImageView.ScaleType.CENTER_CROP);
        nuevaImagen.setPadding(8, 8, 8, 8);

        // Carga de imagen
        // O bien .setImageURI(uri) si te vale
        // O Glide si quieres un mejor manejo
        nuevaImagen.setImageURI(uri);
        // O: Glide.with(this).load(uri).into(nuevaImagen);

        contenedorImagenes.addView(nuevaImagen, contenedorImagenes.getChildCount());
    }


    // =====================
    //  P U B L I C A R
    // =====================
    private void publicarAnuncio() {
        String titulo = etTitulo.getText().toString().trim();
        String descripcion = etDescripcion.getText().toString().trim();
        String oficio = dropdownCategoria.getText().toString().trim();
        String ciudad = etCiudad.getText().toString().trim();
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        // 1) Validaciones mínimas
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
        // Forzamos mínimo 1 imagen
        if (imagenesSeleccionadas.isEmpty()) {
            Toast.makeText(this, "Debes añadir al menos 1 imagen", Toast.LENGTH_SHORT).show();
            return;
        }

        // 2) Creamos objeto Anuncio
        Anuncio anuncio = new Anuncio(
                titulo,
                descripcion,
                oficio,
                ciudad,
                user.getUid(),
                System.currentTimeMillis()
        );

        // 3) Subimos imágenes al Storage, y cuando termine, guardamos en Firestore
        subirImagenesYCrearAnuncio(anuncio);
    }

    private void subirImagenesYCrearAnuncio(Anuncio anuncio) {
        // Aquí guardaremos las URLs finales
        ArrayList<String> urlsSubidas = new ArrayList<>();

        // Para saber cuántas subidas faltan
        final int totalImagenes = imagenesSeleccionadas.size();
        final int[] contadorExitos = {0};

        // Recorremos cada Uri de la lista
        for (Uri uriImagen : imagenesSeleccionadas) {
            String nombreEnStorage = "anuncios/" + UUID.randomUUID().toString() + ".jpg";
            StorageReference storageRef = FirebaseStorage.getInstance().getReference().child(nombreEnStorage);

            storageRef.putFile(uriImagen)
                    .addOnSuccessListener(taskSnapshot -> {
                        // Al subir con éxito, pedimos su downloadURL
                        storageRef.getDownloadUrl().addOnSuccessListener(downloadUri -> {
                            urlsSubidas.add(downloadUri.toString());

                            // Actualizamos contador
                            contadorExitos[0]++;
                            // Si ya se han subido TODAS => guardamos el anuncio
                            if (contadorExitos[0] == totalImagenes) {
                                anuncio.setListaImagenes(urlsSubidas);
                                guardarAnuncioEnFirestore(anuncio);
                            }
                        });
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Error al subir imagen: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    });
        }
    }

    private void guardarAnuncioEnFirestore(Anuncio anuncio) {
        FirestoreHelper firestoreHelper = new FirestoreHelper();
        firestoreHelper.crearAnuncio(
                anuncio,
                id -> {
                    Toast.makeText(this, "Anuncio publicado con ID: " + id, Toast.LENGTH_SHORT).show();
                    // Redirigir a MainActivity
                    startActivity(new Intent(PublicarAnuncio.this, MainActivity.class));
                    finish();
                },
                error -> {
                    Toast.makeText(this, "Error al guardar anuncio: " + error.getMessage(),
                            Toast.LENGTH_SHORT).show();
                }
        );
    }
}
