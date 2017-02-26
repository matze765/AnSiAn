package de.tu.darmstadt.seemoo.ansian.control.events.tx.speech.lsb;

import de.tu.darmstadt.seemoo.ansian.control.events.tx.speech.SpeechTransmitEvent;

/**
 * Created by MATZE on 25.02.2017.
 */

public class LSBTransmitEvent extends SpeechTransmitEvent {
    private final int filterBandwidth;

    public LSBTransmitEvent(State state, Sender sender,int transmissionSampleRate,
                            long transmissionFrequency, boolean amplifier, boolean antennaPowerPort,
                            int vgaGain, int filterBandwidth) {
        super(state, sender, transmissionSampleRate, transmissionFrequency, amplifier, antennaPowerPort, vgaGain);
        this.filterBandwidth = filterBandwidth;
    }

    public int getFilterBandwidth() {
        return filterBandwidth;
    }
}
