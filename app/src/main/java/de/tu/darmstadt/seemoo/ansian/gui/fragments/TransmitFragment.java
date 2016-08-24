package de.tu.darmstadt.seemoo.ansian.gui.fragments;

import android.content.res.Configuration;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import de.tu.darmstadt.seemoo.ansian.MainActivity;
import de.tu.darmstadt.seemoo.ansian.R;
import de.tu.darmstadt.seemoo.ansian.gui.tabs.MyTabFragment;
import de.tu.darmstadt.seemoo.ansian.gui.views.TransmitView;


/**
 * Fragment for the Transmit view
 */

public class TransmitFragment extends MyTabFragment {

    private TransmitView transmitView;

    public TransmitFragment(MainActivity activity) {
        super("Transmit", activity);
    }

    // Inflate the view for the fragment based on layout XML
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.transmit_fragment, container, false);
        transmitView = (TransmitView) v.findViewById(R.id.transmitView);
        return v;
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        LayoutInflater inflater = LayoutInflater.from(getActivity());
        populateViewForOrientation(inflater, (ViewGroup) getView());
        init();
        update();
    }

    private void populateViewForOrientation(LayoutInflater inflater, ViewGroup viewGroup) {
        viewGroup.removeAllViewsInLayout();
        inflater.inflate(R.layout.transmit_fragment, viewGroup);
    }

    private void update() {
        transmitView.update();
    }

    @Override
    public void onResume() {
        super.onResume();
        init();
        update();
    }

    private void init() {
        getView().setBackgroundColor(Color.BLACK);
    }

}
