package com.example.reciclaje11;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.media.Image;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.OptIn;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ExperimentalGetImage;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.core.content.ContextCompat;

import com.example.reciclaje11.databinding.ActivityCameraBinding;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.objects.DetectedObject;
import com.google.mlkit.vision.objects.ObjectDetection;
import com.google.mlkit.vision.objects.ObjectDetector;
import com.google.mlkit.vision.objects.defaults.ObjectDetectorOptions;

import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CameraActivity extends AppCompatActivity {

    private static final String PHOTO = "temp_capture.jpg";

    private ActivityCameraBinding b;
    private ImageCapture capture;
    private ObjectDetector detector;
    private TfliteClassifier classifier;
    private ExecutorService executor;
    private boolean busy;

    @Override
    protected void onCreate(Bundle s) {
        super.onCreate(s);
        b = ActivityCameraBinding.inflate(getLayoutInflater());
        setContentView(b.getRoot());

        executor = Executors.newSingleThreadExecutor();
        detector = ObjectDetection.getClient(new ObjectDetectorOptions.Builder()
                .setDetectorMode(ObjectDetectorOptions.STREAM_MODE)
                .enableMultipleObjects()
                .enableClassification()
                .build());

        try {
            classifier = new TfliteClassifier(this);
        } catch (Exception e) {
            mostrarEstado("Modelo no disponible", "Revisa converted_tflite-2/model_unquant.tflite", "");
            mostrarToast("No se pudo cargar el modelo");
        }

        if (classifier != null) {
            mostrarEstado("Cámara lista", "", "");
        }
        b.btnCapture.setOnClickListener(v -> tomarFoto());
        iniciarCamara();
    }

    private void iniciarCamara() {
        ProcessCameraProvider.getInstance(this).addListener(() -> {
            try {
                ProcessCameraProvider provider = ProcessCameraProvider.getInstance(this).get();

                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(b.viewFinder.getSurfaceProvider());

                capture = new ImageCapture.Builder()
                        .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                        .build();

                ImageAnalysis analysis = new ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build();
                analysis.setAnalyzer(executor, this::analizarFrame);

                provider.unbindAll();
                provider.bindToLifecycle(this, CameraSelector.DEFAULT_BACK_CAMERA, preview, capture, analysis);
            } catch (Exception e) {
                mostrarToast("No se pudo iniciar la cámara");
            }
        }, ContextCompat.getMainExecutor(this));
    }

    @OptIn(markerClass = ExperimentalGetImage.class)
    private void analizarFrame(@NonNull ImageProxy proxy) {
        if (busy || classifier == null) {
            proxy.close();
            return;
        }

        Image image = proxy.getImage();
        if (image == null) {
            proxy.close();
            return;
        }

        busy = true;
        int rotation = proxy.getImageInfo().getRotationDegrees();
        int width = rotation == 90 || rotation == 270 ? proxy.getHeight() : proxy.getWidth();
        int height = rotation == 90 || rotation == 270 ? proxy.getWidth() : proxy.getHeight();

        detector.process(InputImage.fromMediaImage(image, rotation))
                .addOnSuccessListener(objects -> mostrarResultadoEnVivo(objects, proxy, width, height))
                .addOnFailureListener(e -> mostrarEstado("Sin lectura", "", ""))
                .addOnCompleteListener(task -> {
                    busy = false;
                    proxy.close();
                });
    }

    private void mostrarResultadoEnVivo(List<DetectedObject> objects, ImageProxy proxy, int imageWidth, int imageHeight) {
        Bitmap bitmap = yuvABitmap(proxy);
        if (bitmap == null) return;

        DetectedObject object = objects.isEmpty() ? null : objetoMasGrande(objects);
        Rect box = object == null ? null : object.getBoundingBox();
        TfliteClassifier.Result result = classifier.clasificar(bitmap, box);
        String objectName = nombreDelObjeto(object);

        if (box != null) {
            runOnUiThread(() -> b.overlayView.mostrarCajas(Collections.singletonList(
                    new OverlayView.Box(box, result.label, RecyclingClassifier.colorPara(result.label))
            ), imageWidth, imageHeight));
        } else {
            runOnUiThread(() -> b.overlayView.mostrarCajas(Collections.emptyList(), 1, 1));
        }

        mostrarEstado(
                result.label,
                String.format(Locale.getDefault(), "%.0f%% · %s", result.confidence * 100f, objectName),
                RecyclingClassifier.instruccionPara(result.label)
        );
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

    private void tomarFoto() {
        if (capture == null) return;

        capture.takePicture(ContextCompat.getMainExecutor(this), new ImageCapture.OnImageCapturedCallback() {
            @Override
            public void onCaptureSuccess(@NonNull ImageProxy img) {
                Bitmap bitmap = jpegABitmap(img);
                img.close();

                if (bitmap == null) {
                    mostrarToast("No se pudo leer la foto");
                    return;
                }

                try (FileOutputStream out = openFileOutput(PHOTO, MODE_PRIVATE)) {
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 95, out);
                    startActivity(new Intent(CameraActivity.this, ResultActivity.class)
                            .putExtra("image_path", PHOTO));
                } catch (Exception e) {
                    mostrarToast("No se pudo guardar la foto");
                }
            }

            @Override
            public void onError(@NonNull ImageCaptureException e) {
                mostrarToast("No se pudo tomar la foto");
            }
        });
    }

    private Bitmap jpegABitmap(ImageProxy image) {
        try {
            ByteBuffer buffer = image.getPlanes()[0].getBuffer();
            byte[] bytes = new byte[buffer.remaining()];
            buffer.get(bytes);
            Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
            return rotar(bitmap, image.getImageInfo().getRotationDegrees());
        } catch (Exception e) {
            return null;
        }
    }

    private Bitmap yuvABitmap(ImageProxy image) {
        try {
            byte[] nv21 = convertirANv21(image);
            YuvImage yuv = new YuvImage(nv21, ImageFormat.NV21, image.getWidth(), image.getHeight(), null);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            yuv.compressToJpeg(new Rect(0, 0, image.getWidth(), image.getHeight()), 80, out);
            Bitmap bitmap = BitmapFactory.decodeByteArray(out.toByteArray(), 0, out.size());
            return rotar(bitmap, image.getImageInfo().getRotationDegrees());
        } catch (Exception e) {
            return null;
        }
    }

    private byte[] convertirANv21(ImageProxy image) {
        int width = image.getWidth();
        int height = image.getHeight();
        byte[] nv21 = new byte[width * height * 3 / 2];

        ImageProxy.PlaneProxy yPlane = image.getPlanes()[0];
        ImageProxy.PlaneProxy uPlane = image.getPlanes()[1];
        ImageProxy.PlaneProxy vPlane = image.getPlanes()[2];

        ByteBuffer y = yPlane.getBuffer();
        ByteBuffer u = uPlane.getBuffer();
        ByteBuffer v = vPlane.getBuffer();

        int offset = 0;
        for (int row = 0; row < height; row++) {
            int rowStart = row * yPlane.getRowStride();
            for (int col = 0; col < width; col++) {
                nv21[offset++] = y.get(rowStart + col * yPlane.getPixelStride());
            }
        }

        int chromaHeight = height / 2;
        int chromaWidth = width / 2;
        for (int row = 0; row < chromaHeight; row++) {
            int uRow = row * uPlane.getRowStride();
            int vRow = row * vPlane.getRowStride();
            for (int col = 0; col < chromaWidth; col++) {
                nv21[offset++] = v.get(vRow + col * vPlane.getPixelStride());
                nv21[offset++] = u.get(uRow + col * uPlane.getPixelStride());
            }
        }

        return nv21;
    }

    private Bitmap rotar(Bitmap bitmap, int degrees) {
        if (bitmap == null || degrees == 0) return bitmap;
        Matrix matrix = new Matrix();
        matrix.postRotate(degrees);
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
    }

    private void mostrarEstado(String title, String detail, String instruction) {
        runOnUiThread(() -> {
            b.tvLiveCategory.setText(title);
            b.tvLiveDetails.setText(detail == null ? "" : detail);
            b.tvLiveInstruction.setText(instruction == null ? "" : instruction);
        });
    }

    private void mostrarToast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (detector != null) detector.close();
        if (classifier != null) classifier.close();
        if (executor != null) executor.shutdown();
    }
}
