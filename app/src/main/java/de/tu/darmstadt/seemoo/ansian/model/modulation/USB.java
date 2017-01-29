package de.tu.darmstadt.seemoo.ansian.model.modulation;

/**
 * Created by MATZE on 20.01.2017.
 */

public class USB extends SSB {
    public USB(int sampleRate, int filterBandWidth) {
        super(sampleRate, filterBandWidth, true);
    }
}
