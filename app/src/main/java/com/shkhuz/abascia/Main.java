package com.shkhuz.abascia;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.os.Build;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.text.Html;
import android.util.Log;
import android.util.Pair;
import android.view.Gravity;
import android.view.HapticFeedbackConstants;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.NumberPicker;
import android.widget.PopupMenu;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
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

import ch.obermuhlner.math.big.BigDecimalMath;

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
    private final MathContext mc = new MathContext(32, RoundingMode.HALF_EVEN);
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
    private static final int ROUNDING_MODE = BigDecimal.ROUND_HALF_EVEN;
    private static final int SCALE = 18;

    public AngleMode anglemode;
    public DispMode dispmode;

    public int tp_idx;
    public int[] tp_values = {
            0, 2, 5, 10, 15,
    };

    public int dp_idx;
    public int[] dp_values = {
            0, 2, 5, 10, 15,
    };

    public int vibration_millis_idx;
    public int[] vibration_millis_values = {
            20, 25, 30, 35, 40, 45, 50,
    };

    private String[] normal_texts = {
            "SIN", "COS", "TAN", "LN", "LOG",
            "DROP", "SWAP", "<big>&#x215f;&#x2093;</big>"/*1/x*/, "<big>&#x221a;</big>"/*sqrt*/, "<big>&#x2093;&#xb2;</big>"/*x^2*/,
            "SHIFT", "7", "8", "9", "<big>&#x00F7;</big>"/*div*/,
            "DEL", "4", "5", "6", "<big>&#x00D7;</big>"/*star*/,
            "E<br>N<br>T<br>E<br>R", "1", "2", "3", "<big>&#x2212;</big>"/*minus*/,
                                     "0", ".", "&#x00b1;"/*plusminus*/, "<big>+</big>",
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
            "", "", "", "", "",
            "", "", "", "", "<big>&#x2093;&#x207F;</big>"/*x^n*/,
            "SHIFT", "<big>&#x2093;&#xb3;</big>"/*x^3*/, "<big>&#x221B;</big>"/*cuberoot*/, "", "",
            "CLR", "", "", "", "",
            "&#x2699;"/*gear*/, "&#x3C0;"/*pi*/, "e", "", "",
                "", "", "", "",
    };

    private boolean[] shift_styles = {
            false, false, false, false, false,
            false, false, false, false, false,
            false, true, true, false, false,
            false, false, false, false, false,
            false, true, true, false, false,
                   false, false, false, false,
    };

    private String[] settings_texts = {
            "", "", "", "", "",
            "", "", "", "", "",
            "", "", "", "", "",
            "", "", "", "", "",
            "B<br>A<br>C<br>K", "", "", "", "",
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

        anglemode = anglemode_from_string(sharedPreferences.getString("AngleMode", "DEG"));
        dispmode = dispmode_from_string(sharedPreferences.getString("DisplayMode", "SCI"));
        tp_idx = sharedPreferences.getInt("TP", 3);
        dp_idx = sharedPreferences.getInt("DP", 1);
        vibration_millis_idx = sharedPreferences.getInt("VibrationMillis", 1);

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
            //view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS, HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING);
            vibrateWith(vibration_millis_values[vibration_millis_idx]);

        int number = CalculatorKeyLayout.getNumberFromButton(vid);
        if (number != -1) {
            if (state == State.normal) {
                addElementIfNonVolatile();
                input.append(number);
                adapter.notifyItemChanged(volatile_idx);
            } else if (state == State.shift) {
                if (number == 1) {
                    PushResult p = validatePush();
                    if (validatePush() != PushResult.cannot_push) {
                        addElementIfNonVolatile();
                        volatile_idx = -1;
                        ViewModel m = data.get(data.size() - 1);
                        m.val = new BigDecimal(Math.PI);
                        m.latex = "π";
                        adapter.notifyItemChanged(data.size() - 1);
                    }
                    if (p == PushResult.pushed) {
                        binaryOp(Op.mul);
                    }
                } else if (number == 2) {
                    PushResult p = validatePush();
                    if (validatePush() != PushResult.cannot_push) {
                        addElementIfNonVolatile();
                        volatile_idx = -1;
                        ViewModel m = data.get(data.size() - 1);
                        m.val = new BigDecimal(Math.E);
                        m.latex = "e";
                        adapter.notifyItemChanged(data.size() - 1);
                    }
                    if (p == PushResult.pushed) {
                        binaryOp(Op.mul);
                    }
                } else if (number == 7) {
                    if (validatePush() != PushResult.cannot_push && data.size() > 0) {
                        ViewModel n = data.pop();
                        notifyAdapterItemRemoved(data.size() - 1);
                        try {
                            if (n.lastOp != Op.none) addParams(n);
                            n.lastOp = op_from_id(vid);
                            n.latex = n.latex + "^{3}";
                            n.val = n.val.pow(3);
                            data.push(n);
                            adapter.notifyItemInserted(data.size() - 1);
                        } catch (RuntimeException e) {
                            showOnStackError(e.getMessage());
                        }
                    }
                } else if (number == 8) {
                    if (validatePush() != PushResult.cannot_push && data.size() > 0) {
                        ViewModel n = data.pop();
                        notifyAdapterItemRemoved(data.size() - 1);
                        try {
                            n.lastOp = op_from_id(vid);
                            n.latex = "\\sqrt[3]{" + n.latex + "}";
                            n.val = intRoot(n.val, 3, INTERNAL_SCALE);
                            data.push(n);
                            adapter.notifyItemInserted(data.size() - 1);
                        } catch (RuntimeException e) {
                            showOnStackError(e.getMessage());
                        }
                    }
                }
            } else if (state == State.settings) {
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
        } else if (vid == R.id.sin) {
            if (state == State.normal) {
                if (validatePush() != PushResult.cannot_push && data.size() > 0) {
                    ViewModel n = data.pop();
                    notifyAdapterItemRemoved(data.size() - 1);
                    try {
                        n.lastOp = op_from_id(vid);
                        addParams(n);
                        if (anglemode == AngleMode.degrees) {
                            n.latex = "\\sin^{\\circ}{" + n.latex + "}";
                            n.val = BigDecimalMath.sin(BigDecimalMath.toRadians(n.val, mc), mc);
                        } else if (anglemode == AngleMode.radians) {
                            n.latex = "\\sin{" + n.latex + "}";
                            n.val = BigDecimalMath.sin(n.val, mc);
                        }
                        data.push(n);
                        adapter.notifyItemInserted(data.size() - 1);
                    } catch (RuntimeException e) {
                        showOnStackError(e.getMessage());
                    }
                }
            } else if (state == State.shift) {
            } else if (state == State.settings) {
                if (anglemode == AngleMode.degrees) anglemode = AngleMode.radians;
                else anglemode = AngleMode.degrees;

                spedit.putString("AngleMode", anglemode_string(anglemode));
                spedit.apply();
            }
        } else if (vid == R.id.cos) {
            if (state == State.normal) {
                if (validatePush() != PushResult.cannot_push && data.size() > 0) {
                    ViewModel n = data.pop();
                    notifyAdapterItemRemoved(data.size() - 1);
                    try {
                        n.lastOp = op_from_id(vid);
                        addParams(n);
                        if (anglemode == AngleMode.degrees) {
                            n.latex = "\\cos^{\\circ}{" + n.latex + "}";
                            n.val = BigDecimalMath.cos(BigDecimalMath.toRadians(n.val, mc), mc);
                        } else if (anglemode == AngleMode.radians) {
                            n.latex = "\\cos{" + n.latex + "}";
                            n.val = BigDecimalMath.cos(n.val, mc);
                        }
                        data.push(n);
                        adapter.notifyItemInserted(data.size() - 1);
                    } catch (RuntimeException e) {
                        showOnStackError(e.getMessage());
                    }
                }
            } else if (state == State.shift) {
            } else if (state == State.settings) {
                if (dispmode == DispMode.plain) dispmode = DispMode.sci;
                else dispmode = DispMode.plain;

                spedit.putString("DisplayMode", dispmode_string(dispmode));
                spedit.apply();
            }
        } else if (vid == R.id.tan) {
            if (state == State.normal) {
            } else if (state == State.shift) {
            } else if (state == State.settings) {
                if (tp_idx == tp_values.length-1) tp_idx = 0;
                else tp_idx++;

                spedit.putInt("TP", tp_idx);
                spedit.apply();
            }
        } else if (vid == R.id.ln) {
            if (state == State.normal) {
            } else if (state == State.shift) {
            } else if (state == State.settings) {
                if (dp_idx == dp_values.length - 1) dp_idx = 0;
                else dp_idx++;

                spedit.putInt("DP", dp_idx);
                spedit.apply();
            }
        } else if (vid == R.id.log) {
            if (state == State.normal) {
            } else if (state == State.shift) {
            } else if (state == State.settings) {
                if (vibration_millis_idx == vibration_millis_values.length-1) vibration_millis_idx = 0;
                else vibration_millis_idx++;

                spedit.putInt("VibrationMillis", vibration_millis_idx);
                spedit.apply();
            }
        } else if (vid == R.id.enter) {
            if (state == State.normal) {
                validatePush();
            } else if (state == State.shift) {
                updateState(State.settings);
            } else if (state == State.settings) {
                updateState(State.normal);
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
            ((Button)findViewById(R.id.sin)).setText(String.format("%s", anglemode_string(anglemode)));
            ((Button)findViewById(R.id.cos)).setText(String.format("Mode: %s", dispmode_string(dispmode)));
            ((Button)findViewById(R.id.tan)).setText(String.format("TP: %d", tp_values[tp_idx]));
            ((Button)findViewById(R.id.ln)).setText(String.format("DP: %d", dp_values[dp_idx]));
            ((Button)findViewById(R.id.log)).setText(String.format("Vibration: %dms", vibration_millis_values[vibration_millis_idx]));
            adapter.notifyItemRangeChanged(0, data.size());

            ((Button)findViewById(R.id.tan)).setEnabled(dispmode != DispMode.plain);
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
                if (op == Op.add) { n1.val = n1.val.add(n2.val);                          n1.latex = n1.latex + "+" + n2.latex; }
                else if (op == Op.sub) { n1.val = n1.val.subtract(n2.val);                n1.latex = n1.latex + "-" + n2.latex; }
                else if (op == Op.mul) { n1.val = n1.val.multiply(n2.val);                n1.latex = n1.latex + "\\cdot " + n2.latex; }
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
            ((Button)findViewById(R.id.tan)).setEnabled(true);
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

            int dotExpIdx = input.indexOf(".E");
            if (dotExpIdx != -1) {
                input.deleteCharAt(dotExpIdx);
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
     * Compute x^exponent to a given scale. Uses the same algorithm as class
     * numbercruncher.mathutils.IntPower.
     *
     * @param x the value x
     * @param exponent the exponent value
     * @param scale the desired scale of the result
     * @return the result value
     */
    public static BigDecimal intPower(BigDecimal x, long exponent, int scale) {
        // If the exponent is negative, compute 1/(x^-exponent).
        if (exponent < 0) {
            return BigDecimal.valueOf(1)
                    .divide(intPower(x, -exponent, scale), scale, BigDecimal.ROUND_HALF_EVEN);
        }

        BigDecimal power = BigDecimal.valueOf(1);

        // Loop to compute value^exponent.
        while (exponent > 0) {

            // Is the rightmost bit a 1?
            if ((exponent & 1) == 1) {
                power = power.multiply(x).setScale(scale, BigDecimal.ROUND_HALF_EVEN);
            }

            // Square x and shift exponent 1 bit to the right.
            x = x.multiply(x).setScale(scale, BigDecimal.ROUND_HALF_EVEN);
            exponent >>= 1;

            Thread.yield();
        }

        return power;
    }

    /**
     * Compute the integral root of x to a given scale, x >= 0.
     * Use Newton's algorithm.
     * @param x the value of x
     * @param index the integral root value
     * @param scale the desired scale of the result
     * @return the result value
     */
    public static BigDecimal intRoot(BigDecimal x, long index,
                                     int scale)
    {
        // Check that x >= 0.
        if (x.signum() < 0) {
            throw new IllegalArgumentException("x < 0");
        }

        int        sp1 = scale + 1;
        BigDecimal n   = x;
        BigDecimal i   = BigDecimal.valueOf(index);
        BigDecimal im1 = BigDecimal.valueOf(index-1);
        BigDecimal tolerance = BigDecimal.valueOf(5)
                .movePointLeft(sp1);
        BigDecimal xPrev;

        // The initial approximation is x/index.
        x = x.divide(i, scale, BigDecimal.ROUND_HALF_EVEN);

        // Loop until the approximations converge
        // (two successive approximations are equal after rounding).
        do {
            // x^(index-1)
            BigDecimal xToIm1 = intPower(x, index-1, sp1);

            // x^index
            BigDecimal xToI =
                    x.multiply(xToIm1)
                            .setScale(sp1, BigDecimal.ROUND_HALF_EVEN);

            // n + (index-1)*(x^index)
            BigDecimal numerator =
                    n.add(im1.multiply(xToI))
                            .setScale(sp1, BigDecimal.ROUND_HALF_EVEN);

            // (index*(x^(index-1))
            BigDecimal denominator =
                    i.multiply(xToIm1)
                            .setScale(sp1, BigDecimal.ROUND_HALF_EVEN);

            // x = (n + (index-1)*(x^index)) / (index*(x^(index-1)))
            xPrev = x;
            x = numerator
                    .divide(denominator, sp1, BigDecimal.ROUND_DOWN);

            Thread.yield();
        } while (x.subtract(xPrev).abs().compareTo(tolerance) > 0);

        return x;
    }

    /* Compute the natural logarithm of x to a given scale, x > 0. Use Newton's algorithm. */
    private static BigDecimal lnNewton(BigDecimal x, int scale) {
        int sp1 = scale + 1;
        BigDecimal n = x;
        BigDecimal term;

        // Convergence tolerance = 5*(10^-(scale+1))
        BigDecimal tolerance = BigDecimal.valueOf(5).movePointLeft(sp1);

        // Loop until the approximations converge
        // (two successive approximations are within the tolerance).
        do {

            // e^x
            BigDecimal eToX = exp(x, sp1);

            // (e^x - n)/e^x
            term = eToX.subtract(n).divide(eToX, sp1, BigDecimal.ROUND_DOWN);

            // x - (e^x - n)/e^x
            x = x.subtract(term);

            Thread.yield();
        } while (term.compareTo(tolerance) > 0);

        return x.setScale(scale, BigDecimal.ROUND_HALF_EVEN);
    }

    public static BigDecimal cosine(BigDecimal x) {
        BigDecimal currentValue = BigDecimal.ONE;
        BigDecimal lastVal = currentValue.add(BigDecimal.ONE);
        BigDecimal xSquared = x.multiply(x);
        BigDecimal numerator = BigDecimal.ONE;
        BigDecimal denominator = BigDecimal.ONE;
        int i = 0;

        while (lastVal.compareTo(currentValue) != 0) {
            lastVal = currentValue;

            int z = 2 * i + 2;

            denominator = denominator.multiply(BigDecimal.valueOf(z));
            denominator = denominator.multiply(BigDecimal.valueOf(z - 1));
            numerator = numerator.multiply(xSquared);

            BigDecimal term = numerator.divide(denominator, SCALE + 5, ROUNDING_MODE);

            if (i % 2 == 0) {
                currentValue = currentValue.subtract(term);
            } else {
                currentValue = currentValue.add(term);
            }
            i++;
        }

        return currentValue;
    }

    public static BigDecimal sine(BigDecimal x) {
        BigDecimal lastVal = x.add(BigDecimal.ONE);
        BigDecimal currentValue = x;
        BigDecimal xSquared = x.multiply(x);
        BigDecimal numerator = x;
        BigDecimal denominator = BigDecimal.ONE;
        int i = 0;

        while (lastVal.compareTo(currentValue) != 0) {
            lastVal = currentValue;

            int z = 2 * i + 3;

            denominator = denominator.multiply(BigDecimal.valueOf(z));
            denominator = denominator.multiply(BigDecimal.valueOf(z - 1));
            numerator = numerator.multiply(xSquared);

            BigDecimal term = numerator.divide(denominator, SCALE + 5, ROUNDING_MODE);

            if (i % 2 == 0) {
                currentValue = currentValue.subtract(term);
            } else {
                currentValue = currentValue.add(term);
            }

            i++;
        }
        return currentValue;
    }

    public static BigDecimal tangent(BigDecimal x) {

        BigDecimal sin = sine(x);
        BigDecimal cos = cosine(x);

        return sin.divide(cos, SCALE, BigDecimal.ROUND_HALF_UP);
    }

    public static BigDecimal log10(BigDecimal b) {
        final int NUM_OF_DIGITS = SCALE + 2;
        // need to add one to get the right number of dp
        // and then add one again to get the next number
        // so I can round it correctly.

        MathContext mc = new MathContext(NUM_OF_DIGITS, RoundingMode.HALF_EVEN);
        // special conditions:
        // log(-x) -> exception
        // log(1) == 0 exactly;
        // log of a number less than one = -log(1/x)
        if (b.signum() <= 0) {
            throw new ArithmeticException("log of a negative number! (or zero)");
        } else if (b.compareTo(BigDecimal.ONE) == 0) {
            return BigDecimal.ZERO;
        } else if (b.compareTo(BigDecimal.ONE) < 0) {
            return (log10((BigDecimal.ONE).divide(b, mc))).negate();
        }

        StringBuilder sb = new StringBuilder();
        // number of digits on the left of the decimal point
        int leftDigits = b.precision() - b.scale();

        // so, the first digits of the log10 are:
        sb.append(leftDigits - 1).append(".");

        // this is the algorithm outlined in the webpage
        int n = 0;
        while (n < NUM_OF_DIGITS) {
            b = (b.movePointLeft(leftDigits - 1)).pow(10, mc);
            leftDigits = b.precision() - b.scale();
            sb.append(leftDigits - 1);
            n++;
        }

        BigDecimal ans = new BigDecimal(sb.toString());

        // Round the number to the correct number of decimal places.
        ans = ans.round(new MathContext(ans.precision() - ans.scale() + SCALE, RoundingMode.HALF_EVEN));
        return ans;
    }

    public static BigDecimal asin(BigDecimal val) {
        return BigDecimal.valueOf(Math.asin(val.doubleValue()));
    }

    public static BigDecimal acos(BigDecimal val) {
        return BigDecimal.valueOf(Math.acos(val.doubleValue()));
    }

    public static BigDecimal atan(BigDecimal val) {
        return BigDecimal.valueOf(Math.atan(val.doubleValue()));
    }

    /**
     * Compute e^x to a given scale by the Taylor series.
     *
     * @param x the value of x
     * @param scale the desired scale of the result
     * @return the result value
     */
    private static BigDecimal expTaylor(BigDecimal x, int scale) {
        BigDecimal factorial = BigDecimal.valueOf(1);
        BigDecimal xPower = x;
        BigDecimal sumPrev;

        // 1 + x
        BigDecimal sum = x.add(BigDecimal.valueOf(1));

        // Loop until the sums converge
        // (two successive sums are equal after rounding).
        int i = 2;
        do {
            // x^i
            xPower = xPower.multiply(x).setScale(scale, BigDecimal.ROUND_HALF_EVEN);

            // i!
            factorial = factorial.multiply(BigDecimal.valueOf(i));

            // x^i/i!
            BigDecimal term = xPower.divide(factorial, scale, BigDecimal.ROUND_HALF_EVEN);

            // sum = sum + x^i/i!
            sumPrev = sum;
            sum = sum.add(term);

            ++i;
            Thread.yield();
        } while (sum.compareTo(sumPrev) != 0);

        return sum;
    }


    /**
     * Compute e^x to a given scale. Break x into its whole and fraction parts and compute (e^(1 +
     * fraction/whole))^whole using Taylor's formula.
     *
     * @param x the value of x
     * @param scale the desired scale of the result
     * @return the result value
     */
    public static BigDecimal exp(BigDecimal x, int scale) {
        // e^0 = 1
        if (x.signum() == 0) {
            return BigDecimal.valueOf(1);
        }

        // If x is negative, return 1/(e^-x).
        else if (x.signum() == -1) {
            return BigDecimal.valueOf(1)
                    .divide(exp(x.negate(), scale), scale, BigDecimal.ROUND_HALF_EVEN);
        }

        // Compute the whole part of x.
        BigDecimal xWhole = x.setScale(0, BigDecimal.ROUND_DOWN);

        // If there isn't a whole part, compute and return e^x.
        if (xWhole.signum() == 0) {
            return expTaylor(x, scale);
        }

        // Compute the fraction part of x.
        BigDecimal xFraction = x.subtract(xWhole);

        // z = 1 + fraction/whole
        BigDecimal z =
                BigDecimal.valueOf(1)
                        .add(xFraction.divide(xWhole, scale, BigDecimal.ROUND_HALF_EVEN));

        // t = e^z
        BigDecimal t = expTaylor(z, scale);

        BigDecimal maxLong = BigDecimal.valueOf(Long.MAX_VALUE);
        BigDecimal result = BigDecimal.valueOf(1);

        // Compute and return t^whole using intPower().
        // If whole > Long.MAX_VALUE, then first compute products
        // of e^Long.MAX_VALUE.
        while (xWhole.compareTo(maxLong) >= 0) {
            result =
                    result.multiply(intPower(t, Long.MAX_VALUE, scale))
                            .setScale(scale, BigDecimal.ROUND_HALF_EVEN);
            xWhole = xWhole.subtract(maxLong);

            Thread.yield();
        }
        return result.multiply(intPower(t, xWhole.longValue(), scale))
                .setScale(scale, BigDecimal.ROUND_HALF_EVEN);
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

    public void vibrateWith(int millis) {
        Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            v.vibrate(VibrationEffect.createOneShot(millis, VibrationEffect.DEFAULT_AMPLITUDE));
        } else {
            v.vibrate(millis);
        }
    }

    public enum DispMode {
        plain,
        sci,
    }

    public enum AngleMode {
        degrees,
        radians,
    }

    public String anglemode_string(AngleMode mode) {
        switch (mode) {
            case degrees: return "DEG";
            case radians: return "RAD";
        }
        throw new IllegalArgumentException();
    }

    public AngleMode anglemode_from_string(String mode) {
        String modeUpper = mode.toUpperCase();
        if (modeUpper.equals("DEG")) return AngleMode.degrees;
        else if (modeUpper.equals("RAD")) return AngleMode.radians;
        else throw new IllegalArgumentException();
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
