package de.tu.darmstadt.seemoo.ansian.control.events.tx.data;

import de.tu.darmstadt.seemoo.ansian.control.events.tx.TransmitEvent;

/**
 * Created by MATZE on 25.02.2017.
 */

public class DataTransmitEvent extends TransmitEvent {
    private final String payload;

    public DataTransmitEvent(State state, Sender sender,  int transmissionSampleRate,
                             long transmissionFrequency, boolean amplifier, boolean antennaPowerPort,
                             int vgaGain, String payload){
        super(state, sender,transmissionSampleRate, transmissionFrequency, amplifier, antennaPowerPort, vgaGain);
        this.payload = payload;
    }

    public String getPayload() {
        return payload;
    }
}
