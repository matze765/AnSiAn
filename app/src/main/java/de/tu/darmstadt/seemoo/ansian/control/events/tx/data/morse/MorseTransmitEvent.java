package de.tu.darmstadt.seemoo.ansian.control.events.tx.data.morse;

import de.tu.darmstadt.seemoo.ansian.control.events.tx.TransmitEvent;
import de.tu.darmstadt.seemoo.ansian.control.events.tx.data.DataTransmitEvent;

/**
 * Created by MATZE on 25.02.2017.
 */

public class MorseTransmitEvent extends DataTransmitEvent {
    private int wpm;
    private int morseFrequency;

    public MorseTransmitEvent(State state, Sender sender,int transmissionSampleRate,
                              long transmissionFrequency, boolean amplifier, boolean antennaPowerPort,
                              int vgaGain,
                              String payload, int wpm, int morseFrequency){
        super(state, sender,transmissionSampleRate, transmissionFrequency, amplifier, antennaPowerPort, vgaGain, payload);
        this.wpm = wpm;
        this.morseFrequency = morseFrequency;
    }


    public int getWPM() {
        return wpm;
    }

    public int getMorseFrequency() {
        return morseFrequency;
    }
}
