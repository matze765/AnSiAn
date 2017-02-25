package de.tu.darmstadt.seemoo.ansian.control.events.tx;

public abstract class TransmitEvent {

    private State state;
    private Sender sender;

    public enum Sender {
        GUI, TX, TXCHAIN;
    }

    public enum State {
        TXOFF, TXACTIVE, MODULATION;
    }

    public TransmitEvent(State state, Sender sender) {
        this.state = state;
        this.sender = sender;
    }

    public State getState() {
        return state;
    }


    public Sender getSender() {
        return sender;
    }
}
