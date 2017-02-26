package de.tu.darmstadt.seemoo.ansian.control.events.tx.speech.usb;

import de.tu.darmstadt.seemoo.ansian.control.events.tx.speech.SpeechTransmitEvent;

/**
 * Created by MATZE on 25.02.2017.
 */

public class USBTransmitEvent extends SpeechTransmitEvent {
    private int filterBandwidth;

    public USBTransmitEvent(State state, Sender sender,int transmissionSampleRate,
                            long transmissionFrequency, boolean amplifier, boolean antennaPowerPort,
                            int vgaGain, int filterBandwidth) {
        super(state, sender, transmissionSampleRate, transmissionFrequency, amplifier, antennaPowerPort, vgaGain);
        this.filterBandwidth = filterBandwidth;
    }

    public int getFilterBandwidth() {
        return filterBandwidth;
    }
}
