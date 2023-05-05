package com.shkhuz.abascia;

import android.graphics.Color;
import android.opengl.Visibility;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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
    private NumberFormat formatter = new DecimalFormat("0.0E0");

    public CustomAdapter(final Stack<ViewModel> data, final Main main) {
        this.data = data;
        this.main = main;
        this.formatter.setRoundingMode(RoundingMode.HALF_UP);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.stack_item, parent, false);
        return new ViewHolder(view);
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
            BigDecimal b = m.val;
            String text;
            int constrained_scale = Math.min(b.scale(), main.dp_values[main.dp_idx]);
            if (Math.max(b.precision() - b.scale() + constrained_scale, constrained_scale) >
                    main.tp_values[main.tp_idx]
                    && main.dispmode == Main.DispMode.sci) {
                formatter.setMaximumFractionDigits(main.dp_values[main.dp_idx]);
                text = formatter.format(b);
            } else {
                text = b.setScale(main.dp_values[main.dp_idx], RoundingMode.HALF_EVEN).stripTrailingZeros().toPlainString();
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
