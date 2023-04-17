package com.shkhuz.abascia;

import android.os.Bundle;
import android.util.Log;
import android.view.HapticFeedbackConstants;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Stack;

import ru.noties.jlatexmath.JLatexMathView;

public class Main extends AppCompatActivity implements View.OnKeyListener {
    private RecyclerView stackview;
    private Stack<ViewModel> data = new Stack<>();
    private CustomAdapter adapter;
    public StringBuffer input;
    public TextView errorView;
    private ErrorKind error = ErrorKind.EK_NONE;
    public int volatile_idx = -1;
    public int display_scale = 2;
    public final int INTERNAL_SCALE = 32;
    private boolean shift = false;
    private boolean isDecPresent = false;
    private boolean isExpPresent = false;
    private Button keyshift;
    private Button keypoint;
    private CalculatorKeyLayout keygrid;
    private ArrayList<CharSequence> initial_keygrid_texts = new ArrayList<>();

    @Override
    public boolean onKey(View view, int i, KeyEvent keyEvent) {
        if (keyEvent.getAction() == KeyEvent.ACTION_DOWN) {
            if (i == KeyEvent.KEYCODE_1) ((Button) findViewById(R.id.k1)).performClick();
            else if (i == KeyEvent.KEYCODE_2) ((Button) findViewById(R.id.k2)).performClick();
            else if (i == KeyEvent.KEYCODE_3) ((Button) findViewById(R.id.k3)).performClick();
            else if (i == KeyEvent.KEYCODE_4) ((Button) findViewById(R.id.k4)).performClick();
            else if (i == KeyEvent.KEYCODE_5) ((Button) findViewById(R.id.k5)).performClick();
            else if (i == KeyEvent.KEYCODE_6) ((Button) findViewById(R.id.k6)).performClick();
            else if (i == KeyEvent.KEYCODE_7) ((Button) findViewById(R.id.k7)).performClick();
            else if (i == KeyEvent.KEYCODE_8) ((Button) findViewById(R.id.k8)).performClick();
            else if (i == KeyEvent.KEYCODE_9) ((Button) findViewById(R.id.k9)).performClick();
            else if (i == KeyEvent.KEYCODE_0) ((Button) findViewById(R.id.k0)).performClick();
            else if (i == KeyEvent.KEYCODE_ENTER) ((Button) findViewById(R.id.enter)).performClick();
            else if (i == KeyEvent.KEYCODE_EQUALS) ((Button) findViewById(R.id.plus)).performClick();
            else if (i == KeyEvent.KEYCODE_MINUS) ((Button) findViewById(R.id.minus)).performClick();
            else if (i == KeyEvent.KEYCODE_COMMA) ((Button) findViewById(R.id.mult)).performClick();
            else if (i == KeyEvent.KEYCODE_SLASH) ((Button) findViewById(R.id.div)).performClick();
            else if (i == KeyEvent.KEYCODE_PERIOD) ((Button) findViewById(R.id.point)).performClick();
            return true;
        }
        return false;
    }

    public enum ErrorKind {
        EK_PINNED_ERROR,
        EK_ON_STACK_ERROR,
        EK_NONE,
    }

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        InitOther initOther = new InitOther();
        initOther.start();

        this.stackview = findViewById(R.id.stack);
        this.stackview.setOnKeyListener(this);
        final LinearLayoutManager lm = new LinearLayoutManager(this);
        lm.setStackFromEnd(true);
        this.stackview.setLayoutManager(lm);
        this.stackview.setItemAnimator(null);
        this.adapter = new CustomAdapter(data, this);
        this.stackview.setAdapter(this.adapter);
        this.errorView = findViewById(R.id.errorView);

        input = new StringBuffer(32);
        keyshift = findViewById(R.id.shift);
        keypoint = findViewById(R.id.point);
        keygrid = findViewById(R.id.keygrid);
        for (int i = 0; i < keygrid.getChildCount(); i++) {
            initial_keygrid_texts.add(((Button)keygrid.getChildAt(i)).getText());
        }
    }

    public void clickHandler(View view) {
        int vid = view.getId();

        if (error == ErrorKind.EK_PINNED_ERROR) {
            error = ErrorKind.EK_NONE;
            errorView.setText("Abascia");
        } else if (error == ErrorKind.EK_ON_STACK_ERROR) {
            if (vid == R.id.drop) {
                error = ErrorKind.EK_NONE;
                popAndClearInput();
            }
            return;
        }
        if (error == ErrorKind.EK_NONE)
            view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS, HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING);

        int number = getNumberFromButton(vid);
        if (number != -1) {
            if (shift) {
            } else {
                addElementIfNonVolatile();
                input.append(number);
                adapter.notifyItemChanged(volatile_idx);
            }
        } else if (vid == R.id.point) {
            if (shift) {
            } else {
                addElementIfNonVolatile();
                String text = ((Button) view).getText().toString();
                if (text.equals(".")) input.append('.');
                else if (text.equals("EE")) input.append("E");
                else throw new IllegalArgumentException();
                adapter.notifyItemChanged(volatile_idx);
            }
        } else if (vid == R.id.enter) {
            if (shift) {
                if (validatePush() && data.size() > 0) {
                    BigDecimal n = data.pop().val;
                    notifyAdapterItemRemoved(data.size()-1);
                    int nint = n.intValue();
                    if (nint < INTERNAL_SCALE) {
                        display_scale = nint;
                        adapter.notifyItemRangeChanged(0, data.size());
                    } else {
                        showOnStackError(String.format("Display scale must be less than %d", INTERNAL_SCALE));
                    }
                }
            } else validatePush();
        } else if ((vid == R.id.plus || vid == R.id.minus || vid == R.id.mult || vid == R.id.div)) {
            if (shift) {
            } else {
                if (validatePush() && data.size() > 1) {
                    ViewModel n2 = data.pop();
                    ViewModel n1 = data.pop();
                    notifyAdapterItemRangeRemoved(data.size() - 2, 2);

                    try {
                        if (vid == R.id.plus || vid == R.id.minus || vid == R.id.mult) {
                            addParamsIfNeeded(n1, vid, true);
                            addParamsIfNeeded(n2, vid, false);
                        }
                        n1.lastOp = vid;
                        if (vid == R.id.plus) { n1.val = n1.val.add(n2.val);                           n1.latex = n1.latex + "+" + n2.latex; }
                        else if (vid == R.id.minus) { n1.val = n1.val.subtract(n2.val);                n1.latex = n1.latex + "-" + n2.latex; }
                        else if (vid == R.id.mult) { n1.val = n1.val.multiply(n2.val);                 n1.latex = n1.latex + "\\cdot" + n2.latex; }
                        else { n1.val = n1.val.divide(n2.val, INTERNAL_SCALE, RoundingMode.HALF_EVEN); n1.latex = "\\frac{" + n1.latex + "}{" + n2.latex + "}"; }
                        data.push(n1);
                        adapter.notifyItemInserted(data.size() - 1);
                    } catch (ArithmeticException e) {
                        showOnStackError("Division by zero");
                    }
                }
            }
        } else if (vid == R.id.chs) {
            if (shift) {
            } else {
                if (volatile_idx == -1 && data.size() > 0) {
                    ViewModel n = data.get(data.size()-1);
                    if (n.latex.startsWith("-")) {
                        StringBuilder s = new StringBuilder(n.latex).deleteCharAt(0);
//                        if (s.indexOf("\\left(") == 0 && n.latex.endsWith("\\right)")) {
//                            s.delete(0, 6);
//                            s.delete(s.length()-7, s.length());
//                        }
                        n.latex = s.toString();
//                        n.lastOp = ;
                    } else {
                        if (n.lastOp == R.id.plus || n.lastOp == R.id.minus) {
                            addParamsIfNeeded(n, vid, false);
                        }
                        n.lastOp = vid;
                        n.latex = '-' + n.latex;
                    }
                    n.val = n.val.negate();
                    adapter.notifyItemChanged(data.size()-1);
                } else if (volatile_idx != -1) {
                    int idxE = input.indexOf("E");
                    int idxMinus = input.indexOf("-");
                    if (idxE != -1) {
                        if (idxE + 1 < input.length() && input.charAt(idxE + 1) == '-')
                            input.deleteCharAt(idxE + 1);
                        else input.insert(idxE + 1, '-');
                    } else {
                        if (idxMinus != -1) input.deleteCharAt(idxMinus);
                        else input.insert(0, '-');
                    }
                    adapter.notifyItemChanged(volatile_idx);
                }
            }
        } else if (vid == R.id.shift) {
            updateShift(!shift);
        } else if (vid == R.id.del) {
            if (shift) {
                volatile_idx = -1;
                data.clear();
                adapter.notifyItemRangeRemoved(0, data.size());
                input.setLength(0);
            } else {
                if (volatile_idx != -1 && input.length() > 0) {
                    input.setLength(input.length() - 1);
                    adapter.notifyItemChanged(volatile_idx);
                }
            }
        } else if (vid == R.id.pow2) {
            if (shift) {
                if (validatePush() && data.size() > 1) {
                    // TODO: store viewmodel instead of creating anew
                    BigDecimal exp = data.pop().val;
                    BigDecimal n = data.pop().val;
                    notifyAdapterItemRangeRemoved(data.size()-2, 2);
                    BigDecimal res;
                    try {
                        try {
                            int expi = exp.intValueExact();
                            res = n.pow(expi);
                        } catch (ArithmeticException e) {
                            res = approxPow(n, exp);
                        }
                        data.push(new ViewModel(res));
                        adapter.notifyItemInserted(data.size()-1);
                    } catch (RuntimeException e) {
                        showOnStackError(e.getMessage());
                    }
                }
            } else {
                if (validatePush() && data.size() > 0) {
                    ViewModel n = data.pop();
                    notifyAdapterItemRemoved(data.size() - 1);
                    try {
                        if (n.lastOp != -1) addParams(n);
                        n.lastOp = vid;
                        n.latex = n.latex + "^{2}";
                        n.val = n.val.pow(2);
                        data.push(n);
                        adapter.notifyItemInserted(data.size() - 1);
                    } catch (RuntimeException e) {
                        showOnStackError(e.getMessage());
                    }
                }
            }
        } else if (vid == R.id.sqrt) {
            if (shift) {
            } else {
                if (validatePush() && data.size() > 0) {
                    ViewModel n = data.pop();
                    notifyAdapterItemRemoved(data.size() - 1);
                    try {
                        n.lastOp = vid;
                        n.latex = "\\sqrt{" + n.latex + "}";
                        n.val = sqrt(n.val, INTERNAL_SCALE);
                        data.push(n);
                        adapter.notifyItemInserted(data.size() - 1);
                    } catch (RuntimeException e) {
                        showOnStackError(e.getMessage());
                    }
                }
            }
        } else if (vid == R.id.drop) {
            if (shift) {
            } else {
                popAndClearInput();
            }
        }
        if (vid != R.id.shift) unShiftIfShift();

        if (error == ErrorKind.EK_NONE && volatile_idx != -1) {
            isDecPresent = input.indexOf(".") != -1;
            isExpPresent = input.indexOf("E") != -1;
        } else {
            isDecPresent = false;
            isExpPresent = false;
        }

        if (shift) {
            keyshift.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.green));
        } else {
            keyshift.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.dark_green));

            if (isExpPresent) {
                keypoint.setText(".");
                keypoint.setEnabled(false);
            } else {
                keypoint.setEnabled(true);
                if (isDecPresent) keypoint.setText("EE");
                else keypoint.setText(".");
            }
        }

        // if Ek.OnStackError then scroll to error position
        stackview.scrollToPosition(data.size() - 1);
    }

    // Only applicable for add, sub, mul operations
    private int getOpPrecedence(int vid) {
        if (vid == R.id.plus || vid == R.id.minus) return 1;
        else if (vid == R.id.mult || vid == R.id.div) return 2;
        else return 3;
    }

    private void addParams(ViewModel v) {
        v.latex = "\\left(" + v.latex + "\\right)";
    }

    private void addParamsIfNeeded(ViewModel v, int parentOp, boolean isLeftOperand) {
        // TODO: maybe change lastOp definition (because unary '-' and binary '-' are treated same
        // even though they are different.
        if (getOpPrecedence(parentOp) > getOpPrecedence(v.lastOp) ||
                (parentOp == R.id.minus && (v.lastOp == R.id.plus || v.lastOp == R.id.minus) && !isLeftOperand)) addParams(v);
    }

    private void updateShift(boolean value) {
        Log.d("Shift", "Updating buttons");
        shift = value;
        for (int i = 0; i < keygrid.getChildCount(); i++) {
            final Button b = (Button) keygrid.getChildAt(i);
            b.setText(shift ? (CharSequence) b.getTag() : initial_keygrid_texts.get(i));
            if (shift && b.getId() == R.id.point) b.setEnabled(true);
        }
    }

    private void unShiftIfShift() {
        if (shift) {
            updateShift(false);
        }
    }

    private void notifyAdapterItemRemoved(int position) {
        adapter.notifyItemRemoved(position);
        adapter.notifyItemRangeChanged(0, data.size());
    }

    private void notifyAdapterItemRangeRemoved(int position, int itemCount) {
        adapter.notifyItemRangeRemoved(position, itemCount);
        adapter.notifyItemRangeChanged(0, data.size());
    }

    private void showPinnedError(final String err) {
        error = ErrorKind.EK_PINNED_ERROR;
        errorView.setText(err);
    }

    private void showOnStackError(final String err) {
        error = ErrorKind.EK_ON_STACK_ERROR;
        data.push(null);
        input = new StringBuffer(err);
        volatile_idx = data.size()-1;
        adapter.notifyItemInserted(volatile_idx);
    }

    private void addElementIfNonVolatile() {
        if (volatile_idx == -1) {
            data.push(new ViewModel(new BigDecimal("0")));
            adapter.notifyItemInserted(data.size()-1);
            volatile_idx = data.size()-1;
        }
    }

    private void addElementIfEmpty() {
        if (data.size() == 0) {
            data.push(new ViewModel(new BigDecimal("0")));
            adapter.notifyItemInserted(data.size()-1);
            volatile_idx = data.size()-1;
        }
    }

    private boolean validatePush() {
        if (volatile_idx == -1) return true;
        else if (input.length() > 0 && input.indexOf("E") < input.length()-1) {
            if (input.indexOf(".") == 0 || input.indexOf("-.") == 0) {
                input.insert(input.indexOf("-") != -1 ? 1 : 0, '0');
            }
            ViewModel m = data.get(volatile_idx);
            String repr = input.toString();
            m.latex = repr;
            m.val = new BigDecimal(repr);
            adapter.notifyItemChanged(volatile_idx);
            input.setLength(0);
            volatile_idx = -1;
            return true;
        }
        return false;
    }

    private void popAndClearInput() {
        if (data.size() > 0) {
            data.pop();
            notifyAdapterItemRemoved(data.size() - 1);
            volatile_idx = -1;
            input.setLength(0);
        }
    }

    private int getNumberFromButton(int id) {
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

    /**
     * Computes the square root of x to a given scale, x >= 0.
     * Use Newton's algorithm.
     * Taken from "Java Number Cruncher: The Java Programmer's Guide to
     * Numerical Computing" (Ronald Mak, 2003) http://goo.gl/CXpi2
     * @param x the value of x
     * @param scale the desired scale of the result
     * @return the result value
     */
    private static BigDecimal sqrt(final BigDecimal x, final int scale)
    {
        // Check that x >= 0.
        if (x.signum() < 0) {
            throw new IllegalArgumentException("Sqrt of negative number");
        }
        if (x.signum() == 0) {
            return BigDecimal.ZERO;
        }

        // n = x*(10^(2*scale))
        BigInteger n = x.movePointRight(scale << 1).toBigInteger();

        // The first approximation is the upper half of n.
        int bits = (n.bitLength() + 1) >> 1;
        BigInteger ix = n.shiftRight(bits);
        BigInteger ixPrev;

        // Loop until the approximations converge
        // (two successive approximations are equal after rounding).
        do {
            ixPrev = ix;

            // x = (x + n/x)/2
            ix = ix.add(n.divide(ix)).shiftRight(1);

            Thread.yield();
        } while (ix.compareTo(ixPrev) != 0);

        return new BigDecimal(ix, scale);
    }

    /**
     * Compute the power x^y to a the given scale, using doubles.
     * Loses some precision, but means y can have non integer values.
     */
    private static BigDecimal approxPow(final BigDecimal x, final BigDecimal y)
    {
        double d;

        // Check that |y| >= 1 for negative x.
        if (x.signum() < 0 && y.abs().doubleValue() < 1.0) {
            throw new IllegalArgumentException("|n| < 1");
        }
        // Check that y is positive or 0 for x = 0.
        else if (x.signum() == 0 && y.signum() < 0) {
            throw new IllegalArgumentException("n < 0");
        }

        d = Math.pow(x.doubleValue(), y.doubleValue());
        return new BigDecimal(d);
    }

    public class InitOther extends Thread {
        public void run() {
            // A new temp JLatexMathView is created so that once the string is set, the class
            // can build its cache (in this thread, so as to not block the main thread), and when
            // we do need to actually set the latex string in some view, then it can utilize the
            // cached resources.
            JLatexMathView view = new JLatexMathView(Main.this);
            view.setLatex("\\frac{a}{c}");
        }
    }
}