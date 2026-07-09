package com.example.reciclaje11;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.reciclaje11.databinding.ActivityResultBinding;
import com.example.reciclaje11.db.AppDatabase;
import com.example.reciclaje11.db.ObjetoReciclado;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.objects.DetectedObject;
import com.google.mlkit.vision.objects.ObjectDetection;
import com.google.mlkit.vision.objects.ObjectDetector;
import com.google.mlkit.vision.objects.defaults.ObjectDetectorOptions;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ResultActivity extends AppCompatActivity {

    private ActivityResultBinding b;
    private Bitmap bitmap;
    private ObjectDetector detector;
    private TfliteClassifier classifier;
    private ExecutorService executor;

    private String category = "";
    private String objectName = "";
    private float confidence = 0f;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        b = ActivityResultBinding.inflate(getLayoutInflater());
        setContentView(b.getRoot());

        executor = Executors.newSingleThreadExecutor();
        b.btnSave.setEnabled(false);
        b.btnSave.setOnClickListener(v -> guardarResultado());
        b.btnBack.setOnClickListener(v -> finish());

        try {
            classifier = new TfliteClassifier(this);
            detector = ObjectDetection.getClient(new ObjectDetectorOptions.Builder()
                    .setDetectorMode(ObjectDetectorOptions.SINGLE_IMAGE_MODE)
                    .enableMultipleObjects()
                    .enableClassification()
                    .build());
            cargarImagen();
        } catch (Exception e) {
            mostrarMensaje("No se pudo cargar el modelo. Revisa converted_tflite-2/model_unquant.tflite y labels.txt.");
        }
    }

    private void cargarImagen() {
        try {
            if (getIntent().hasExtra("image_uri")) {
                Uri uri = Uri.parse(getIntent().getStringExtra("image_uri"));
                try (InputStream inputStream = getContentResolver().openInputStream(uri)) {
                    bitmap = BitmapFactory.decodeStream(inputStream);
                }
            } else if (getIntent().hasExtra("image_path")) {
                try (InputStream inputStream = openFileInput(getIntent().getStringExtra("image_path"))) {
                    bitmap = BitmapFactory.decodeStream(inputStream);
                }
            }

            if (bitmap == null) {
                mostrarMensaje("No se pudo cargar la imagen.");
                return;
            }

            b.ivResult.setImageBitmap(bitmap);
            detectarObjeto(bitmap);
        } catch (Exception e) {
            mostrarMensaje("No se pudo cargar la imagen.");
        }
    }

    private void detectarObjeto(Bitmap image) {
        b.tvCategory.setText("Analizando");
        b.tvConfidence.setText("");
        b.tvLabeling.setText("");
        b.tvWarning.setVisibility(View.GONE);

        detector.process(InputImage.fromBitmap(image, 0))
                .addOnSuccessListener(objects -> mostrarResultado(image, objects))
                .addOnFailureListener(e -> mostrarMensaje("No se pudo analizar la imagen."));
    }

    private void mostrarResultado(Bitmap image, List<DetectedObject> objects) {
        DetectedObject object = objects.isEmpty() ? null : objetoMasGrande(objects);
        Rect box = object == null ? null : object.getBoundingBox();
        TfliteClassifier.Result result = classifier.clasificar(image, box);

        category = result.label;
        confidence = result.confidence;
        objectName = nombreDelObjeto(object);

        if (box != null) {
            b.overlayView.mostrarCajas(
                    Collections.singletonList(new OverlayView.Box(box, category, RecyclingClassifier.colorPara(category))),
                    image.getWidth(),
                    image.getHeight()
            );
        } else {
            b.overlayView.mostrarCajas(Collections.emptyList(), 1, 1);
        }

        b.tvCategory.setText(category);
        b.tvConfidence.setText(String.format(Locale.getDefault(), "%.0f%%", confidence * 100f));
        b.tvLabeling.setText(objectName);
        b.tvInstruction.setText(RecyclingClassifier.instruccionPara(category));
        b.tvInstruction.setBackgroundColor(RecyclingClassifier.fondoPara(category));
        b.btnSave.setEnabled(true);
    }

    private DetectedObject objetoMasGrande(List<DetectedObject> objects) {
        DetectedObject best = objects.get(0);
        int bestArea = areaDe(best.getBoundingBox());
        for (DetectedObject object : objects) {
            int area = areaDe(object.getBoundingBox());
            if (area > bestArea) {
                best = object;
                bestArea = area;
            }
        }
        return best;
    }

    private int areaDe(Rect rect) {
        return Math.max(0, rect.width()) * Math.max(0, rect.height());
    }

    private void guardarResultado() {
        if (bitmap == null || category.isEmpty()) {
            return;
        }

        b.btnSave.setEnabled(false);
        executor.execute(() -> {
            try {
                String filename = "scan_" + System.currentTimeMillis() + ".jpg";
                File file = new File(getFilesDir(), filename);
                try (FileOutputStream outputStream = new FileOutputStream(file)) {
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 90, outputStream);
                }

                AppDatabase.obtenerBaseDeDatos(this).objetoDao().insertar(new ObjetoReciclado(
                        category,
                        confidence,
                        objectName,
                        System.currentTimeMillis(),
                        file.getAbsolutePath()
                ));

                runOnUiThread(() -> {
                    Toast.makeText(this, "Guardado", Toast.LENGTH_SHORT).show();
                    finish();
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    b.btnSave.setEnabled(true);
                    Toast.makeText(this, "No se pudo guardar", Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void mostrarMensaje(String message) {
        b.tvCategory.setText("Sin resultado");
        b.tvConfidence.setText("");
        b.tvLabeling.setText("");
        b.tvInstruction.setText(message);
        b.tvWarning.setVisibility(View.GONE);
        b.btnSave.setEnabled(false);
    }

    private String nombreDelObjeto(DetectedObject object) {
        if (object == null || object.getLabels().isEmpty()) {
            return "No identificado";
        }

        DetectedObject.Label label = object.getLabels().get(0);
        for (DetectedObject.Label item : object.getLabels()) {
            if (item.getConfidence() > label.getConfidence()) {
                label = item;
            }
        }

        String name = label.getText();
        if (name == null || name.trim().isEmpty()) {
            return "No identificado";
        }

        switch (name.toLowerCase(Locale.ROOT)) {
            case "food":
                return "Comida";
            case "fashion good":
                return "Ropa o accesorio";
            case "home good":
                return "Objeto del hogar";
            case "place":
                return "Lugar";
            case "plant":
                return "Planta";
            default:
                return name;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (detector != null) detector.close();
        if (classifier != null) classifier.close();
        if (executor != null) executor.shutdown();
    }
}
