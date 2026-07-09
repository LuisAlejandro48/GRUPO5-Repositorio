package com.example.reciclaje11;

import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.reciclaje11.databinding.ActivityHistorialBinding;
import com.example.reciclaje11.db.AppDatabase;
import com.example.reciclaje11.db.ObjetoDao;
import com.example.reciclaje11.db.ObjetoReciclado;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class HistorialActivity extends AppCompatActivity {

    private ActivityHistorialBinding binding;
    private HistorialAdapter adapter;
    private ObjetoDao objetoDao;
    private ExecutorService databaseExecutor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityHistorialBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        binding.toolbar.setNavigationOnClickListener(v -> finish());

        objetoDao = AppDatabase.obtenerBaseDeDatos(this).objetoDao();
        databaseExecutor = Executors.newSingleThreadExecutor();
        prepararLista();
        cargarHistorial();
    }

    private void prepararLista() {
        adapter = new HistorialAdapter(new ArrayList<>(), this::mostrarDialogoEliminar);
        binding.rvHistorial.setLayoutManager(new LinearLayoutManager(this));
        binding.rvHistorial.setAdapter(adapter);
    }

    private void cargarHistorial() {
        databaseExecutor.execute(() -> {
            List<ObjetoReciclado> items = objetoDao.obtenerTodos();
            int plastico = objetoDao.contarPorCategoria("Plastico");
            int vidrio = objetoDao.contarPorCategoria("Vidrio");
            int papel = objetoDao.contarPorCategoria("Papel");
            int carton = objetoDao.contarPorCategoria("Carton");
            int metal = objetoDao.contarPorCategoria("Metal");
            int basura = objetoDao.contarPorCategoria("Basura");

            runOnUiThread(() -> {
                adapter.actualizarDatos(items);
                actualizarResumen(plastico, vidrio, papel, carton, metal, basura);
            });
        });
    }

    private void actualizarResumen(int plastico, int vidrio, int papel, int carton, int metal, int basura) {
        String summary = String.format(Locale.getDefault(), "Plástico: %d, Vidrio: %d, Papel: %d, Cartón: %d, Metal: %d, Basura: %d",
                plastico, vidrio, papel, carton, metal, basura);
        binding.tvSummary.setText(summary);
    }

    private void mostrarDialogoEliminar(ObjetoReciclado item) {
        new AlertDialog.Builder(this)
                .setTitle("Eliminar registro")
                .setMessage("¿Estás seguro de que deseas eliminar este escaneo del historial?")
                .setPositiveButton("Eliminar", (dialog, which) -> eliminarRegistro(item))
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void eliminarRegistro(ObjetoReciclado item) {
        databaseExecutor.execute(() -> {
            if (item.rutaImagen != null) {
                File file = new File(item.rutaImagen);
                if (file.exists()) {
                    file.delete();
                }
            }
            objetoDao.eliminar(item);
            runOnUiThread(() -> {
                Toast.makeText(this, "Eliminado", Toast.LENGTH_SHORT).show();
                cargarHistorial();
            });
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (databaseExecutor != null) {
            databaseExecutor.shutdown();
        }
    }
}
