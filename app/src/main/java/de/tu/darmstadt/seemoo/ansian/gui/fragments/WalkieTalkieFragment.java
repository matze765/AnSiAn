package de.tu.darmstadt.seemoo.ansian.gui.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import de.tu.darmstadt.seemoo.ansian.MainActivity;
import de.tu.darmstadt.seemoo.ansian.R;
import de.tu.darmstadt.seemoo.ansian.gui.tabs.MyTabFragment;
import de.tu.darmstadt.seemoo.ansian.gui.views.TransmitView;
import de.tu.darmstadt.seemoo.ansian.gui.views.WalkieTalkieView;

/**
 * Tab that implements the WalkieTalkie front end. The user is able to select an amateur radio frequency
 * band, a frequency and  a modulation scheme (currently FM or SSB). Then the user can start the reception.
 * By pressing a button the user can switch to transmission mode to transmit own signals, AnSiAn
 * switches back to reception mode afterwards.
 *
 * @author Matthias Kannwischer
 */

public class WalkieTalkieFragment extends MyTabFragment {

    WalkieTalkieView walkieTalkieView;
    public WalkieTalkieFragment(MainActivity activity) {
        super("Walkie-Talkie", activity);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.walkietalkie_fragment, container, false);
        walkieTalkieView = (WalkieTalkieView) v.findViewById(R.id.walkieTalkieView);

        return v;
    }
}
