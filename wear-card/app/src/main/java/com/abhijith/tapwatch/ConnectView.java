package com.abhijith.tapwatch;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.os.SystemClock;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.View;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.common.BitMatrix;

import java.util.EnumMap;
import java.util.Map;

public class ConnectView extends View {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final boolean hceHardware;
    private final boolean nfcPresent;
    private final boolean nfcEnabled;
    private final boolean roundScreen;
    private Bitmap qrBitmap;
    private boolean qrMode = false;
    private long downAt;

    private static final int BG = Color.rgb(5, 7, 12);
    private static final int TEXT = Color.rgb(247, 249, 252);
    private static final int MUTED = Color.rgb(147, 160, 179);
    private static final int CYAN = Color.rgb(110, 231, 255);
    private static final int GREEN = Color.rgb(124, 247, 194);
    private static final int AMBER = Color.rgb(255, 199, 92);

    public ConnectView(Context context, boolean hceHardware, boolean nfcPresent, boolean nfcEnabled) {
        super(context);
        this.hceHardware = hceHardware;
        this.nfcPresent = nfcPresent;
        this.nfcEnabled = nfcEnabled;
        this.roundScreen = getResources().getConfiguration().isScreenRound();
        setFocusable(true);
        setContentDescription("TapWatch digital business card. Tap the screen to show a full-size QR code.");
        qrBitmap = createQr(CardConfig.CARD_URL, 720);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawColor(BG);
        drawGlow(canvas);
        if (qrMode) drawQrMode(canvas); else drawHandoffMode(canvas);
    }

    private void drawGlow(Canvas canvas) {
        int w = getWidth();
        int h = getHeight();
        paint.setShader(new LinearGradient(0, 0, w, h,
                new int[]{Color.argb(40, 110, 231, 255), Color.TRANSPARENT, Color.argb(30, 155, 140, 255)},
                new float[]{0f, 0.48f, 1f}, Shader.TileMode.CLAMP));
        canvas.drawRect(0, 0, w, h, paint);
        paint.setShader(null);
    }

    private void drawHandoffMode(Canvas c) {
        float w = getWidth();
        float h = getHeight();
        float safe = roundScreen ? w * 0.11f : 22f;

        drawText(c, CardConfig.EVENT + "  //  CONNECT", safe, h * 0.105f, 13f, MUTED, Paint.Align.LEFT, true);
        String status;
        int statusColor;
        if (hceHardware && nfcEnabled) {
            status = "NFC READY";
            statusColor = GREEN;
        } else if (!nfcPresent) {
            status = "QR READY";
            statusColor = AMBER;
        } else if (!nfcEnabled) {
            status = "NFC OFF";
            statusColor = AMBER;
        } else {
            status = "HCE UNAVAILABLE";
            statusColor = AMBER;
        }
        drawText(c, status, w - safe, h * 0.105f, 12f, statusColor, Paint.Align.RIGHT, true);

        float cx = w / 2f;
        float cy = h * 0.285f;
        drawNfcPulse(c, cx, cy, Math.min(w, h) * 0.19f);

        drawText(c, "TAP TO", cx, h * 0.47f, 31f, TEXT, Paint.Align.CENTER, true);
        drawText(c, "CONNECT", cx, h * 0.57f, 42f, TEXT, Paint.Align.CENTER, true);
        drawText(c, CardConfig.NAME, cx, h * 0.645f, 16f, Color.rgb(219, 228, 239), Paint.Align.CENTER, true);
        drawText(c, "THERMAL + ENERGY SYSTEMS", cx, h * 0.695f, 11f, CYAN, Paint.Align.CENTER, true);

        int qrSize = Math.max(92, Math.min(132, (int) (w * 0.27f)));
        float qrLeft = cx - qrSize / 2f;
        float qrTop = h * 0.72f;
        drawQr(c, qrLeft, qrTop, qrSize);
        drawText(c, "TAP SCREEN FOR LARGE QR", cx, Math.min(h - 14f, qrTop + qrSize + 22f), 9.5f, MUTED, Paint.Align.CENTER, true);

        postInvalidateDelayed(45);
    }

    private void drawQrMode(Canvas c) {
        float w = getWidth();
        float h = getHeight();
        float cx = w / 2f;
        drawText(c, "SCAN TO CONNECT", cx, h * 0.105f, 14f, CYAN, Paint.Align.CENTER, true);

        int size = (int) Math.min(w * 0.66f, h * 0.60f);
        if (roundScreen) size = (int) Math.min(size, w * 0.62f);
        float left = cx - size / 2f;
        float top = (h - size) / 2f - 4f;
        drawQr(c, left, top, size);

        drawText(c, CardConfig.NAME, cx, Math.min(h - 42f, top + size + 29f), 14f, TEXT, Paint.Align.CENTER, true);
        drawText(c, "TAP TO RETURN", cx, h - 18f, 9.5f, MUTED, Paint.Align.CENTER, true);
    }

    private void drawNfcPulse(Canvas c, float cx, float cy, float radius) {
        float phase = (SystemClock.uptimeMillis() % 2600L) / 2600f;
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeCap(Paint.Cap.ROUND);
        for (int i = 0; i < 3; i++) {
            float p = (phase + i / 3f) % 1f;
            float r = radius * (0.45f + 0.75f * p);
            int alpha = (int) (150 * (1f - p));
            paint.setColor(Color.argb(alpha, 110, 231, 255));
            paint.setStrokeWidth(2.2f);
            c.drawCircle(cx, cy, r, paint);
        }
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.argb(24, 110, 231, 255));
        c.drawCircle(cx, cy, radius * 0.53f, paint);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(3.2f);
        paint.setColor(CYAN);
        RectF outer = new RectF(cx - 26, cy - 26, cx + 26, cy + 26);
        RectF inner = new RectF(cx - 17, cy - 17, cx + 17, cy + 17);
        c.drawArc(outer, -55, 110, false, paint);
        c.drawArc(inner, -55, 110, false, paint);
        paint.setStyle(Paint.Style.FILL);
        c.drawCircle(cx + 4, cy, 3.8f, paint);
    }

    private void drawQr(Canvas c, float left, float top, int size) {
        if (qrBitmap == null) return;
        paint.setColor(Color.WHITE);
        paint.setStyle(Paint.Style.FILL);
        RectF pad = new RectF(left - 7, top - 7, left + size + 7, top + size + 7);
        c.drawRoundRect(pad, 13, 13, paint);
        RectF dst = new RectF(left, top, left + size, top + size);
        paint.setFilterBitmap(false);
        c.drawBitmap(qrBitmap, null, dst, paint);
    }

    private void drawText(Canvas c, String value, float x, float y, float sizeSp, int color,
                          Paint.Align align, boolean bold) {
        paint.setShader(null);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(color);
        paint.setTextAlign(align);
        paint.setTextSize(sp(sizeSp));
        paint.setTypeface(bold ? android.graphics.Typeface.create("sans", android.graphics.Typeface.BOLD)
                : android.graphics.Typeface.create("sans", android.graphics.Typeface.NORMAL));
        c.drawText(value, x, y, paint);
    }

    private float sp(float value) {
        return value * getResources().getDisplayMetrics().scaledDensity;
    }

    private Bitmap createQr(String text, int size) {
        try {
            Map<EncodeHintType, Object> hints = new EnumMap<>(EncodeHintType.class);
            hints.put(EncodeHintType.MARGIN, 1);
            BitMatrix matrix = new MultiFormatWriter().encode(text, BarcodeFormat.QR_CODE, size, size, hints);
            Bitmap bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
            for (int y = 0; y < size; y++) {
                for (int x = 0; x < size; x++) {
                    bitmap.setPixel(x, y, matrix.get(x, y) ? Color.BLACK : Color.WHITE);
                }
            }
            return bitmap;
        } catch (Exception ignored) {
            return null;
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            downAt = SystemClock.uptimeMillis();
            return true;
        }
        if (event.getAction() == MotionEvent.ACTION_UP) {
            if (SystemClock.uptimeMillis() - downAt < 700) {
                qrMode = !qrMode;
                performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK);
                invalidate();
            }
            return true;
        }
        return super.onTouchEvent(event);
    }
}
