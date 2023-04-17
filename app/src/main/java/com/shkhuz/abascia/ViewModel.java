package com.shkhuz.abascia;

import java.math.BigDecimal;

public class ViewModel {
    public String latex;
    // Specifies last operation done on it. Uses id of operator buttons.
    public int lastOp;
    public BigDecimal val;

    public ViewModel() {
        this.latex = "";
        this.val = null;
    }

    public ViewModel(final BigDecimal val) {
        this.latex = "";
        this.lastOp = -1;
        this.val = val;
    }
}