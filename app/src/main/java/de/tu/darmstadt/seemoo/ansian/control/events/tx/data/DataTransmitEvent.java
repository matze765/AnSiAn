package de.tu.darmstadt.seemoo.ansian.control.events.tx.data;

import de.tu.darmstadt.seemoo.ansian.control.events.tx.TransmitEvent;

/**
 * Created by MATZE on 25.02.2017.
 */

public class DataTransmitEvent extends TransmitEvent {
    private final String payload;

    public DataTransmitEvent(State state, Sender sender, String payload){
        super(state, sender);
        this.payload = payload;
    }

    public String getPayload() {
        return payload;
    }
}
