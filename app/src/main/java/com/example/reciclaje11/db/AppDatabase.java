package com.example.reciclaje11.db;

import android.content.Context;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

@Database(entities = {ObjetoReciclado.class}, version = 1, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {
    public abstract ObjetoDao objetoDao();

    private static volatile AppDatabase INSTANCE;

    public static AppDatabase obtenerBaseDeDatos(final Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                                    AppDatabase.class, "reciclaje_db")
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}
