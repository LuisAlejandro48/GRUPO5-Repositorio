package com.example.reciclaje11;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Rect;

import org.tensorflow.lite.Interpreter;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;

public class TfliteClassifier implements AutoCloseable {

    private static final String MODEL_PATH = "converted_tflite-2/model_unquant.tflite";
    private static final String LABELS_PATH = "converted_tflite-2/labels.txt";

    public static class Result {
        public final String label;
        public final float confidence;

        Result(String label, float confidence) {
            this.label = label;
            this.confidence = confidence;
        }
    }

    private final Interpreter interpreter;
    private final List<String> labels;
    private final int inputWidth;
    private final int inputHeight;
    private final int inputChannels;
    private final int outputCount;

    public TfliteClassifier(Context context) throws Exception {
        interpreter = new Interpreter(cargarModelo(context));
        labels = cargarEtiquetas(context);

        int[] inputShape = interpreter.getInputTensor(0).shape();
        inputHeight = inputShape[1];
        inputWidth = inputShape[2];
        inputChannels = inputShape[3];

        int[] outputShape = interpreter.getOutputTensor(0).shape();
        outputCount = outputShape[outputShape.length - 1];
    }

    public Result clasificar(Bitmap bitmap, Rect box) {
        Bitmap target = recortar(bitmap, box);
        Bitmap resized = Bitmap.createScaledBitmap(target, inputWidth, inputHeight, true);

        ByteBuffer input = ByteBuffer.allocateDirect(4 * inputWidth * inputHeight * inputChannels);
        input.order(ByteOrder.nativeOrder());

        int[] pixels = new int[inputWidth * inputHeight];
        resized.getPixels(pixels, 0, inputWidth, 0, 0, inputWidth, inputHeight);

        for (int pixel : pixels) {
            input.putFloat(((pixel >> 16) & 0xFF) / 255f);
            input.putFloat(((pixel >> 8) & 0xFF) / 255f);
            input.putFloat((pixel & 0xFF) / 255f);
        }

        input.rewind();

        float[][] output = new float[1][outputCount];
        interpreter.run(input, output);

        int bestIndex = 0;
        float bestScore = output[0][0];
        for (int i = 1; i < outputCount; i++) {
            if (output[0][i] > bestScore) {
                bestScore = output[0][i];
                bestIndex = i;
            }
        }

        int metalIndex = buscarEtiquetaMetal();
        if (metalIndex >= 0 && metalIndex < outputCount && metalIndex != bestIndex) {
            float metalScore = output[0][metalIndex];
            if (metalScore >= 0.18f && bestScore - metalScore <= 0.18f) {
                bestIndex = metalIndex;
                bestScore = metalScore;
            }
        }

        String label = bestIndex < labels.size() ? labels.get(bestIndex) : "Clase " + bestIndex;
        return new Result(RecyclingClassifier.normalizarCategoria(label), bestScore);
    }

    private int buscarEtiquetaMetal() {
        for (int i = 0; i < labels.size(); i++) {
            String label = labels.get(i).toLowerCase();
            if (label.contains("metal") || label.contains("lata") || label.contains("can")) {
                return i;
            }
        }
        return -1;
    }

    private Bitmap recortar(Bitmap bitmap, Rect box) {
        if (box == null) {
            return bitmap;
        }

        int left = Math.max(0, box.left);
        int top = Math.max(0, box.top);
        int right = Math.min(bitmap.getWidth(), box.right);
        int bottom = Math.min(bitmap.getHeight(), box.bottom);

        if (right <= left || bottom <= top) {
            return bitmap;
        }

        return Bitmap.createBitmap(bitmap, left, top, right - left, bottom - top);
    }

    private ByteBuffer cargarModelo(Context context) throws Exception {
        try (InputStream inputStream = context.getAssets().open(MODEL_PATH);
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[4096];
            int read;
            while ((read = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, read);
            }

            byte[] model = outputStream.toByteArray();
            ByteBuffer byteBuffer = ByteBuffer.allocateDirect(model.length);
            byteBuffer.order(ByteOrder.nativeOrder());
            byteBuffer.put(model);
            byteBuffer.rewind();
            return byteBuffer;
        }
    }

    private List<String> cargarEtiquetas(Context context) throws Exception {
        List<String> result = new ArrayList<>();
        try (InputStream inputStream = context.getAssets().open(LABELS_PATH);
             BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String label = line.trim().replaceFirst("^\\d+\\s+", "");
                if (!label.isEmpty()) {
                    result.add(label);
                }
            }
        }
        return result;
    }

    @Override
    public void close() {
        interpreter.close();
    }
}
