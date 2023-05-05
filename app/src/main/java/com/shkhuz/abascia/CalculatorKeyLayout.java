package com.shkhuz.abascia;

import android.content.Context;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.SpannedString;
import android.text.style.QuoteSpan;
import android.text.style.RelativeSizeSpan;
import android.text.style.SuperscriptSpan;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.widget.Button;
import android.widget.GridLayout;

import java.math.BigDecimal;

public class CalculatorKeyLayout extends GridLayout {
    private final Context ctx;

    public CalculatorKeyLayout(final Context context) {
        super(context);
        this.ctx = context;
    }

    public CalculatorKeyLayout(final Context context, final AttributeSet attrs) {
        super(context, attrs);
        this.ctx = context;
    }

    public CalculatorKeyLayout(final Context context, final AttributeSet attrs,
                               final int defStyle) {
        super(context, attrs, defStyle);
        this.ctx = context;
    }

    @Override
    protected void onMeasure(final int widthMeasureSpec, final int heightMeasureSpec) {
        int width = MeasureSpec.getSize(widthMeasureSpec);
        int height = MeasureSpec.getSize(heightMeasureSpec);
        resizeKeys(width);
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    private void resizeKeys(final int keyboardWidth) {
        int keyw = keyboardWidth / 5;
        int keyh = keyw;
        for (int i = 0; i < getChildCount(); i++) {
            final Button key = (Button) getChildAt(i);
            final int kid = key.getId();
            if (kid == R.id.enter) {
                key.setHeight(keyh * 2);
            } else if (!isSmallHeightButton(kid)) {
                key.setHeight(keyh);
            }
            key.setWidth(keyw);
            if (isSmallHeightButton(kid) && !isSymbolButton(kid)) {
                float size = key.getTextSize();
                if (size > 32.0) {
                    key.setTextSize(TypedValue.COMPLEX_UNIT_PX, 32.0f);
                }
            }
        }
    }

    public static boolean isSmallHeightButton(int id) {
        return id == R.id.sqrt || id == R.id.pow2 || id == R.id.swap || id == R.id.drop || id == R.id.recip ||
                id == R.id.t1 || id == R.id.t2 || id == R.id.t3 || id == R.id.t4 || id == R.id.t5;
    }

    public static boolean isSymbolButton(int id) {
        return id == R.id.sqrt || id == R.id.pow2 || id == R.id.recip;
    }

    public static int getNumberFromButton(int id) {
        if (id == R.id.k0) return 0;
        else if (id == R.id.k1) return 1;
        else if (id == R.id.k2) return 2;
        else if (id == R.id.k3) return 3;
        else if (id == R.id.k4) return 4;
        else if (id == R.id.k5) return 5;
        else if (id == R.id.k6) return 6;
        else if (id == R.id.k7) return 7;
        else if (id == R.id.k8) return 8;
        else if (id == R.id.k9) return 9;
        else return -1;
    }
}
