package com.example.reciclaje11;

import android.graphics.Color;

import java.text.Normalizer;
import java.util.Locale;

public final class RecyclingClassifier {

    private RecyclingClassifier() {
    }

    public static String normalizarCategoria(String label) {
        String value = limpiarTexto(label);

        if (value.contains("plastico") || value.contains("plastic")) {
            return "Plastico";
        }
        if (value.contains("vidrio") || value.contains("glass")) {
            return "Vidrio";
        }
        if (value.contains("papel") || value.contains("paper")) {
            return "Papel";
        }
        if (value.contains("carton") || value.contains("cardboard")) {
            return "Carton";
        }
        if (value.contains("metal") || value.contains("lata") || value.contains("can")) {
            return "Metal";
        }
        if (value.contains("basura") || value.contains("trash") || value.contains("garbage") || value.contains("waste")) {
            return "Basura";
        }

        return label == null || label.trim().isEmpty() ? "Sin categoria" : label.trim();
    }

    public static String instruccionPara(String category) {
        switch (limpiarTexto(category)) {
            case "plastico":
            case "metal":
                return "Va en el contenedor amarillo";
            case "vidrio":
                return "Va en el contenedor verde";
            case "papel":
                return "Va en el contenedor azul para papel";
            case "carton":
                return "Va en el contenedor azul para carton";
            case "basura":
                return "Va en el contenedor de basura";
            default:
                return "Revisa la categoria antes de reciclar.";
        }
    }

    public static int colorPara(String category) {
        switch (limpiarTexto(category)) {
            case "plastico":
            case "metal":
                return Color.rgb(245, 196, 0);
            case "vidrio":
                return Color.rgb(56, 142, 60);
            case "papel":
                return Color.rgb(3, 169, 244);
            case "carton":
                return Color.rgb(25, 118, 210);
            case "basura":
                return Color.rgb(97, 97, 97);
            default:
                return Color.rgb(211, 47, 47);
        }
    }

    public static int fondoPara(String category) {
        switch (limpiarTexto(category)) {
            case "plastico":
            case "metal":
            case "vidrio":
            case "papel":
            case "carton":
            case "basura":
                return Color.parseColor("#DDEFE3");
            default:
                return Color.parseColor("#E8F2EC");
        }
    }

    private static String limpiarTexto(String value) {
        if (value == null) {
            return "";
        }
        String lower = value.toLowerCase(Locale.ROOT);
        return Normalizer.normalize(lower, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
    }
}
