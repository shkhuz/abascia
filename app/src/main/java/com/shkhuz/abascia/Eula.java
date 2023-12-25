package com.shkhuz.abascia;

import android.content.DialogInterface;
import android.util.Log;

import androidx.appcompat.app.AlertDialog;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Objects;

public class Eula {
    private Main main;

    private static final String SP_EULA_ACCEPTED = "eula.accepted";

    public Eula(Main main) {
        this.main = main;
    }

    public void showIfNotAccepted() {
        if (!main.sp.getBoolean(SP_EULA_ACCEPTED, false)) {
            AlertDialog.Builder b = new AlertDialog.Builder(main);
            b.setTitle("EULA");
            b.setCancelable(true);
            b.setPositiveButton("Accept", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    // We commit instead of apply(). For such an important
                    // function, why not?
                    main.spedit.putBoolean(SP_EULA_ACCEPTED, true).commit();
                }
            });
            b.setNegativeButton("Refuse", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    refuse();
                }
            });
            b.setOnCancelListener(new DialogInterface.OnCancelListener() {
                @Override
                public void onCancel(DialogInterface dialog) {
                    refuse();
                }
            });
            b.setMessage(readAsset("EULA.txt"));
            b.show();
        }
    }

    private void refuse() {
        main.finish();
    }

    private CharSequence readAsset(final String path) {
        CharSequence result = "";
        BufferedReader txt = null;
        try {
            txt = new BufferedReader(new InputStreamReader(main.getAssets().open(path)));
            String line;
            final StringBuilder buffer = new StringBuilder();
            while ((line = txt.readLine()) != null) {
                buffer.append(line).append('\n');
            }
            result = buffer;
        } catch (IOException e) {
            Log.e("openReadStream", e.getMessage());
        } finally {
            if (txt != null) {
                try {
                    txt.close();
                } catch (IOException e) {
                    Log.e("closeStream", e.getMessage());
                }
            }
        }
        return result;
    }
}
