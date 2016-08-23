package de.tu.darmstadt.seemoo.ansian.gui.fragments.settings;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.SwitchPreference;

import de.tu.darmstadt.seemoo.ansian.model.preferences.Preferences;

/**
 * Fragment for Morse preferences
 */
public class DemodFragment extends MyPreferenceFragment {

    private static final String LOGTAG = "DemodFragment";

    public DemodFragment() {
        super(Preferences.DEMOD_PREFERENCE);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        updateReinitEnabledState();
        updateLogfileEnabledState();
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        super.onSharedPreferenceChanged(sharedPreferences, key);
        if (key.equals("receive_mode")) {
            updateReinitEnabledState();
        } else if (key.equals("demod_log")) {
            updateLogfileEnabledState();
        }
    }

    /**
     * disable logfile selection if logging is disabled
     */
    private void updateLogfileEnabledState() {
        SwitchPreference switchPref = (SwitchPreference) findPreference("demod_log");
        EditTextPreference topPref = (EditTextPreference) findPreference("demod_top_log_path");
        EditTextPreference botPref = (EditTextPreference) findPreference("demod_bot_log_path");
        topPref.setEnabled(switchPref.isChecked());
        botPref.setEnabled(switchPref.isChecked());
    }

    /**
     * disable automatic reinit if manual mode is selected
     */
    private void updateReinitEnabledState() {
        ListPreference listPref = (ListPreference) findPreference("receive_mode");
        SwitchPreference switchPref = (SwitchPreference) findPreference("automatic_init");
        if (listPref.getValue().equals("1"))
            switchPref.setEnabled(false);
        else
            switchPref.setEnabled(true);
    }
}
