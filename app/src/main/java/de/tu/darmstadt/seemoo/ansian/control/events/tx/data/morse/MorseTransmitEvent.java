package de.tu.darmstadt.seemoo.ansian.control.events.tx.data.morse;

import de.tu.darmstadt.seemoo.ansian.control.events.tx.TransmitEvent;

/**
 * Created by MATZE on 25.02.2017.
 */

public class MorseTransmitEvent extends TransmitEvent {

    private String payload;
    private int wpm;

    public MorseTransmitEvent(State state, Sender sender, String payload, int wpm){
        super(state, sender);
        this.payload = payload;
        this.wpm = wpm;
    }

    public String getPayload() {
        return payload;
    }

    public int getWPM() {
        return wpm;
    }
}
