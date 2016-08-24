package de.tu.darmstadt.seemoo.ansian.model.preferences;

import android.os.Environment;
import android.util.Log;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;

import de.tu.darmstadt.seemoo.ansian.MainActivity;
import de.tu.darmstadt.seemoo.ansian.R;

public class DemodPreference extends MySharedPreferences {

    private static final String LOGTAG = "DemodPreference";
    private int ditDuration;
    private boolean fixedDit;
    private int morseFrequency;
    private boolean clearTextAfter;
    private boolean automaticReinit;
    private boolean amDemod;
    private int initTime;
    private boolean fmRDS;
	private boolean usbPSK31;
    private int performanceSelector;
    private boolean logging;
    private String logfile_top_path;
    private String logfile_bot_path;

    public DemodPreference(MainActivity activity) {
        super(activity);
    }

    @Override
    public void loadPreference() {
        ditDuration = getInt("dit_duration", 300);
        morseFrequency = getInt("morse_frequency", 1000);
        fixedDit = getBoolean("dah_frequency", false);
        clearTextAfter = getBoolean("clear_text_after", false);
        automaticReinit = getBoolean("automatic_init", false);
        initTime = getInt("init_time", 5);
        amDemod = getBoolean("am_demod", true);
        fmRDS = getBoolean("fm_rds", true);
		usbPSK31 = getBoolean("usb_psk31", true);
        performanceSelector = Integer.parseInt(getString("performance_selector", "1"));
        logging = getBoolean("demod_log", false);

        logfile_top_path = getString("demod_top_log_path", "dummy");
        if(logfile_top_path.equals("dummy")) {
            File sdcard = Environment.getExternalStorageDirectory();
            logfile_top_path = sdcard.getAbsolutePath() + '/' + "demod_top.log";
            edit().putString("demod_top_log_path", logfile_top_path);
        }
        logfile_bot_path = getString("demod_bot_log_path", "dummy");
        if(logfile_bot_path.equals("dummy")) {
            File sdcard = Environment.getExternalStorageDirectory();
            logfile_bot_path = sdcard.getAbsolutePath() + '/' + "demod_bot.log";
            edit().putString("demod_bot_log_path", logfile_bot_path);
        }
    }

    @Override
    public void savePreference() {
        // create editor
        MyEditor editor = edit();
        editor.putInt("dit_duration", ditDuration);
        editor.putInt("init_time", initTime);
        editor.putInt("morse_frequency", morseFrequency);
        editor.putBoolean("fixed_dit", fixedDit);
        editor.putBoolean("clear_text_after", clearTextAfter);
        editor.putBoolean("fm_rds", fmRDS);
        editor.putString("performance_selector", "" + performanceSelector);
        editor.putBoolean("demod_log", logging);
        editor.putString("demod_top_log_path", logfile_top_path);
        editor.putString("demod_bot_log_path", logfile_bot_path);
        Log.d(LOGTAG, LOGTAG + " saved: " + editor.commit());
    }

    @Override
    public String getName() {
        return "demod";
    }

    @Override
    public int getResID() {
        return R.xml.demod_preferences;
    }

    public int getDitDuration() {
        return ditDuration;
    }

    public int getMorseFrequency() {
        return morseFrequency;
    }

    public boolean isClearAfter() {
        return clearTextAfter;
    }

    public void setWPM(int i) {
        ditDuration = 1200 / i;
    }

    public int getWPM() {
        return 1200 / ditDuration;
    }

    public int getInitTime() {
        return initTime * 1000;
    }

    public boolean isDitFixed() {
        return fixedDit;
    }

    public int getMode() {
        return getInt("receive_mode", 0);
    }

    public boolean isAmDemod() {
        return amDemod;
    }

    public boolean isAutomaticReinit() {
        return automaticReinit;
    }

    public boolean isFmRDS() {
        return fmRDS;
    }

    public boolean isUsbPSK31() {
        return usbPSK31;
    }

    public void setFmRDS(boolean fmRDS) {
        this.fmRDS = fmRDS;
    }

    public void setUsbPSK31(boolean usbPSK31) {
        this.usbPSK31 = usbPSK31;
    }

    public int getPerformanceSelector() {
        return performanceSelector;
    }

    public void setPerformanceSelector(int performanceSelector) {
        this.performanceSelector = performanceSelector;
    }

    public boolean isLogging() {
        return logging;
    }

    public String getTopLogfilePath() {
        return logfile_top_path;
    }

    public String getBotLogfilePath() {
        return logfile_bot_path;
    }
}
