package com.example.homeservice.ui.Publicar;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
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
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import com.bumptech.glide.Glide;
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
    private Uri uriFotoCamara;

    private EditText etTitulo, etDescripcion;
    private AutoCompleteTextView dropdownCategoria;
    private TextView tvUbicacion;
    // Para mostrar un mapa estático
    private ImageView ivMapa;

    // Guardaremos la lat/lon del usuario
    private Double latUser = null;
    private Double lonUser = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.publicar_anuncio);

        contenedorImagenes = findViewById(R.id.contenedorImagenes);
        ImageView btnAgregarImagen = findViewById(R.id.btnAgregarImagen);

        etTitulo = findViewById(R.id.etTitulo);
        etDescripcion = findViewById(R.id.etDescripcion);
        tvUbicacion = findViewById(R.id.tvUbicacion);


        dropdownCategoria = findViewById(R.id.dropdownCategoria);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_dropdown_item_1line,
                getResources().getStringArray(R.array.categorias_array)
        );
        dropdownCategoria.setAdapter(adapter);
        dropdownCategoria.setKeyListener(null); // Desactiva la escritura manual
        dropdownCategoria.setFocusable(false);  // Desactiva el foco manual
        dropdownCategoria.setOnClickListener(v -> dropdownCategoria.showDropDown());

        // Botón para publicar anuncio
        Button btnPublicar = findViewById(R.id.btnPublicar);
        btnPublicar.setOnClickListener(v -> {
            publicarAnuncio();
        });

        // ================
        // L A U N C H E R S
        // ================
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

        btnAgregarImagen.setOnClickListener(v -> mostrarDialogoImagen());

        // NUEVO: leer la info del usuario para mostrar su ciudad y el mapa
        cargarDatosUsuarioYMostrarMapa();
    }

    private void cargarDatosUsuarioYMostrarMapa() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Log.w("PublicarAnuncio", "No hay usuario autenticado, no se puede leer la ubicación");
            return;
        }
        String userId = user.getUid();
        FirestoreHelper firestoreHelper = new FirestoreHelper();
        firestoreHelper.leerUsuario(
                userId,
                usuarioLeido -> {
                    if (usuarioLeido != null) {
                        // Asignamos la ciudad al TextView, en caso de que exista
                        if (usuarioLeido.getLocalizacion() != null && !usuarioLeido.getLocalizacion().isEmpty()) {
                            tvUbicacion.setText(usuarioLeido.getLocalizacion());
                        }
                        // Guardamos lat/lon en variables locales
                        latUser = usuarioLeido.getLat();
                        lonUser = usuarioLeido.getLon();

                    }
                },
                error -> {
                    Log.e("PublicarAnuncio", "Error leyendo usuario => " + error.getMessage());
                }
        );
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
                        if (checkCameraPermission()) {
                            lanzarCamaraConFileProvider();
                        } else {
                            requestCameraPermission();
                        }
                    } else {
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

    private void openGallery() {
        galleryLauncher.launch("image/*");
    }

    private void lanzarCamaraConFileProvider() {
        File directorioFotos = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        String nombreFoto = "foto_" + System.currentTimeMillis() + ".jpg";
        File archivoFoto = new File(directorioFotos, nombreFoto);

        uriFotoCamara = FileProvider.getUriForFile(
                this,
                "com.example.homeservice.fileprovider",
                archivoFoto
        );

        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, uriFotoCamara);
        cameraLauncher.launch(intent);
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

    private void agregarImagenAlContenedor(Uri uri) {
        ImageView nuevaImagen = new ImageView(this);
        nuevaImagen.setLayoutParams(new LinearLayout.LayoutParams(200, 200));
        nuevaImagen.setScaleType(ImageView.ScaleType.CENTER_CROP);
        nuevaImagen.setPadding(8, 8, 8, 8);

        nuevaImagen.setImageURI(uri);
        contenedorImagenes.addView(nuevaImagen, contenedorImagenes.getChildCount());
    }

    // =====================
    //  P U B L I C A R
    // =====================
    private void publicarAnuncio() {
        String titulo = etTitulo.getText().toString().trim();
        String descripcion = etDescripcion.getText().toString().trim();
        String oficio = dropdownCategoria.getText().toString().trim();
        String ciudad = tvUbicacion.getText().toString().trim();

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

        if (descripcion.isEmpty()) {
            etDescripcion.setError("Descripción requerida");
            etDescripcion.requestFocus();
            return;
        }
        if (user == null) {
            Toast.makeText(this, "Usuario no autenticado", Toast.LENGTH_SHORT).show();
            return;
        }
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

        // NUEVO: Asignar lat/lon del user
        // (si user introdujo otra ciudad manual, no lo controlamos aquí
        //  Se quedaría con latUser/lonUser => la localización real,
        //  y la "ciudad" del EditText quizás no coincide.
        //  En un futuro podrías hacer forward geocode o algo)
        if (latUser != null && lonUser != null) {
            anuncio.setLatitud(latUser);
            anuncio.setLongitud(lonUser);
        }

        // 3) Subir imágenes al Storage, y cuando termine, guardamos en Firestore
        subirImagenesYCrearAnuncio(anuncio);
    }

    private void subirImagenesYCrearAnuncio(Anuncio anuncio) {
        ArrayList<String> urlsSubidas = new ArrayList<>();
        final int totalImagenes = imagenesSeleccionadas.size();
        final int[] contadorExitos = {0};

        for (Uri uriImagen : imagenesSeleccionadas) {
            String nombreEnStorage = "anuncios/" + UUID.randomUUID().toString() + ".jpg";
            StorageReference storageRef = FirebaseStorage.getInstance().getReference().child(nombreEnStorage);

            storageRef.putFile(uriImagen)
                    .addOnSuccessListener(taskSnapshot -> {
                        storageRef.getDownloadUrl().addOnSuccessListener(downloadUri -> {
                            urlsSubidas.add(downloadUri.toString());
                            contadorExitos[0]++;

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
