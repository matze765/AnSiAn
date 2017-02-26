package de.tu.darmstadt.seemoo.ansian.control.events.tx.data.rds;

import de.tu.darmstadt.seemoo.ansian.control.events.tx.TransmitEvent;
import de.tu.darmstadt.seemoo.ansian.control.events.tx.data.DataTransmitEvent;

/**
 * Created by MATZE on 25.02.2017.
 */

public class RDSTransmitEvent extends DataTransmitEvent {
    private boolean fileAudioSource;
    public RDSTransmitEvent(State state,Sender sender,int transmissionSampleRate,
                            long transmissionFrequency, boolean amplifier, boolean antennaPowerPort,
                            int vgaGain, String stationName, boolean fileAudioSource){
        super(state, sender,transmissionSampleRate, transmissionFrequency, amplifier, antennaPowerPort, vgaGain, stationName);
        this.fileAudioSource = fileAudioSource;
    }

    public boolean getFileAudioSource() {
        return fileAudioSource;
    }
}
