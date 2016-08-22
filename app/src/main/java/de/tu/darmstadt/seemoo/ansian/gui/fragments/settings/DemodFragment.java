package de.tu.darmstadt.seemoo.ansian.gui.fragments.settings;

import android.content.SharedPreferences;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.SwitchPreference;
import de.tu.darmstadt.seemoo.ansian.model.preferences.Preferences;

/**
 * Fragment for Morse preferences
 *
 */
public class DemodFragment extends MyPreferenceFragment {

	private static final String LOGTAG = "DemodFragment";
	
	public DemodFragment() {
		super(Preferences.DEMOD_PREFERENCE);
		}

	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
		super.onSharedPreferenceChanged(sharedPreferences, key);	
		Preference pref = findPreference(key);

		// disable automatic reinit if manual mode is selected
		if (key.equals("receive_mode")) {
			ListPreference listPref = (ListPreference) pref;
			SwitchPreference switchPref= (SwitchPreference) findPreference("automatic_init");
			if(listPref.getValue().equals("1"))
				switchPref.setEnabled(false);
			else
				switchPref.setEnabled(true);
		}
	}
}
