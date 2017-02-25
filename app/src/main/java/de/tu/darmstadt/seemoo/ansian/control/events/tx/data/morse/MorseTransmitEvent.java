package de.tu.darmstadt.seemoo.ansian.control.events.tx.data.morse;

import de.tu.darmstadt.seemoo.ansian.control.events.tx.TransmitEvent;
import de.tu.darmstadt.seemoo.ansian.control.events.tx.data.DataTransmitEvent;

/**
 * Created by MATZE on 25.02.2017.
 */

public class MorseTransmitEvent extends DataTransmitEvent {
    private int wpm;

    public MorseTransmitEvent(State state, Sender sender,String payload, int wpm){
        super(state, sender, payload);
        this.wpm = wpm;
    }


    public int getWPM() {
        return wpm;
    }
}
