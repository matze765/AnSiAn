package de.tu.darmstadt.seemoo.ansian.gui.fragments;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import de.tu.darmstadt.seemoo.ansian.MainActivity;
import de.tu.darmstadt.seemoo.ansian.R;
import de.tu.darmstadt.seemoo.ansian.gui.tabs.MyTabFragment;
import de.tu.darmstadt.seemoo.ansian.gui.views.SSTVView;
import de.tu.darmstadt.seemoo.ansian.gui.views.WalkieTalkieView;

/**
 * Created by MATZE on 22.02.2017.
 */

public class SSTVFragement  extends MyTabFragment{
    private SSTVView sstvView;

    public SSTVFragement(MainActivity activity) {
        super("SSTV", activity);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.sstv_fragement, container, false);
        sstvView = (SSTVView) v.findViewById(R.id.sstvView);

        return v;

    }
}
