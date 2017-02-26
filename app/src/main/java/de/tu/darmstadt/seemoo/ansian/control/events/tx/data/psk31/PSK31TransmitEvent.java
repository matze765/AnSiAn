package de.tu.darmstadt.seemoo.ansian.control.events.tx.data.psk31;

import de.tu.darmstadt.seemoo.ansian.control.events.tx.TransmitEvent;
import de.tu.darmstadt.seemoo.ansian.control.events.tx.data.DataTransmitEvent;

/**
 * Created by MATZE on 25.02.2017.
 */

public class PSK31TransmitEvent extends DataTransmitEvent {
    public PSK31TransmitEvent(State state, Sender sender,int transmissionSampleRate,
                              long transmissionFrequency, boolean amplifier, boolean antennaPowerPort,
                              int vgaGain, String payload){
        super(state, sender,transmissionSampleRate, transmissionFrequency, amplifier, antennaPowerPort, vgaGain, payload);
    }
}
