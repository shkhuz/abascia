package com.shkhuz.abascia;

import android.annotation.SuppressLint;
import android.content.ClipData;
import android.content.ClipDescription;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.os.Build;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.text.HtmlCompat;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Stack;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import ru.noties.jlatexmath.JLatexMathView;

import ch.obermuhlner.math.big.BigDecimalMath;

enum State {
    shift,
    normal,
    settings,
}

public class Main extends AppCompatActivity implements View.OnKeyListener {
    public enum MsgKind {
        PINNED_ERROR,
        PINNED_INFO,
        ON_STACK_ERROR,
        NONE,
    }

    public class Conversions {
        public List<ConvGroup> convGroups;
        public CharSequence[] convGroupNames;
        public Conversions() {
            this.convGroups = new ArrayList<>();
        }
    }

    public class ConvGroup {
        public CharSequence[] convUnitDisplays;
        public List<ConvUnit> convUnits;
        public ConvGroup() {
            this.convUnits = new ArrayList<>();
        }
    }

    public class ConvUnit {
        public String name;
        public String symbol;
        public BigDecimal ratio;
        public BigDecimal offset;
        public boolean inverse;
    }

    private RecyclerView stackview;
    private Stack<ViewModel> data = new Stack<>();
    private CustomAdapter adapter;
    public StringBuffer input;
    public TextView msgView;
    public TextView anglemodeTextView;
    private MsgKind msgKind = MsgKind.NONE;
    public int volatile_idx = -1;
    public final int INTERNAL_SCALE = 32;
    private State state = State.normal;

    private CalculatorKeyLayout keygrid;
    private ArrayList<CharSequence> initial_keygrid_texts = new ArrayList<>();
    private ArrayList<Boolean> initial_keygrid_styles = new ArrayList<>();
    private ColorStateList default_button_color;
    private ColorStateList default_msgkind_fgcolor;
    public SharedPreferences sp;
    public SharedPreferences.Editor spedit;
    private static final int ROUNDING_MODE = BigDecimal.ROUND_HALF_EVEN;
    private static final int SCALE = 18;
    private MathContext mc = new MathContext(INTERNAL_SCALE, RoundingMode.HALF_EVEN);

    private ClipboardManager clipMgr;

    public class History {
        Stack<ViewModel> data;
        StringBuffer input;
        MsgKind error;
        int volatile_idx;
    }

    private Stack<History> history = new Stack<>();

    Conversions convs = new Conversions();

    private Button sinButton;
    private Button cosButton;
    private Button tanButton;
    private Button lnButton;
    private Button logButton;
    private Button dropButton;
    private Button shiftButton;
    private Button pointButton;
    private Button delButton;

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
            0, 20, 25, 30, 35, 40, 45, 50,
    };

    private String[] normal_texts = {
            "SIN", "COS", "TAN", "LN", "LOG",
            "DROP", "SWAP", "UNDO", "<big>&#x221a;</big>"/*sqrt*/, "<big>&#x2093;&#xb2;</big>"/*x^2*/,
            "SHIFT", "7", "8", "9", "<big>&#x00F7;</big>"/*div*/,
            "DEL", "4", "5", "6", "<big>&#x00D7;</big>"/*star*/,
            "E<br>N<br>T<br>E<br>R", "1", "2", "3", "<big>&#x2212;</big>"/*minus*/,
                                     "0", ".", "&#x00b1;"/*plusminus*/, "<big>+</big>",
    };

    private int[] normal_styles = {
            0, 0, 0, 0, 0,
            0, 0, 0, 0, 0,
            1, 2, 2, 2, 2,
            1, 2, 2, 2, 2,
            1, 2, 2, 2, 2,
               2, 2, 2, 2,
    };

    private String[] shift_texts = {
            "&#x2699;"/*gear*/, "", "", "COPY", "PASTE",
            "", "", "", "", "<big>&#x2093;&#x207F;</big>"/*x^n*/,
            "SHIFT", "<small>&#x2093;&#xb3;</small>"/*x^3*/, "<small>&#x221B;</small>"/*cuberoot*/, "", "",
            "CLR", "", "", "", "",
            "D<br>U<br>P", "&#x3C0;"/*pi*/, "e", "", "",
                "CONV", "", "", "",
    };

    private int[] shift_styles = {
            1, 0, 0, 0, 0,
            0, 0, 0, 0, 0,
            1, 2, 2, 2, 2,
            1, 2, 2, 2, 2,
            1, 2, 2, 2, 2,
               1, 2, 2, 2,
    };

    private String[] settings_texts = {
            "BACK", "", "", "", "",
            "", "", "", "", "",
            "", "", "", "", "",
            "", "", "", "", "",
            "", "", "", "", "",
                "", "", "", "",
    };

    public enum CopyItemsMode {
        copy_last_item,
        copy_all_items,
    }

    private CharSequence[] copy_items_text = {
            "Copy last item",
            "Copy all items",
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

    public class ConvRatioParser {
        public int current;
        public BigDecimal val;
        public String input;
        public int inputLength;

        public ConvRatioParser(String input) {
            current = 0;
            this.input = input;
            this.input = this.input.replace(",", "");
            inputLength = this.input.length();
        }

        private BigDecimal parseNumber() {
            int from = current;
            while (current < input.length() && input.charAt(current) != '*' && input.charAt(current) != '/' && !Character.isWhitespace(input.charAt(current)))
                current++;
            String toParse = input.substring(from, current);
            return new BigDecimal(toParse);
        }

        private void skipWhitespace() {
            while (Character.isWhitespace(input.charAt(current))) current++;
        }

        public BigDecimal parse() {
            while (current != inputLength) {
                if (input.charAt(current) == '*') {
                    current++;
                    skipWhitespace();
                    BigDecimal operand2 = parseNumber();
                    val = val.multiply(operand2);
                } else if (input.charAt(current) == '/') {
                    current++;
                    skipWhitespace();
                    BigDecimal operand2 = parseNumber();
                    val = val.divide(operand2, INTERNAL_SCALE, RoundingMode.HALF_EVEN);
                } else if (Character.isWhitespace(input.charAt(current))) {
                    skipWhitespace();
                } else {
                    val = parseNumber();
                }
            }
            return val;
        }
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
        this.msgView = findViewById(R.id.errorView);
        this.anglemodeTextView = findViewById(R.id.anglemodeTextView);

        this.sp = PreferenceManager.getDefaultSharedPreferences(this);
        this.spedit = sp.edit();

        this.sinButton = (Button)findViewById(R.id.sin);
        this.cosButton = (Button)findViewById(R.id.cos);
        this.tanButton = (Button)findViewById(R.id.tan);
        this.lnButton = (Button)findViewById(R.id.ln);
        this.logButton = (Button)findViewById(R.id.log);
        this.dropButton = (Button)findViewById(R.id.drop);
        this.shiftButton = findViewById(R.id.shift);
        this.pointButton = findViewById(R.id.point);
        this.delButton = findViewById(R.id.del);

        this.input = new StringBuffer(32);
        this.keygrid = findViewById(R.id.keygrid);
        this.default_button_color = pointButton.getTextColors();
        this.default_msgkind_fgcolor = msgView.getTextColors();

        anglemode = anglemode_from_string(sp.getString("AngleMode", "DEG"));
        dispmode = dispmode_from_string(sp.getString("DisplayMode", "SCI"));
        tp_idx = sp.getInt("TP", 3);
        dp_idx = sp.getInt("DP", 1);
        vibration_millis_idx = sp.getInt("VibrationMillis", 1);

        anglemodeTextView.setText(anglemode_string(anglemode));

        updateState(State.normal);
        this.clipMgr = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);

        List<CharSequence> convGroupNamesList = new ArrayList<>();
        try {
            XmlPullParser x = getResources().getXml(R.xml.conv_const_data);
            int ev = x.getEventType();
            while (ev != XmlPullParser.END_DOCUMENT) {
                if (ev == XmlPullParser.START_TAG && x.getName().equals("conversions")) {
                    while ((ev = x.next()) == XmlPullParser.START_TAG && x.getName().equals("group")) {
                        ConvGroup convGroup = new ConvGroup();
                        List<CharSequence> convUnitDisplaysList = new ArrayList<>();
                        assert(x.getAttributeCount() == 1);
                        convGroupNamesList.add(x.getAttributeValue(0));
                        while ((ev = x.next()) == XmlPullParser.START_TAG && x.getName().equals("unit")) {
                            ConvUnit convUnit = new ConvUnit();
                            for (int i = 0; i < x.getAttributeCount(); i++) {
                                String attr = x.getAttributeName(i);
                                switch (attr) {
                                    case "name":
                                        convUnit.name = x.getAttributeValue(i);
                                        break;
                                    case "symbol":
                                        convUnit.symbol = x.getAttributeValue(i);
                                        break;
                                    case "ratio":
                                        ConvRatioParser convRatioParser = new ConvRatioParser(x.getAttributeValue(i));
                                        convUnit.ratio = convRatioParser.parse();
                                        break;
                                    case "offset":
                                        convUnit.offset = new BigDecimal(x.getAttributeValue(i));
                                        break;
                                    case "inverse":
                                        convUnit.inverse = x.getAttributeValue(i).equalsIgnoreCase("true");
                                        break;
                                    default:
                                        throw new RuntimeException("Unknown unit attribute");
                                }
                            }
                            convGroup.convUnits.add(convUnit);
                            convUnitDisplaysList.add(String.format("%s (%s)", convUnit.symbol, convUnit.name));
                            ev = x.next();
                            assert(ev == XmlPullParser.END_TAG && x.getName().equals("unit"));
                        }
                        convGroup.convUnitDisplays = convUnitDisplaysList.toArray(new CharSequence[0]);
                        convs.convGroups.add(convGroup);
                        assert(ev == XmlPullParser.END_TAG && x.getName().equals("group"));
                    }
                    assert(ev == XmlPullParser.END_TAG && x.getName().equals("conversions"));
                }
                ev = x.next();
            }
        } catch (XmlPullParserException | IOException e) {
            throw new RuntimeException(e);
        }
        convs.convGroupNames = convGroupNamesList.toArray(new CharSequence[0]);

        Eula eula = new Eula(this);
        eula.showIfNotAccepted();
    }

    @SuppressLint("NotifyDataSetChanged")
    public void clickHandler(View view) {
        int vid = view.getId();

        if (msgKind == MsgKind.PINNED_ERROR || msgKind == MsgKind.PINNED_INFO) {
            msgKind = MsgKind.NONE;
            msgView.setText("Abascia");
            msgView.setTextColor(default_msgkind_fgcolor);
        } else if (msgKind == MsgKind.ON_STACK_ERROR) {
            if (vid == R.id.drop) {
                popAndClearInput();
                msgKind = MsgKind.NONE;
                return;
            } else if (vid == R.id.undo) {
            } else return;
        }
        if (msgKind == MsgKind.NONE && vibration_millis_values[vibration_millis_idx] != 0)
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
                    if (pushOk(p)) {
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
                    if (pushOk(p)) {
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
                    if (pushOk(validatePush()) && validateNItemsOnStack(1)) {
                        pushHistory();
                        ViewModel n = data.pop();
                        notifyAdapterItemRemoved(data.size() - 1);
                        try {
                            if (n.lastOp != Op.none) addParams(n);
                            n.lastToLastOp = n.lastOp;
                            n.lastOp = op_from_id(vid, state);
                            n.latex = n.latex + "^{3}";
                            n.val = n.val.pow(3);
                            data.push(n);
                            adapter.notifyItemInserted(data.size() - 1);
                        } catch (RuntimeException e) {
                            showOnStackError(e.getMessage());
                        }
                    }
                } else if (number == 8) {
                    if (pushOk(validatePush()) && validateNItemsOnStack(1)) {
                        pushHistory();
                        ViewModel n = data.pop();
                        notifyAdapterItemRemoved(data.size() - 1);
                        try {
                            n.lastToLastOp = n.lastOp;
                            n.lastOp = op_from_id(vid, state);
                            n.latex = "\\sqrt[3]{" + n.latex + "}";
                            n.val = intRoot(n.val, 3, INTERNAL_SCALE);
                            data.push(n);
                            adapter.notifyItemInserted(data.size() - 1);
                        } catch (RuntimeException e) {
                            showOnStackError(e.getMessage());
                        }
                    }
                } else if (number == 0) {
                    State stateAtClick = state;
                    if (pushOk(validatePush()) && validateNItemsOnStack(1)) {
                        pushHistory();
                        AlertDialog.Builder b = new AlertDialog.Builder(this);
                        b.setTitle("Convert");
                        b.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                dialogInterface.dismiss();
                            }
                        });
                        b.setItems(convs.convGroupNames, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                final int rootIdx = i;
                                AlertDialog.Builder bf = new AlertDialog.Builder(Main.this);
                                bf.setTitle("From");
                                bf.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialogInterface, int i) {
                                        dialogInterface.dismiss();
                                    }
                                });
                                bf.setNeutralButton("Back", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialogInterface, int i) {
                                        dialogInterface.dismiss();
                                        b.show();
                                    }
                                });
                                bf.setItems(convs.convGroups.get(rootIdx).convUnitDisplays, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialogInterface, int i) {
                                        final int fromIdx = i;
                                        AlertDialog.Builder bt = new AlertDialog.Builder(Main.this);
                                        bt.setTitle("To");
                                        bt.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialogInterface, int i) {
                                                dialogInterface.dismiss();
                                            }
                                        });
                                        bt.setNeutralButton("Back", new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialogInterface, int i) {
                                                dialogInterface.dismiss();
                                                bf.show();
                                            }
                                        });
                                        bt.setItems(convs.convGroups.get(rootIdx).convUnitDisplays, new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialogInterface, int i) {
                                                final int toIdx = i;
                                                //result = (input - src.offset) * (src.ratio / dst.ratio) + dst.offset
                                                //        - if 'src' is an inverse unit, then 1/input will be used in the above formula
                                                //        - if 'dst' is an inverse unit, then 1/result will be returned
                                                ConvUnit src = convs.convGroups.get(rootIdx).convUnits.get(fromIdx);
                                                ConvUnit dst = convs.convGroups.get(rootIdx).convUnits.get(toIdx);

                                                ViewModel n = data.pop();
                                                notifyAdapterItemRemoved(data.size() - 1);
                                                try {
                                                    addParams(n);
                                                    n.lastToLastOp = n.lastOp;
                                                    n.lastOp = op_from_id(vid, stateAtClick);

                                                    BigDecimal input = src.inverse ? BigDecimal.ONE.divide(n.val, mc) : n.val;
                                                    BigDecimal result = input
                                                            .subtract(src.offset == null ? BigDecimal.ZERO : src.offset)
                                                            .multiply(src.ratio.divide(dst.ratio, mc))
                                                            .add(dst.offset == null ? BigDecimal.ZERO : dst.offset);
                                                    if (dst.inverse)
                                                        result = BigDecimal.ONE.divide(result, mc);
                                                    n.val = result;
                                                    n.latex = n.latex + String.format("\\text{\\small{%s→%s}}", src.symbol, dst.symbol);

                                                    data.push(n);
                                                    adapter.notifyItemInserted(data.size() - 1);
                                                } catch (RuntimeException e) {
                                                    showOnStackError(e.getMessage());
                                                }
                                            }
                                        });
                                        bt.show();
                                    }
                                });
                                bf.show();
                            }
                        });
                        b.show();
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
                if (pushOk(validatePush()) && validateNItemsOnStack(1)) {
                    pushHistory();
                    ViewModel n = data.pop();
                    notifyAdapterItemRemoved(data.size() - 1);
                    try {
                        if (n.lastOp != Op.none) addParams(n);
                        n.lastToLastOp = n.lastOp;
                        n.lastOp = op_from_id(vid, state);
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
                updateState(State.settings);
            } else if (state == State.settings) {
                updateState(State.normal);
            }
        } else if (vid == R.id.cos) {
            if (state == State.normal) {
                if (pushOk(validatePush()) && validateNItemsOnStack(1)) {
                    pushHistory();
                    ViewModel n = data.pop();
                    notifyAdapterItemRemoved(data.size() - 1);
                    try {
                        if (n.lastOp != Op.none) addParams(n);
                        n.lastToLastOp = n.lastOp;
                        n.lastOp = op_from_id(vid, state);
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
                if (pushOk(validatePush()) && validateNItemsOnStack(1)) {
                    pushHistory();
                    ViewModel n = data.pop();
                    notifyAdapterItemRemoved(data.size() - 1);
                    try {
                        if (n.lastOp != Op.none) addParams(n);
                        n.lastToLastOp = n.lastOp;
                        n.lastOp = op_from_id(vid, state);
                        if (anglemode == AngleMode.degrees) {
                            n.latex = "\\tan^{\\circ}{" + n.latex + "}";
                            n.val = BigDecimalMath.tan(BigDecimalMath.toRadians(n.val, mc), mc);
                        } else if (anglemode == AngleMode.radians) {
                            n.latex = "\\tan{" + n.latex + "}";
                            n.val = BigDecimalMath.tan(n.val, mc);
                        }
                        data.push(n);
                        adapter.notifyItemInserted(data.size() - 1);
                    } catch (RuntimeException e) {
                        showOnStackError(e.getMessage());
                    }
                }
            } else if (state == State.shift) {
            } else if (state == State.settings) {
                if (tp_idx == tp_values.length-1) tp_idx = 0;
                else tp_idx++;

                spedit.putInt("TP", tp_idx);
                spedit.apply();
            }
        } else if (vid == R.id.ln) {
            if (state == State.normal) {
                if (pushOk(validatePush()) && validateNItemsOnStack(1)) {
                    pushHistory();
                    ViewModel n = data.pop();
                    notifyAdapterItemRemoved(data.size() - 1);
                    try {
                        if (n.lastOp != Op.none) addParams(n);
                        n.lastToLastOp = n.lastOp;
                        n.lastOp = op_from_id(vid, state);
                        n.latex = "\\ln{" + n.latex + "}";
                        n.val = BigDecimalMath.log(n.val, mc);
                        data.push(n);
                        adapter.notifyItemInserted(data.size() - 1);
                    } catch (RuntimeException e) {
                        showOnStackError(e.getMessage());
                    }
                }
            } else if (state == State.shift) {
                if (data.size() == 0) {
                    showPinnedError("Nothing to copy");
                } else if (pushOk(validatePush())) {
                    if (data.size() > 1) {
                        AlertDialog.Builder b = new AlertDialog.Builder(this);
                        b.setTitle("Copy items");
                        b.setItems(copy_items_text, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                CopyItemsMode mode = CopyItemsMode.values()[which];
                                switch (mode) {
                                    case copy_last_item:
                                        copyStringToClip(convertNumberToString(data.lastElement().val));
                                        break;

                                    case copy_all_items: {
                                        StringBuilder all_items = new StringBuilder();
                                        for (int i = 0; i < data.size(); i++) {
                                            all_items.append(convertNumberToString(data.get(i).val));
                                            all_items.append('\n');
                                        }
                                        copyStringToClip(all_items.toString());
                                    } break;
                                }
                            }
                        });
                        b.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        });
                        b.show();
                    } else {
                        copyStringToClip(convertNumberToString(data.lastElement().val));
                    }
                }
            } else if (state == State.settings) {
                if (dp_idx == dp_values.length - 1) dp_idx = 0;
                else dp_idx++;

                spedit.putInt("DP", dp_idx);
                spedit.apply();
            }
        } else if (vid == R.id.log) {
            if (state == State.normal) {
                if (pushOk(validatePush()) && validateNItemsOnStack(1)) {
                    pushHistory();
                    ViewModel n = data.pop();
                    notifyAdapterItemRemoved(data.size() - 1);
                    try {
                        if (n.lastOp != Op.none) addParams(n);
                        n.lastToLastOp = n.lastOp;
                        n.lastOp = op_from_id(vid, state);
                        n.latex = "\\log{" + n.latex + "}";
                        n.val = BigDecimalMath.log10(n.val, mc);
                        data.push(n);
                        adapter.notifyItemInserted(data.size() - 1);
                    } catch (RuntimeException e) {
                        showOnStackError(e.getMessage());
                    }
                }
            } else if (state == State.shift) {
                if (pushOk(validatePush())) {
                    pushHistory();
                    ClipData.Item clipItem = Objects.requireNonNull(clipMgr.getPrimaryClip()).getItemAt(0);
                    CharSequence pasteDataRaw = clipItem.getText();
                    if (pasteDataRaw == null) {
                        showOnStackError("Paste failed: non-text clipboard input");
                    } else {
                        List<String> pasteItems = new LinkedList<>(Arrays.asList(pasteDataRaw
                                .toString()
                                .replaceAll(",", "")
                                .split("\n")));
                        pasteItems.removeAll(Arrays.asList("", null));

                        List<BigDecimal> decimals = new ArrayList<>();
                        try {
                            for (String item: pasteItems) {
                                decimals.add(new BigDecimal(item));
                            }

                            for (int i = 0; i < decimals.size(); i++) {
                                ViewModel m = new ViewModel();
                                m.val = decimals.get(i);
                                m.latex = formatInputToLatex(pasteItems.get(i));
                                data.push(m);
                                adapter.notifyItemInserted(data.size()-1);
                            }
                        } catch (NumberFormatException e) {
                            showOnStackError("Paste failed: non-numerical or invalid input provided");
                        }
                    }
                }
            } else if (state == State.settings) {
                if (vibration_millis_idx == vibration_millis_values.length-1) vibration_millis_idx = 0;
                else vibration_millis_idx++;

                spedit.putInt("VibrationMillis", vibration_millis_idx);
                spedit.apply();
            }
        } else if (vid == R.id.enter) {
            if (state == State.normal) {
                // This automatically creates history.
                validatePush();
            } else if (state == State.shift) {
                if (pushOk(validatePush()) && validateNItemsOnStack(1)) {
                    pushHistory();
                    ViewModel n = data.pop();
                    notifyAdapterItemRemoved(data.size()-1);
                    data.push(n);
                    data.push(n.clone());
                    adapter.notifyItemRangeInserted(data.size()-1, 2);
                }
            }
        } else if ((vid == R.id.plus || vid == R.id.minus || vid == R.id.mult || vid == R.id.div)) {
            if (state == State.normal) {
                binaryOp(op_from_id(vid, state));
            } else if (state == State.shift) {
            } else if (state == State.settings) {
            }
        } else if (vid == R.id.chs) {
            if (state == State.normal) {
                if (volatile_idx == -1 && validateNItemsOnStack(1)) {
                    pushHistory();
                    ViewModel n = data.get(data.size()-1);
                    if (n.latex.startsWith("-")) {
                        StringBuilder s = new StringBuilder(n.latex).deleteCharAt(0);
                        n.latex = s.toString();
                        // We don't do n.lastToLastOp = n.lastOp here
                        n.lastOp = n.lastToLastOp;
                    } else {
                        if ((n.lastOp == Op.conv || n.lastOp == Op.add || n.lastOp == Op.sub)) {
                            addParamsIfNeeded(n, op_from_id(vid, state), false);
                        }
                        n.lastToLastOp = n.lastOp;
                        n.lastOp = op_from_id(vid, state);
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
                // TODO: should we push history even if stack is empty?
                pushHistory();
                volatile_idx = -1;
                data.clear();
                adapter.notifyItemRangeRemoved(0, data.size());
                input.setLength(0);
            } else if (state == State.settings) {
            }
        } else if (vid == R.id.pow2) {
            if (state == State.normal) {
                if (pushOk(validatePush()) && validateNItemsOnStack(1)) {
                    pushHistory();
                    ViewModel n = data.pop();
                    notifyAdapterItemRemoved(data.size() - 1);
                    try {
                        if (n.lastOp != Op.none) addParams(n);
                        n.lastToLastOp = n.lastOp;
                        n.lastOp = op_from_id(vid, state);
                        n.latex = n.latex + "^{2}";
                        n.val = n.val.pow(2);
                        data.push(n);
                        adapter.notifyItemInserted(data.size() - 1);
                    } catch (ArithmeticException e) {
                        showOnStackError(e.getMessage());
                    }
                }
            } else if (state == State.shift) {
                if (pushOk(validatePush()) && validateNItemsOnStack(2)) {
                    pushHistory();
                    // TODO: store viewmodel instead of creating anew
                    ViewModel exp = data.pop();
                    ViewModel n = data.pop();
                    notifyAdapterItemRangeRemoved(data.size()-2, 2);
                    try {
                        if (n.lastOp != Op.none) addParams(n);
                        n.lastToLastOp = n.lastOp;
                        n.lastOp = op_from_id(vid, state);
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
                if (pushOk(validatePush()) && validateNItemsOnStack(1)) {
                    pushHistory();
                    ViewModel n = data.pop();
                    notifyAdapterItemRemoved(data.size() - 1);
                    try {
                        n.lastToLastOp = n.lastOp;
                        n.lastOp = op_from_id(vid, state);
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
                // This automatically creates history.
                popAndClearInput();
            } else if (state == State.shift) {
            } else if (state == State.settings) {
                if (anglemode == AngleMode.degrees) anglemode = AngleMode.radians;
                else anglemode = AngleMode.degrees;

                spedit.putString("AngleMode", anglemode_string(anglemode));
                spedit.apply();
            }
        } else if (vid == R.id.swap) {
            if (state == State.normal) {
                if (pushOk(validatePush()) && validateNItemsOnStack(2)) {
                    pushHistory();
                    ViewModel n1 = data.pop();
                    ViewModel n2 = data.pop();
                    notifyAdapterItemRangeRemoved(data.size() - 2, 2);
                    data.push(n1);
                    data.push(n2);
                    adapter.notifyItemRangeInserted(data.size() - 1, 2);
                }
            } else if (state == State.shift) {
            } else if (state == State.settings) {
            }
        } else if (vid == R.id.undo) {
            if (state == State.normal) {
                if (history.isEmpty()) {
                    showPinnedError("No previous history");
                } else {
                    History h = popHistory();
                    data = h.data;
                    adapter.data = h.data;
                    msgKind = h.error;
                    input = h.input;
                    volatile_idx = h.volatile_idx;
                    adapter.notifyDataSetChanged();
                    showPinnedInfo("Undid change");
                }

            }
        }

        if (state == State.shift && vid != R.id.shift) updateState(State.normal);

        // State settings
        if (state == State.normal) {
            // We use a volatile element to show stack errors, so
            // here we need an additional check to see if it is a
            // number or an error.
            if (msgKind != MsgKind.ON_STACK_ERROR) {
                if (volatile_idx != -1) {
                    boolean isDecPresent = input.indexOf(".") != -1;
                    boolean isExpPresent = input.indexOf("E") != -1;

                    if (isExpPresent) {
                        pointButton.setText(".");
                        pointButton.setEnabled(false);
                    } else {
                        pointButton.setEnabled(true);
                        if (isDecPresent) pointButton.setText("EE");
                        else pointButton.setText(".");
                    }
                } else {
                    pointButton.setEnabled(true);
                    pointButton.setText(".");
                }
            }
        } else if (state == State.settings) {
            String anglemodestr = anglemode_string(anglemode);
            anglemodeTextView.setText(anglemodestr);
            cosButton.setText(String.format("Mode: %s", dispmode_string(dispmode)));
            tanButton.setText(String.format("TP: %d", tp_values[tp_idx]));
            lnButton.setText(String.format("DP: %d", dp_values[dp_idx]));
            logButton.setText(String.format("Vibr: %dms", vibration_millis_values[vibration_millis_idx]));
            dropButton.setText(String.format("%s", anglemodestr));
            adapter.notifyItemRangeChanged(0, data.size());

            tanButton.setEnabled(dispmode != DispMode.plain);
        }

        // if Ek.OnStackError then scroll to error position
        stackview.scrollToPosition(data.size() - 1);
        Log.d("StackSize", String.format("%d", data.size()));
    }

    private boolean validateNItemsOnStack(int n) {
        if (data.size() >= n) {
            return true;
        } else {
            showPinnedError(String.format(Locale.ENGLISH, "Needs %d arguments", n));
            return false;
        }
    }

    private void pushHistory() {
        History h = new History();
        Stack<ViewModel> dataCopy = new Stack<>();
        for (ViewModel v : data) {
            dataCopy.push(v.clone());
        }
        h.data = dataCopy;
        h.input = new StringBuffer(input);
        h.error = msgKind;
        h.volatile_idx = volatile_idx;
        history.push(h);
    }

    private History popHistory() {
        return history.pop();
    }

    private void binaryOp(Op op) {
        if (pushOk(validatePush()) && validateNItemsOnStack(2)) {
            pushHistory();
            ViewModel n2 = data.pop();
            ViewModel n1 = data.pop();
            notifyAdapterItemRangeRemoved(data.size() - 2, 2);

            try {
                if (op == Op.add || op == Op.sub || op == Op.mul) {
                    addParamsIfNeeded(n1, op, true);
                    addParamsIfNeeded(n2, op, false);
                }
                n1.lastToLastOp = n1.lastOp;
                n1.lastOp = op;
                if (op == Op.add) {
                    n1.val = n1.val.add(n2.val);
                    n1.latex = n1.latex + "+" + n2.latex;
                } else if (op == Op.sub) {
                    n1.val = n1.val.subtract(n2.val);
                    n1.latex = n1.latex + "-" + n2.latex;
                } else if (op == Op.mul) {
                    n1.val = n1.val.multiply(n2.val);
                    n1.latex = n1.latex + "\\cdot " + n2.latex;
                } else {
                    n1.val = n1.val.divide(n2.val, INTERNAL_SCALE, RoundingMode.HALF_EVEN);
                    n1.latex = "\\frac{" + n1.latex + "}{" + n2.latex + "}";
                }
                data.push(n1);
                adapter.notifyItemInserted(data.size() - 1);
            } catch (ArithmeticException e) {
                showOnStackError("Division by zero");
            }
        }
    }

    private int getOpPrecedence(Op op) {
        if (op == Op.conv) return 1;
        else if (op == Op.add || op == Op.sub) return 2;
        else if (op == Op.mul || op == Op.div) return 3;
        else if (op == Op.neg) return 4;
        else return 5;
    }

    private void addParams(ViewModel v) {
         if (!(v.latex.startsWith("\\left(") && v.latex.endsWith("\\right)"))) {
             v.latex = "\\left(" + v.latex + "\\right)";
         }
    }

    private void addParamsIfNeeded(ViewModel v, Op parentOp, boolean isLeftOperand) {
        if (getOpPrecedence(parentOp) > getOpPrecedence(v.lastOp) ||
                (parentOp == Op.sub && (v.lastOp == Op.add || v.lastOp == Op.sub) && !isLeftOperand))
            addParams(v);
    }

    private void updateState(State value) {
        state = value;
        if (state == State.normal) {
            for (int i = 0; i < keygrid.getChildCount(); i++) {
                final Button b = (Button) keygrid.getChildAt(i);
                b.setText(HtmlCompat.fromHtml(normal_texts[i], HtmlCompat.FROM_HTML_MODE_LEGACY));
                b.setTextAppearance(normal_styles[i] == 0
                        ? R.style.CalculatorButtonSmallStyle
                        : normal_styles[i] == 1 ? R.style.CalculatorButtonMediumStyle : R.style.CalculatorButtonLargeStyle);
                b.setTextColor(default_button_color);
            }
            shiftButton.setTextColor(ContextCompat.getColorStateList(this, R.color.shift_normal));
            delButton.setTextColor(ContextCompat.getColorStateList(this, R.color.deletebtn));
            tanButton.setEnabled(true);
            logButton.setEnabled(true);
        } else if (state == State.shift) {
            for (int i = 0; i < keygrid.getChildCount(); i++) {
                final Button b = (Button) keygrid.getChildAt(i);
                b.setText(HtmlCompat.fromHtml(shift_texts[i], HtmlCompat.FROM_HTML_MODE_LEGACY));
                b.setTextAppearance(shift_styles[i] == 0
                        ? R.style.CalculatorButtonSmallStyle
                        : shift_styles[i] == 1 ? R.style.CalculatorButtonMediumStyle : R.style.CalculatorButtonLargeStyle);
                b.setTextColor(default_button_color);
                if (b.getId() == R.id.point) b.setEnabled(true);
            }
            shiftButton.setTextColor(ContextCompat.getColorStateList(this, R.color.shift_pressed));
            delButton.setTextColor(ContextCompat.getColorStateList(this, R.color.deletebtn));
            if (!clipMgr.hasPrimaryClip())
                logButton.setEnabled(false);
            else
                logButton.setEnabled(clipMgr.getPrimaryClipDescription().hasMimeType(ClipDescription.MIMETYPE_TEXT_PLAIN));
        } else if (state == State.settings) {
            for (int i = 0; i < keygrid.getChildCount(); i++) {
                final Button b = (Button) keygrid.getChildAt(i);
                b.setText(HtmlCompat.fromHtml(settings_texts[i], HtmlCompat.FROM_HTML_MODE_LEGACY));
                b.setTextAppearance(R.style.CalculatorButtonSmallStyle);
                b.setTextColor(ContextCompat.getColorStateList(this, R.color.settingbtns));
            }
            logButton.setEnabled(true);
        }
        Log.d("TEXTSIZE", String.format("%f, %f", sinButton.getTextSize(), pointButton.getTextSize()));
    }

    private int getBigDecimalDigits(BigDecimal b) {
        if (b.scale() < 0) {
            return b.precision() + (-b.scale());
        }
        return b.precision();
    }

    private int getBigDecimalDigitsToRight(BigDecimal b) {
        return Math.max(b.scale(), 0);
    }

    public String convertNumberToString(BigDecimal b) {
        b = b.stripTrailingZeros();
        String text;
        int constrained_scale = Math.min(getBigDecimalDigitsToRight(b), dp_values[dp_idx]);
        if (Math.max(getBigDecimalDigits(b) - getBigDecimalDigitsToRight(b) + constrained_scale, constrained_scale) >
                tp_values[tp_idx]
                && dispmode == Main.DispMode.sci) {
            NumberFormat formatter = new DecimalFormat("0.#E0");
            formatter.setRoundingMode(RoundingMode.HALF_EVEN);
            formatter.setMaximumFractionDigits(dp_values[dp_idx]);
            text = formatter.format(b);
            if (text.endsWith("E0")) {
                text = text.substring(0, text.length()-2);
            }
        } else {
            //b.setScale(main.dp_values[main.dp_idx], RoundingMode.HALF_EVEN).stripTrailingZeros();
            NumberFormat formatter = new DecimalFormat("###,###.##");
            formatter.setMaximumFractionDigits(dp_values[dp_idx]);
            text = formatter.format(b);
        }
        return text;
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
        msgKind = MsgKind.PINNED_ERROR;
        msgView.setText(err);
        msgView.setTextColor(ContextCompat.getColorStateList(this, R.color.msgerror));
    }

    private void showPinnedInfo(final String info) {
        msgKind = MsgKind.PINNED_INFO;
        msgView.setText(info);
        msgView.setTextColor(ContextCompat.getColorStateList(this, R.color.msginfo));
    }

    private void showOnStackError(final String err) {
        msgKind = MsgKind.ON_STACK_ERROR;
        data.push(null);
        input = new StringBuffer(err);
        volatile_idx = data.size()-1;
        adapter.notifyItemInserted(volatile_idx);
    }

    private void addElementIfNonVolatile() {
        if (volatile_idx == -1) {
            pushHistory();
            data.push(new ViewModel(new BigDecimal("0")));
            adapter.notifyItemInserted(data.size()-1);
            volatile_idx = data.size()-1;
        }
    }

    private void addElementIfEmpty() {
        if (data.size() == 0) {
            pushHistory();
            data.push(new ViewModel(new BigDecimal("0")));
            adapter.notifyItemInserted(data.size()-1);
            volatile_idx = data.size()-1;
        }
    }

    public enum PushResult {
        nothing_to_push,
        pushed,
        invalid_item,
        blank_item,
    }

    private PushResult validatePush() {
        if (volatile_idx == -1) {
            return PushResult.nothing_to_push;
        } else if (input.length() == 0) {
            showPinnedError("Fix current blank item first");
            return PushResult.blank_item;
        } else {
            pushHistory();
            if (input.indexOf(".") == 0 || input.indexOf("-.") == 0) {
                input.insert(input.indexOf("-") != -1 ? 1 : 0, '0');
            }

            int dotExpIdx = input.indexOf(".E");
            if (dotExpIdx != -1) {
                input.deleteCharAt(dotExpIdx);
            }

            ViewModel m = data.get(volatile_idx);
            String repr = input.toString();
            m.latex = formatInputToLatex(repr);
            try {
                m.val = new BigDecimal(repr);
            } catch (NumberFormatException e) {
                showPinnedError("Fix current invalid item first");
                // Because we pushed history some lines ago
                popHistory();
                return PushResult.invalid_item;
            }
            adapter.notifyItemChanged(volatile_idx);
            input.setLength(0);
            volatile_idx = -1;
            return PushResult.pushed;
        }
    }
    
    private boolean pushOk(PushResult pushResult) {
        switch (pushResult) {
            case pushed:
            case nothing_to_push:
                return true;
            case blank_item:
            case invalid_item:
            default:
                return false;
        }
    }

    private String formatInputToLatex(String input) {
        input = input.toUpperCase();
        if (input.contains("E-")) {
            input = input.replace("E-", "\\text{E-}");
        } else if (input.contains("E")) {
            input = input.replace("E", "\\text{E}");
        }
        return input;
    }

    private void popAndClearInput() {
        if (validateNItemsOnStack(1)) {
            if (msgKind != MsgKind.ON_STACK_ERROR) pushHistory();
            data.pop();
            notifyAdapterItemRemoved(data.size() - 1);
            volatile_idx = -1;
            input.setLength(0);
        }
    }

    private void copyStringToClip(String s) {
        ClipData clipData = ClipData.newPlainText("", s);
        clipMgr.setPrimaryClip(clipData);
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

    public Op op_from_id(int id, State state) {
        if (id == R.id.k0 && state == State.shift) return Op.conv;
        else if (id == R.id.plus) return Op.add;
        else if (id == R.id.minus) return Op.sub;
        else if (id == R.id.mult) return Op.mul;
        else if (id == R.id.div) return Op.div;
        else if (id == R.id.chs) return Op.neg;
        else return Op.other;
    }

    public enum Op {
        conv,
        add,
        sub,
        mul,
        div,
        neg,
        other,
        none,
    }
}
