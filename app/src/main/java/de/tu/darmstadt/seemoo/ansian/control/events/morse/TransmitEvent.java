package de.tu.darmstadt.seemoo.ansian.control.events.morse;

public class TransmitEvent {

    private boolean transmitting;
    private Sender sender;

    public enum Sender {
        GUI, TX;
    }

    public TransmitEvent(boolean b, Sender s) {
        transmitting = b;
        sender = s;
    }

    public boolean isTransmitting() {
        return transmitting;
    }

    public Sender getSender() {
        return sender;
    }
}
