package de.tu.darmstadt.seemoo.ansian.control.events.morse;

public class TransmitEvent {

    private State state;
    private Sender sender;
    private String iqFile;

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

    public String getIqFile() {
        return iqFile;
    }

    public void setIqFile(String iqFile) {
        this.iqFile = iqFile;
    }

    public Sender getSender() {
        return sender;
    }
}
