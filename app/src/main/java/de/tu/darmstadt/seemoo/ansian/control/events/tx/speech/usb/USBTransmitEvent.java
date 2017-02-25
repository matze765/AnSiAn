package de.tu.darmstadt.seemoo.ansian.control.events.tx.speech.usb;

import de.tu.darmstadt.seemoo.ansian.control.events.tx.speech.SpeechTransmitEvent;

/**
 * Created by MATZE on 25.02.2017.
 */

public class USBTransmitEvent extends SpeechTransmitEvent {
    private int filterBandwidth;

    public USBTransmitEvent(State state, Sender sender, int filterBandwidth) {
        super(state, sender);
        this.filterBandwidth = filterBandwidth;
    }

    public int getFilterBandwidth() {
        return filterBandwidth;
    }
}
