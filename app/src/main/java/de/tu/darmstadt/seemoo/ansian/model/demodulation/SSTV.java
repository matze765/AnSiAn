package de.tu.darmstadt.seemoo.ansian.model.demodulation;

import android.util.Log;

import de.tu.darmstadt.seemoo.ansian.model.SamplePacket;

/**
 * Created by MATZE on 26.02.2017.
 */

public class SSTV extends Demodulation {
    private static final String LOGTAG = "SSTV";

    @Override
    public void demodulate(SamplePacket input, SamplePacket output) {
        Log.d(LOGTAG, "sample_rate="+input.getSampleRate());
        Log.d(LOGTAG, "packet_size="+input.size());
        input.copyTo(output);
    }

    @Override
    public DemoType getType() {
        return DemoType.SSTV;
    }
}
