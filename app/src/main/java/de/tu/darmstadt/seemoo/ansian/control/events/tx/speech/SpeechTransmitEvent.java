package de.tu.darmstadt.seemoo.ansian.control.events.tx.speech;

import de.tu.darmstadt.seemoo.ansian.control.events.tx.TransmitEvent;

/**
 * Created by MATZE on 25.02.2017.
 */

public class SpeechTransmitEvent extends TransmitEvent {
    public SpeechTransmitEvent(State state, Sender sender,int transmissionSampleRate,
                               long transmissionFrequency, boolean amplifier, boolean antennaPowerPort,
                               int vgaGain){
        super(state, sender, transmissionSampleRate, transmissionFrequency, amplifier, antennaPowerPort, vgaGain);
    }
}
