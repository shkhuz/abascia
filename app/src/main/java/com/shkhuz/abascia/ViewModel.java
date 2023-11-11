package com.shkhuz.abascia;

import android.view.View;

import androidx.annotation.NonNull;

import java.math.BigDecimal;

public class ViewModel {
    public String latex;
    // Specifies last operation done on it.
    public Main.Op lastOp;
    public Main.Op lastToLastOp;
    public BigDecimal val;

    public ViewModel() {
        this.latex = "";
        this.val = null;
    }

    public ViewModel(final BigDecimal val) {
        this.latex = "";
        this.lastOp = Main.Op.none;
        this.lastToLastOp = Main.Op.none;
        this.val = val;
    }

    @NonNull
    @Override
    public ViewModel clone() {
        ViewModel v = new ViewModel();
        v.latex = this.latex;
        v.lastOp = this.lastOp;
        v.lastToLastOp = this.lastToLastOp;
        v.val = this.val;
        return v;
    }

}