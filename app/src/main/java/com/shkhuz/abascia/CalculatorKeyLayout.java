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
            } else if (kid != R.id.sqrt && kid != R.id.pow2 && kid != R.id.swap && kid != R.id.drop && kid != R.id.recip) {
                key.setHeight(keyh);
            }
            key.setWidth(keyw);
            if (kid == R.id.swap || kid == R.id.drop) {
                float size = key.getTextSize();
                if (size > 32.0) {
                    key.setTextSize(TypedValue.COMPLEX_UNIT_PX, 32.0f);
                }
            }
        }
    }
}
