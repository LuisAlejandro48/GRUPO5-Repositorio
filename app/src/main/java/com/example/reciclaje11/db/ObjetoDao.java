package com.example.reciclaje11.db;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

@Dao
public interface ObjetoDao {
    @Insert
    void insertar(ObjetoReciclado objeto);

    @Query("SELECT * FROM objetos_reciclados ORDER BY fechaEscaneo DESC")
    List<ObjetoReciclado> obtenerTodos();

    @Delete
    void eliminar(ObjetoReciclado objeto);

    @Query("SELECT COUNT(*) FROM objetos_reciclados WHERE categoria = :cat")
    int contarPorCategoria(String cat);
}
