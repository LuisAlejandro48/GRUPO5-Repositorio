package com.example.reciclaje11;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.reciclaje11.databinding.ActivityMainBinding;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    abrirCamara();
                } else {
                    mostrarAvisoPermiso();
                }
            });

    private final ActivityResultLauncher<String> getContentLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    abrirResultadoConUri(uri);
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.btnCamera.setOnClickListener(v -> revisarPermisoCamara());
        binding.btnGallery.setOnClickListener(v -> getContentLauncher.launch("image/*"));
        binding.btnHistory.setOnClickListener(v -> startActivity(new Intent(this, HistorialActivity.class)));
    }

    private void revisarPermisoCamara() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            abrirCamara();
        } else {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA);
        }
    }

    private void abrirCamara() {
        startActivity(new Intent(this, CameraActivity.class));
    }

    private void abrirResultadoConUri(Uri uri) {
        Intent intent = new Intent(this, ResultActivity.class);
        intent.putExtra("image_uri", uri.toString());
        startActivity(intent);
    }

    private void mostrarAvisoPermiso() {
        new AlertDialog.Builder(this)
                .setTitle("Permiso necesario")
                .setMessage("Se requiere el permiso de cámara para identificar objetos en tiempo real.")
                .setPositiveButton("Ir a Ajustes", (dialog, which) -> {
                    Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                    Uri uri = Uri.fromParts("package", getPackageName(), null);
                    intent.setData(uri);
                    startActivity(intent);
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }
}
