package de.tu.darmstadt.seemoo.ansian.control.events.tx.speech.fm;

import de.tu.darmstadt.seemoo.ansian.control.events.tx.speech.SpeechTransmitEvent;

/**
 * Created by MATZE on 25.02.2017.
 */

public class FMTransmitEvent extends SpeechTransmitEvent {

    public FMTransmitEvent(State state, Sender sender) {
        super(state, sender);
    }
}
