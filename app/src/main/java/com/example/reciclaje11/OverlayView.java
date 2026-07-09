package com.example.reciclaje11;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

public class OverlayView extends View {

    public static class Box {
        public Rect rect;
        public String label;
        public int color;

        public Box(Rect rect, String label, int color) {
            this.rect = rect;
            this.label = label;
            this.color = color;
        }
    }

    private final List<Box> boxes = new ArrayList<>();
    private final Paint boxPaint = new Paint();
    private final Paint textPaint = new Paint();
    private final Paint backgroundPaint = new Paint();
    private int imageWidth;
    private int imageHeight;

    public OverlayView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        boxPaint.setStyle(Paint.Style.STROKE);
        boxPaint.setStrokeWidth(8f);
        
        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(40f);
        
        backgroundPaint.setStyle(Paint.Style.FILL);
    }

    public void mostrarCajas(List<Box> newBoxes, int imageWidth, int imageHeight) {
        boxes.clear();
        boxes.addAll(newBoxes);
        this.imageWidth = imageWidth;
        this.imageHeight = imageHeight;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (imageWidth <= 0 || imageHeight <= 0) {
            return;
        }

        float scale = Math.min(getWidth() / (float) imageWidth, getHeight() / (float) imageHeight);
        float dx = (getWidth() - imageWidth * scale) / 2f;
        float dy = (getHeight() - imageHeight * scale) / 2f;

        for (Box box : boxes) {
            float left = dx + box.rect.left * scale;
            float top = dy + box.rect.top * scale;
            float right = dx + box.rect.right * scale;
            float bottom = dy + box.rect.bottom * scale;

            boxPaint.setColor(box.color);
            canvas.drawRect(left, top, right, bottom, boxPaint);

            String text = box.label;
            float textWidth = textPaint.measureText(text);
            float textHeight = textPaint.getTextSize();
            float labelTop = Math.max(0f, top - textHeight - 12f);

            backgroundPaint.setColor(box.color);
            canvas.drawRect(
                left,
                labelTop,
                Math.min(getWidth(), left + textWidth + 20f),
                labelTop + textHeight + 12f,
                backgroundPaint
            );

            canvas.drawText(text, left + 10f, labelTop + textHeight, textPaint);
        }
    }
}
