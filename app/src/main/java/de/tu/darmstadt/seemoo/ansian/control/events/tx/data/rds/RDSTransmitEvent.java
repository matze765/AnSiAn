package de.tu.darmstadt.seemoo.ansian.control.events.tx.data.rds;

import de.tu.darmstadt.seemoo.ansian.control.events.tx.TransmitEvent;
import de.tu.darmstadt.seemoo.ansian.control.events.tx.data.DataTransmitEvent;

/**
 * Created by MATZE on 25.02.2017.
 */

public class RDSTransmitEvent extends DataTransmitEvent {
    private boolean fileAudioSource;
    public RDSTransmitEvent(State state,Sender sender, String stationName, boolean fileAudioSource){
        super(state, sender, stationName);
        this.fileAudioSource = fileAudioSource;
    }

    public boolean getFileAudioSource() {
        return fileAudioSource;
    }
}
