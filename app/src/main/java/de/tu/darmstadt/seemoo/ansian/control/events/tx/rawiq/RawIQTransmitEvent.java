package de.tu.darmstadt.seemoo.ansian.control.events.tx.rawiq;

import de.tu.darmstadt.seemoo.ansian.control.events.tx.TransmitEvent;

/**
 * Created by MATZE on 25.02.2017.
 */

public class RawIQTransmitEvent extends TransmitEvent {
    private String transmitFileName;

    public RawIQTransmitEvent(State state, Sender sender, int transmissionSampleRate,
                              long transmissionFrequency, boolean amplifier, boolean antennaPowerPort,
                              int vgaGain, String transmitFileName){
        super(state, sender,transmissionSampleRate, transmissionFrequency, amplifier, antennaPowerPort, vgaGain);
        this.transmitFileName = transmitFileName;
    }

    public String getTransmitFileName() {
        return transmitFileName;
    }
}
