package de.tu.darmstadt.seemoo.ansian.control.events.tx.data.rds;

import de.tu.darmstadt.seemoo.ansian.control.events.tx.TransmitEvent;

/**
 * Created by MATZE on 25.02.2017.
 */

public class RDSTransmitEvent extends TransmitEvent {
    private String stationName;
    private boolean fileAudioSource;
    public RDSTransmitEvent(State state,Sender sender, String stationName, boolean fileAudioSource){
        super(state, sender);
        this.stationName = stationName;
        this.fileAudioSource = fileAudioSource;
    }

    public String getStationName() {
        return stationName;
    }

    public boolean getFileAudioSource() {
        return fileAudioSource;
    }
}
