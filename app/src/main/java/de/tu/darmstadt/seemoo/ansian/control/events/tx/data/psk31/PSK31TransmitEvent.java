package de.tu.darmstadt.seemoo.ansian.control.events.tx.data.psk31;

import de.tu.darmstadt.seemoo.ansian.control.events.tx.TransmitEvent;

/**
 * Created by MATZE on 25.02.2017.
 */

public class PSK31TransmitEvent extends TransmitEvent {
    private String payload;
    public PSK31TransmitEvent(State state, Sender sender, String payload){
        super(state, sender);
        this.payload = payload;
    }

    public String getPayload() {
        return payload;
    }
}
