package de.tu.darmstadt.seemoo.ansian.control.events.tx.speech.usb;

import de.tu.darmstadt.seemoo.ansian.control.events.tx.speech.SpeechTransmitEvent;

/**
 * Created by MATZE on 25.02.2017.
 */

public class USBTransmitEvent extends SpeechTransmitEvent {
    public USBTransmitEvent(State state, Sender sender) {
        super(state, sender);
    }
}
