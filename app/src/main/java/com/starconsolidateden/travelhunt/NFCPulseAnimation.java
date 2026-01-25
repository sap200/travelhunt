package com.starconsolidateden.travelhunt;


import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.LinearInterpolator;

import androidx.core.content.ContextCompat;

public class NFCPulseAnimation extends View {

    private Paint paint;                // Paint for circles
    private float[] radii = {0f, 0f, 0f};  // Radii for 3 concentric circles
    private int pulseCount = 3;         // Number of pulses
    private ValueAnimator animator;     // Animator for pulses

    private Drawable cardImage;         // Center card image

    private Paint textPaint;            // Paint for heading and bottom text

    public NFCPulseAnimation(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        // Paint for pulsing circles
        paint = new Paint();
        paint.setColor(Color.parseColor("#FFFFFF"));
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(10f);
        paint.setAlpha(150);

        // Load card image from drawable (replace with your drawable)
        cardImage = ContextCompat.getDrawable(getContext(), R.drawable.ic_card); // Your card image here
        cardImage.setBounds(0,0,200,120); // Will adjust position in onDraw

        // Paint for heading and bottom text
        textPaint = new Paint();
        textPaint.setColor(Color.BLACK);
        textPaint.setTypeface(Typeface.create("cursive", Typeface.BOLD)); // cursive + bold
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setTextSize(60f);
        textPaint.setAntiAlias(true);

        // Animator from 0 -> 1, loops infinitely
        animator = ValueAnimator.ofFloat(0f, 1f);
        animator.setDuration(1500);
        animator.setRepeatCount(ValueAnimator.INFINITE);
        animator.setInterpolator(new LinearInterpolator());
        animator.addUpdateListener(animation -> {
            float value = (float) animation.getAnimatedValue();

            // Update radii for 3 circles with offsets
            for (int i = 0; i < pulseCount; i++) {
                float delay = i * 0.3f; // stagger each pulse
                float v = value + delay;
                if (v > 1f) v -= 1f;
                radii[i] = v * getWidth() / 3f;
            }

            invalidate(); // Redraw
        });

        animator.start();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);



        float cx = getWidth() / 2f;   // Center X
        float cy = getHeight() / 2f;  // Center Y



        // Draw card image in the center
        int imgWidth = cardImage.getIntrinsicWidth();
        int imgHeight = cardImage.getIntrinsicHeight();
        cardImage.setBounds((int)(cx - imgWidth/2), (int)(cy - imgHeight/2),
                (int)(cx + imgWidth/2), (int)(cy + imgHeight/2));
        cardImage.draw(canvas);


        // Draw 3 concentric pulsing circles
        for (int i = 0; i < pulseCount; i++) {
            int alpha = (int) ((1 - (radii[i] / (getWidth() / 3f))) * 150);
            paint.setAlpha(Math.max(alpha, 0));
            canvas.drawCircle(cx, cy, radii[i], paint);
        }


        // Draw heading text at the top
        // Draw bottom text

        canvas.drawText("Tap your NFC card", cx, cy + getHeight()/2.5f, textPaint);
    }
}

