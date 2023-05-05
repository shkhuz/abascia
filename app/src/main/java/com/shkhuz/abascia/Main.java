package com.shkhuz.abascia;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.text.Html;
import android.util.Log;
import android.view.HapticFeedbackConstants;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.text.HtmlCompat;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Stack;

import ru.noties.jlatexmath.JLatexMathView;

enum State {
    shift,
    normal,
    settings,
}

public class Main extends AppCompatActivity implements View.OnKeyListener {
    private RecyclerView stackview;
    private Stack<ViewModel> data = new Stack<>();
    private CustomAdapter adapter;
    public StringBuffer input;
    public TextView errorView;
    private ErrorKind error = ErrorKind.EK_NONE;
    public int volatile_idx = -1;
    public final int INTERNAL_SCALE = 32;
    private State state = State.normal;
    private boolean isDecPresent = false;
    private boolean isExpPresent = false;
    private Button keyshift;
    private Button keypoint;
    private CalculatorKeyLayout keygrid;
    private ArrayList<CharSequence> initial_keygrid_texts = new ArrayList<>();
    private ArrayList<Boolean> initial_keygrid_styles = new ArrayList<>();
    private ColorStateList default_button_color;
    private SharedPreferences sharedPreferences;
    private SharedPreferences.Editor spedit;

    public DispMode dispmode;

    public int tp_idx;
    public int[] tp_values = {
            0, 2, 5, 10, 15,
    };

    public int dp_idx;
    public int[] dp_values = {
            0, 2, 5, 10, 15,
    };

    private String[] normal_texts = {
            "", "", "", "", "",
            "DROP", "SWAP", "&#x215f;&#x2093;", "&#x221a;", "&#x2093;&#xb2;",
            "SHIFT", "7", "8", "9", "&#x00F7;",
            "DEL", "4", "5", "6", "&#x00D7;",
            "E<br>N<br>T<br>E<br>R", "1", "2", "3", "&#x2212;",
                                     "0", ".", "&#x00b1;", "+",
    };

    private boolean[] normal_styles = {
            false, false, false, false, false,
            false, false, false, false, false,
            false, true, true, true, true,
            false, true, true, true, true,
            false, true, true, true, true,
                  true, true, true, true,
    };

    private String[] shift_texts = {
            "⚙", "", "", "", "",
            "", "", "", "", "&#x2093;&#x207F;",
            "SHIFT", "π", "", "", "",
            "CLR", "", "", "", "",
            "", "", "", "", "",
                "", "", "", "",
    };

    private boolean[] shift_styles = {
            false, false, false, false, false,
            false, false, false, false, false,
            false, true, false, false, false,
            false, false, false, false, false,
            false, false, false, false, false,
                   false, false, false, false,
    };

    private String[] settings_texts = {
            "BACK", "", "", "", "",
            "", "", "", "", "",
            "", "", "", "", "",
            "", "", "", "", "",
            "", "", "", "", "",
                "", "", "", "",
    };

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

        this.input = new StringBuffer(32);
        this.keyshift = findViewById(R.id.shift);
        this.keypoint = findViewById(R.id.point);
        this.keygrid = findViewById(R.id.keygrid);
        this.default_button_color = keypoint.getBackgroundTintList();

        this.sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        this.spedit = sharedPreferences.edit();

        dispmode = dispmode_from_string(sharedPreferences.getString("Mode", "SCI"));
        tp_idx = sharedPreferences.getInt("TP", 3);
        dp_idx = sharedPreferences.getInt("DP", 1);

        updateState(State.normal);
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

        int number = CalculatorKeyLayout.getNumberFromButton(vid);
        if (number != -1) {
            if (state == State.shift) {
                if (number == 7) {
                    PushResult p = validatePush();
                    if (validatePush() != PushResult.cannot_push) {
                        addElementIfNonVolatile();
                        volatile_idx = -1;
                        ViewModel m = data.get(data.size()-1);
                        m.val = new BigDecimal(Math.PI);
                        m.latex = "π";
                        adapter.notifyItemChanged(data.size()-1);
                    }
                    if (p == PushResult.pushed) {
                        binaryOp(Op.mul);
                    }
                }
            } else {
                addElementIfNonVolatile();
                input.append(number);
                adapter.notifyItemChanged(volatile_idx);
            }
        } else if (vid == R.id.point) {
            if (state == State.normal) {
                addElementIfNonVolatile();
                String text = ((Button) view).getText().toString();
                if (text.equals(".")) input.append('.');
                else if (text.equals("EE")) input.append("E");
                else throw new IllegalArgumentException();
                adapter.notifyItemChanged(volatile_idx);
            } else if (state == State.shift) {
            } else if (state == State.settings) {
            }
        } else if (vid == R.id.t1) {
            if (state == State.normal) {
            } else if (state == State.shift) {
                updateState(State.settings);
            } else if (state == State.settings) {
                updateState(State.normal);
            }
        } else if (vid == R.id.t2) {
            if (state == State.normal) {
            } else if (state == State.shift) {
            } else if (state == State.settings) {
                if (dispmode == DispMode.plain) dispmode = DispMode.sci;
                else dispmode = DispMode.plain;

                spedit.putString("Mode", dispmode_string(dispmode));
                spedit.apply();
            }
        } else if (vid == R.id.t3) {
            if (state == State.normal) {
            } else if (state == State.shift) {
            } else if (state == State.settings) {
                if (tp_idx == tp_values.length-1) tp_idx = 0;
                else tp_idx++;

                spedit.putInt("TP", tp_idx);
                spedit.apply();
            }
        } else if (vid == R.id.t4) {
            if (state == State.normal) {
            } else if (state == State.shift) {
            } else if (state == State.settings) {
                if (dp_idx == dp_values.length-1) dp_idx = 0;
                else dp_idx++;

                spedit.putInt("DP", dp_idx);
                spedit.apply();
            }
        } else if (vid == R.id.enter) {
            if (state == State.normal) {
                validatePush();
            } else if (state == State.shift) {
            } else if (state == State.settings) {
            }

        } else if ((vid == R.id.plus || vid == R.id.minus || vid == R.id.mult || vid == R.id.div)) {
            if (state == State.normal) {
                binaryOp(op_from_id(vid));
            } else if (state == State.shift) {
            } else if (state == State.settings) {
            }
        } else if (vid == R.id.chs) {
            if (state == State.normal) {
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
                        if (n.lastOp == Op.add || n.lastOp == Op.sub) {
                            addParamsIfNeeded(n, op_from_id(vid), false);
                        }
                        n.lastOp = op_from_id(vid);
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
            } else if (state == State.shift) {
            } else if (state == State.settings) {
            }
        } else if (vid == R.id.shift) {
            if (state == State.normal) {
                updateState(State.shift);
            } else if (state == State.shift) {
                updateState(State.normal);
            } else if (state == State.settings) {
            }
        } else if (vid == R.id.del) {
            if (state == State.normal) {
                if (volatile_idx != -1 && input.length() > 0) {
                    input.setLength(input.length() - 1);
                    adapter.notifyItemChanged(volatile_idx);
                }
            } else if (state == State.shift) {
                volatile_idx = -1;
                data.clear();
                adapter.notifyItemRangeRemoved(0, data.size());
                input.setLength(0);
            } else if (state == State.settings) {
            }
        } else if (vid == R.id.pow2) {
            if (state == State.normal) {
                if (validatePush() != PushResult.cannot_push && data.size() > 0) {
                    ViewModel n = data.pop();
                    notifyAdapterItemRemoved(data.size() - 1);
                    try {
                        if (n.lastOp != Op.none) addParams(n);
                        n.lastOp = op_from_id(vid);
                        n.latex = n.latex + "^{2}";
                        n.val = n.val.pow(2);
                        data.push(n);
                        adapter.notifyItemInserted(data.size() - 1);
                    } catch (RuntimeException e) {
                        showOnStackError(e.getMessage());
                    }
                }
            } else if (state == State.shift) {
                if (validatePush() != PushResult.cannot_push && data.size() > 1) {
                    // TODO: store viewmodel instead of creating anew
                    ViewModel exp = data.pop();
                    ViewModel n = data.pop();
                    notifyAdapterItemRangeRemoved(data.size()-2, 2);
                    try {
                        if (n.lastOp != Op.none) addParams(n);
                        n.lastOp = op_from_id(vid);
                        n.latex = n.latex + "^{" + exp.latex + "}";
                        BigDecimal res;
                        try {
                            int expi = exp.val.intValueExact();
                            res = n.val.pow(expi);
                        } catch (ArithmeticException e) {
                            res = approxPow(n.val, exp.val);
                        }
                        n.val = res;
                        data.push(n);
                        adapter.notifyItemInserted(data.size()-1);
                    } catch (RuntimeException e) {
                        showOnStackError(e.getMessage());
                    }
                }
            } else if (state == State.settings) {
            }
        } else if (vid == R.id.sqrt) {
            if (state == State.normal) {
                if (validatePush() != PushResult.cannot_push && data.size() > 0) {
                    ViewModel n = data.pop();
                    notifyAdapterItemRemoved(data.size() - 1);
                    try {
                        n.lastOp = op_from_id(vid);
                        n.latex = "\\sqrt{" + n.latex + "}";
                        n.val = sqrt(n.val, INTERNAL_SCALE);
                        data.push(n);
                        adapter.notifyItemInserted(data.size() - 1);
                    } catch (RuntimeException e) {
                        showOnStackError(e.getMessage());
                    }
                }
            } else if (state == State.shift) {
            } else if (state == State.settings) {
            }
        } else if (vid == R.id.drop) {
            if (state == State.normal) {
                popAndClearInput();
            } else if (state == State.shift) {
            } else if (state == State.settings) {
            }
        }

        // Transition state changes
        if (state == State.shift && vid != R.id.shift) updateState(State.normal);

        if (error == ErrorKind.EK_NONE && volatile_idx != -1) {
            isDecPresent = input.indexOf(".") != -1;
            isExpPresent = input.indexOf("E") != -1;
        } else {
            isDecPresent = false;
            isExpPresent = false;
        }

        // In state changes
        if (state == State.normal) {
            if (isExpPresent) {
                keypoint.setText(".");
                keypoint.setEnabled(false);
            } else {
                keypoint.setEnabled(true);
                if (isDecPresent) keypoint.setText("EE");
                else keypoint.setText(".");
            }
        } else if (state == State.settings) {
            ((Button)findViewById(R.id.t2)).setText(String.format("Mode: %s", dispmode_string(dispmode)));
            ((Button)findViewById(R.id.t3)).setText(String.format("TP: %d", tp_values[tp_idx]));
            ((Button)findViewById(R.id.t4)).setText(String.format("DP: %d", dp_values[dp_idx]));
            adapter.notifyItemRangeChanged(0, data.size());

            if (dispmode == DispMode.plain) ((Button)findViewById(R.id.t3)).setEnabled(false);
            else ((Button)findViewById(R.id.t3)).setEnabled(true);
        }

        // if Ek.OnStackError then scroll to error position
        stackview.scrollToPosition(data.size() - 1);
    }

//    private void setDP() {
//        if (validatePush() != PushResult.cannot_push && data.size() > 0) {
//            BigDecimal n = data.pop().val;
//            notifyAdapterItemRemoved(data.size()-1);
//            int nint = n.intValue();
//            if (nint < INTERNAL_SCALE) {
//                display_scale = nint;
//                adapter.notifyItemRangeChanged(0, data.size());
//            } else {
//                showOnStackError(String.format("Display scale must be less than %d", INTERNAL_SCALE));
//            }
//        }
//    }

    private void binaryOp(Op op) {
        if (validatePush() != PushResult.cannot_push && data.size() > 1) {
            ViewModel n2 = data.pop();
            ViewModel n1 = data.pop();
            notifyAdapterItemRangeRemoved(data.size() - 2, 2);

            try {
                if (op == Op.add || op == Op.sub || op == Op.mul) {
                    addParamsIfNeeded(n1, op, true);
                    addParamsIfNeeded(n2, op, false);
                }
                n1.lastOp = op;
                if (op == Op.add) { n1.val = n1.val.add(n2.val);                           n1.latex = n1.latex + "+" + n2.latex; }
                else if (op == Op.sub) { n1.val = n1.val.subtract(n2.val);                n1.latex = n1.latex + "-" + n2.latex; }
                else if (op == Op.mul) { n1.val = n1.val.multiply(n2.val);                 n1.latex = n1.latex + "\\cdot" + n2.latex; }
                else { n1.val = n1.val.divide(n2.val, INTERNAL_SCALE, RoundingMode.HALF_EVEN); n1.latex = "\\frac{" + n1.latex + "}{" + n2.latex + "}"; }
                data.push(n1);
                adapter.notifyItemInserted(data.size() - 1);
            } catch (ArithmeticException e) {
                showOnStackError("Division by zero");
            }
        }
    }

    private int getOpPrecedence(Op op) {
        if (op == Op.add || op == Op.sub) return 1;
        else if (op == Op.mul || op == Op.div) return 2;
        else if (op == Op.neg) return 3;
        else return 4;
    }

    private void addParams(ViewModel v) {
        v.latex = "\\left(" + v.latex + "\\right)";
    }

    private void addParamsIfNeeded(ViewModel v, Op parentOp, boolean isLeftOperand) {
        if (getOpPrecedence(parentOp) > getOpPrecedence(v.lastOp) ||
                (parentOp == Op.sub && (v.lastOp == Op.add || v.lastOp == Op.sub) && !isLeftOperand))
            addParams(v);
    }

    private void updateState(State value) {
        state = value;
        if (state == State.normal) {
            ((Button)findViewById(R.id.t3)).setEnabled(true);
            for (int i = 0; i < keygrid.getChildCount(); i++) {
                final Button b = (Button) keygrid.getChildAt(i);
                b.setText(HtmlCompat.fromHtml(normal_texts[i], HtmlCompat.FROM_HTML_MODE_LEGACY));
                b.setTextAppearance(normal_styles[i] ? R.style.CalculatorButtonLargeStyle : R.style.CalculatorButtonSmallStyle);
                b.setBackgroundTintList(default_button_color);
            }
            keyshift.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.dark_green));
        } else if (state == State.shift) {
            for (int i = 0; i < keygrid.getChildCount(); i++) {
                final Button b = (Button) keygrid.getChildAt(i);
                b.setText(HtmlCompat.fromHtml(shift_texts[i], HtmlCompat.FROM_HTML_MODE_LEGACY));
                b.setTextAppearance(shift_styles[i] ? R.style.CalculatorButtonLargeStyle : R.style.CalculatorButtonSmallStyle);
                b.setBackgroundTintList(default_button_color);
                if (b.getId() == R.id.point) b.setEnabled(true);
            }
            keyshift.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.green));
        } else if (state == State.settings) {
            for (int i = 0; i < keygrid.getChildCount(); i++) {
                final Button b = (Button) keygrid.getChildAt(i);
                b.setText(HtmlCompat.fromHtml(settings_texts[i], HtmlCompat.FROM_HTML_MODE_LEGACY));
                b.setTextAppearance(R.style.CalculatorButtonSmallStyle);
                b.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.blue));
            }
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

    public enum PushResult {
        nothing_to_push,
        pushed,
        cannot_push,
    }

    private PushResult validatePush() {
        if (volatile_idx == -1) return PushResult.nothing_to_push;
        else if (input.length() > 0 && input.indexOf("E") < input.length()-1) {
            if (input.indexOf(".") == 0 || input.indexOf("-.") == 0) {
                input.insert(input.indexOf("-") != -1 ? 1 : 0, '0');
            }
            ViewModel m = data.get(volatile_idx);
            String repr = input.toString();
            if (repr.contains("E-")) {
                m.latex = repr.replace("E-", "\\text{E-}");
            } else if (repr.contains("E")) {
                m.latex = repr.replace("E", "\\text{E}");
            } else {
                m.latex = repr;
            }
            m.val = new BigDecimal(repr);
            adapter.notifyItemChanged(volatile_idx);
            input.setLength(0);
            volatile_idx = -1;
            return PushResult.pushed;
        }
        return PushResult.cannot_push;
    }

    private void popAndClearInput() {
        if (data.size() > 0) {
            data.pop();
            notifyAdapterItemRemoved(data.size() - 1);
            volatile_idx = -1;
            input.setLength(0);
        }
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

    public enum DispMode {
        plain,
        sci,
    }

    public String dispmode_string(DispMode mode) {
        switch (mode) {
            case plain: return "PLAIN";
            case sci: return "SCI";
        }
        throw new IllegalArgumentException();
    }

    public DispMode dispmode_from_string(String mode) {
        String modeUpper = mode.toUpperCase();
        if (modeUpper.equals("PLAIN")) return DispMode.plain;
        else if (modeUpper.equals("SCI")) return DispMode.sci;
        else throw new IllegalArgumentException();
    }

    public Op op_from_id(int id) {
        if (id == R.id.plus) return Op.add;
        else if (id == R.id.minus) return Op.sub;
        else if (id == R.id.mult) return Op.mul;
        else if (id == R.id.div) return Op.div;
        else if (id == R.id.chs) return Op.neg;
        else return Op.other;
    }

    public enum Op {
        add,
        sub,
        mul,
        div,
        neg,
        other,
        none,
    }
}