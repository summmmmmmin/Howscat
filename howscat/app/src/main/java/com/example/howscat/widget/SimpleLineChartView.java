package com.example.howscat.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;

import com.example.howscat.R;

/**
 * 최근 수치(몸무게·체지방률 등)를 단순 꺾은선으로 표시합니다. X축에 날짜 라벨을 둘 수 있습니다.
 */
public class SimpleLineChartView extends View {

    private final Paint gridPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint fillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pointPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint labelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private float[] values = new float[0];
    private String[] xLabels = new String[0];
    private String unit = "";

    public SimpleLineChartView(Context context) {
        super(context);
        init(context);
    }

    public SimpleLineChartView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    private void init(Context context) {
        gridPaint.setColor(context.getColor(R.color.app_outline));
        gridPaint.setStrokeWidth(1f);
        linePaint.setColor(context.getColor(R.color.app_primary_dark));
        linePaint.setStrokeWidth(4f);
        linePaint.setStyle(Paint.Style.STROKE);
        linePaint.setStrokeCap(Paint.Cap.ROUND);
        linePaint.setStrokeJoin(Paint.Join.ROUND);
        fillPaint.setColor(context.getColor(R.color.app_primary_container));
        fillPaint.setStyle(Paint.Style.FILL);
        pointPaint.setColor(context.getColor(R.color.app_primary_dark));
        pointPaint.setStyle(Paint.Style.FILL);
        labelPaint.setColor(context.getColor(R.color.app_on_surface_variant));
        labelPaint.setTextSize(10f * getResources().getDisplayMetrics().scaledDensity);
        labelPaint.setTextAlign(Paint.Align.CENTER);
    }

    public void setUnit(String unit) {
        this.unit = unit != null ? unit : "";
        invalidate();
    }

    public void setValues(float[] values) {
        this.values = values != null ? values : new float[0];
        this.xLabels = new String[0];
        invalidate();
    }

    /**
     * values 와 dateLabels 길이가 같을 때만 하단에 날짜 라벨을 그립니다.
     */
    public void setSeries(float[] values, String[] dateLabels) {
        this.values = values != null ? values : new float[0];
        if (dateLabels != null && dateLabels.length == this.values.length) {
            this.xLabels = dateLabels;
        } else {
            this.xLabels = new String[0];
        }
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int w = getWidth();
        int h = getHeight();
        if (w <= 0 || h <= 0) return;

        boolean hasX = xLabels.length == values.length && values.length > 0;
        float padL = 16f;
        float padR = 16f;
        float padT = 8f;
        float padB = hasX ? 56f : 24f;
        float innerW = w - padL - padR;
        float innerH = h - padT - padB;

        canvas.drawLine(padL, h - padB, w - padR, h - padB, gridPaint);
        float y1 = padT + innerH * 0.25f;
        float y2 = padT + innerH * 0.5f;
        float y3 = padT + innerH * 0.75f;
        canvas.drawLine(padL, y1, w - padR, y1, gridPaint);
        canvas.drawLine(padL, y2, w - padR, y2, gridPaint);
        canvas.drawLine(padL, y3, w - padR, y3, gridPaint);

        if (values.length == 0) {
            labelPaint.setTextSize(10f * getResources().getDisplayMetrics().scaledDensity);
            labelPaint.setTextAlign(Paint.Align.LEFT);
            canvas.drawText("데이터가 없어요", padL, padT + labelPaint.getTextSize(), labelPaint);
            return;
        }

        float min = values[0];
        float max = values[0];
        for (float v : values) {
            min = Math.min(min, v);
            max = Math.max(max, v);
        }
        if (max - min < 1e-6f) {
            min -= 1f;
            max += 1f;
        }
        float range = max - min;

        int n = values.length;
        Path path = new Path();
        Path fillPath = new Path();
        for (int i = 0; i < n; i++) {
            float x = padL + innerW * (n == 1 ? 0.5f : (float) i / (n - 1));
            float ny = (values[i] - min) / range;
            float y = padT + innerH * (1f - ny);
            if (i == 0) {
                path.moveTo(x, y);
                fillPath.moveTo(x, h - padB);
                fillPath.lineTo(x, y);
            } else {
                path.lineTo(x, y);
                fillPath.lineTo(x, y);
            }
        }
        if (n > 0) {
            float lastX = padL + innerW * (n == 1 ? 0.5f : (float) (n - 1) / (n - 1));
            fillPath.lineTo(lastX, h - padB);
            fillPath.close();
            canvas.drawPath(fillPath, fillPaint);
        }
        canvas.drawPath(path, linePaint);
        for (int i = 0; i < n; i++) {
            float x = padL + innerW * (n == 1 ? 0.5f : (float) i / (n - 1));
            float ny = (values[i] - min) / range;
            float y = padT + innerH * (1f - ny);
            canvas.drawCircle(x, y, 4.5f, pointPaint);
        }

        float axisLabelSize = 10f * getResources().getDisplayMetrics().scaledDensity;
        labelPaint.setTextSize(axisLabelSize);
        labelPaint.setTextAlign(Paint.Align.LEFT);
        String lo = String.format(java.util.Locale.getDefault(), "%.1f%s", min, unit);
        String hi = String.format(java.util.Locale.getDefault(), "%.1f%s", max, unit);
        canvas.drawText(lo, padL, h - (hasX ? 34f : 6f), labelPaint);
        labelPaint.setTextAlign(Paint.Align.RIGHT);
        canvas.drawText(hi, w - padR, h - (hasX ? 34f : 6f), labelPaint);

        if (hasX) {
            labelPaint.setTextAlign(Paint.Align.CENTER);
            float baseLabelSize = 9f * getResources().getDisplayMetrics().scaledDensity;
            labelPaint.setTextSize(baseLabelSize);
            float labelY = h - 10f;
            int step = 1;
            if (n >= 7) step = 2;
            if (n >= 10) step = 3;

            // 라벨이 촘촘하면 일부만 그려서 겹침/잘림을 줄입니다.
            for (int i = 0; i < n; i += step) {
                float x = padL + innerW * (n == 1 ? 0.5f : (float) i / (n - 1));
                String lab = xLabels[i] != null ? xLabels[i] : "";
                if (lab.length() > 6) {
                    lab = lab.substring(0, Math.min(6, lab.length()));
                }
                canvas.drawText(lab, x, labelY, labelPaint);
            }
            // 마지막 라벨은 항상 한 번 더 보장
            if (n > 1 && ((n - 1) % step != 0)) {
                float x = padL + innerW;
                String lab = xLabels[n - 1] != null ? xLabels[n - 1] : "";
                if (lab.length() > 6) {
                    lab = lab.substring(0, Math.min(6, lab.length()));
                }
                canvas.drawText(lab, x, labelY, labelPaint);
            }
            labelPaint.setTextSize(axisLabelSize);
        }
    }
}
