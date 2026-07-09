package com.example.reciclaje11.db;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "objetos_reciclados")
public class ObjetoReciclado {
    @PrimaryKey(autoGenerate = true)
    public int id;
    
    public String categoria;
    public float confianza;
    public String etiquetaImageLabeling;
    public long fechaEscaneo;
    public String rutaImagen;

    public ObjetoReciclado(String categoria, float confianza, String etiquetaImageLabeling, long fechaEscaneo, String rutaImagen) {
        this.categoria = categoria;
        this.confianza = confianza;
        this.etiquetaImageLabeling = etiquetaImageLabeling;
        this.fechaEscaneo = fechaEscaneo;
        this.rutaImagen = rutaImagen;
    }
}
