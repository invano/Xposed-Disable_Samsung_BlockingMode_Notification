package com.invano.disableblockingmodenotification;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.RadioGroup;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringWriter;


public class Settings extends Activity {

    private SharedPreferences prefs;
    private RadioGroup prefRadioGroup;
    private Button closeButton;
    private Button rebootButton;
    private Button softRebootButton;
    private static Context context;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings_layout);

        context = getApplicationContext();
        prefRadioGroup = (RadioGroup) findViewById(R.id.radioGroupPrefs1);
        closeButton = (Button) findViewById(R.id.closeButton);
        rebootButton = (Button) findViewById(R.id.rebootButton);
        softRebootButton = (Button) findViewById(R.id.softRebootButton);
        prefs = this.getSharedPreferences(getPackageName(), Context.MODE_WORLD_READABLE);

        if (!prefs.getBoolean(Common.PREF_BLOCK_KEY, false)) {
            prefRadioGroup.check(R.id.radioBlockTime);
        }
        else {
            prefRadioGroup.check(R.id.radioBlockAll);
        }

        prefRadioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                switch (checkedId) {
                    case R.id.radioBlockAll:
                        prefs.edit().putBoolean(Common.PREF_BLOCK_KEY, true).commit();
                        break;
                    case R.id.radioBlockTime:
                        prefs.edit().putBoolean(Common.PREF_BLOCK_KEY, false).commit();
                        break;
                    default:
                        break;
                }
            }
        });

        closeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        rebootButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                executeScript("reboot.sh");
            }
        });

        softRebootButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                executeScript("soft_reboot.sh");
            }
        });
    }

    public static String executeScript(String name) {
        File scriptFile = writeAssetToCacheFile(name);
        if (scriptFile == null)
            return "Could not find asset \"" + name + "\"";

        File busybox = writeAssetToCacheFile("busybox-xposed");
        if (busybox == null) {
            scriptFile.delete();
            return "Could not find asset \"busybox-xposed\"";
        }

        scriptFile.setReadable(true, false);
        scriptFile.setExecutable(true, false);

        busybox.setReadable(true, false);
        busybox.setExecutable(true, false);

        try {
            Process p = Runtime.getRuntime().exec(
                    new String[] {
                            "su",
                            "-c",
                            scriptFile.getAbsolutePath() + " "
                                    + android.os.Process.myUid() + " 2>&1" },
                    null, context.getCacheDir());
            BufferedReader stdout = new BufferedReader(new InputStreamReader(
                    p.getInputStream()));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = stdout.readLine()) != null) {
                sb.append(line);
                sb.append('\n');
            }
            stdout.close();
            return sb.toString();

        } catch (IOException e) {
            StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw));
            return sw.toString();
        } finally {
            scriptFile.delete();
            busybox.delete();
        }
    }

    public static File writeAssetToCacheFile(String name) {
        return writeAssetToCacheFile(name, name);
    }

    public static File writeAssetToCacheFile(String assetName, String fileName) {
        File file = null;
        try {
            InputStream in = context.getAssets().open(assetName);
            file = new File(context.getCacheDir(), fileName);
            FileOutputStream out = new FileOutputStream(file);

            byte[] buffer = new byte[1024];
            int len;
            while ((len = in.read(buffer)) > 0) {
                out.write(buffer, 0, len);
            }
            in.close();
            out.close();

            return file;
        } catch (IOException e) {
            e.printStackTrace();
            if (file != null)
                file.delete();

            return null;
        }
    }

}
