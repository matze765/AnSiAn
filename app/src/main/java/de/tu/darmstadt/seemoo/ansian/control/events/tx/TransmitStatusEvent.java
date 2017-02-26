package de.tu.darmstadt.seemoo.ansian.control.events.tx;

/**
 * Created by MATZE on 25.02.2017.
 */

public class TransmitStatusEvent extends TransmitEvent {
    public TransmitStatusEvent(State state, Sender sender){
        super(state, sender,0,0,false, false,0);
    }
}
