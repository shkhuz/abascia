package com.shkhuz.abascia;

import java.math.BigDecimal;

public class ViewModel {
    public String latex;
    // Specifies last operation done on it.
    public Main.Op lastOp;
    public BigDecimal val;

    public ViewModel() {
        this.latex = "";
        this.val = null;
    }

    public ViewModel(final BigDecimal val) {
        this.latex = "";
        this.lastOp = Main.Op.none;
        this.val = val;
    }
}