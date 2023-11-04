package com.shkhuz.abascia;

import android.graphics.Color;
import android.opengl.Visibility;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.NumberPicker;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Stack;

import kotlin.text.Regex;
import ru.noties.jlatexmath.JLatexMathView;

public class CustomAdapter extends RecyclerView.Adapter<CustomAdapter.ViewHolder> {

    public Stack<ViewModel> data;
    public Main main;

    public CustomAdapter(final Stack<ViewModel> data, final Main main) {
        this.data = data;
        this.main = main;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.stack_item, parent, false);
        return new ViewHolder(view);
    }

    public int getBigDecimalDigits(BigDecimal b) {
        if (b.scale() < 0) {
            return b.precision() + (-b.scale());
        }
        return b.precision();
    }

    public int getBigDecimalDigitsToRight(BigDecimal b) {
        return Math.max(b.scale(), 0);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        // TODO: disable mathRepr on error
        if (main.volatile_idx == position) {
            holder.textView.setText(main.input.toString());
            holder.textView.setTextColor(ContextCompat.getColor(main, android.R.color.white));
            holder.mathView.setVisibility(View.GONE);
        } else {
            ViewModel m = data.get(position);
            BigDecimal b = m.val.stripTrailingZeros();
            String text;
            int constrained_scale = Math.min(getBigDecimalDigitsToRight(b), main.dp_values[main.dp_idx]);
            if (Math.max(getBigDecimalDigits(b) - getBigDecimalDigitsToRight(b) + constrained_scale, constrained_scale) >
                    main.tp_values[main.tp_idx]
                    && main.dispmode == Main.DispMode.sci) {
                NumberFormat formatter = new DecimalFormat("0.#E0");
                formatter.setRoundingMode(RoundingMode.HALF_EVEN);
                formatter.setMaximumFractionDigits(main.dp_values[main.dp_idx]);
                text = formatter.format(b);
                if (text.endsWith("E0")) {
                    text = text.substring(0, text.length()-2);
                }
            } else {
                //b.setScale(main.dp_values[main.dp_idx], RoundingMode.HALF_EVEN).stripTrailingZeros();
                NumberFormat formatter = new DecimalFormat("###,###.##");
                formatter.setMaximumFractionDigits(main.dp_values[main.dp_idx]);
                text = formatter.format(b);
            }

            holder.textView.setText(text);
            holder.textView.setTextColor(ContextCompat.getColor(main, R.color.light_green));
            holder.mathView.setVisibility(View.VISIBLE);
            holder.mathView.setLatex(m.latex);
        }
    }

    @Override
    public int getItemCount() {
        return data.size();
    }
    
    public class ViewHolder extends RecyclerView.ViewHolder {
        public TextView textView;
        public JLatexMathView mathView;
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            this.textView = itemView.findViewById(R.id.textView);
            this.mathView = itemView.findViewById(R.id.mathRepr);
        }
    }
}
