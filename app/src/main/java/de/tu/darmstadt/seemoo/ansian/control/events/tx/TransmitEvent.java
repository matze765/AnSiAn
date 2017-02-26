package de.tu.darmstadt.seemoo.ansian.control.events.tx;

public abstract class TransmitEvent {

    private State state;
    private Sender sender;
    private int transmissionSampleRate;
    private long transmissionFrequency;
    private boolean amplifier;
    private boolean antennaPowerPort;
    private int vgaGain;




    public enum Sender {
        GUI, TX, TXCHAIN;
    }

    public enum State {
        TXOFF, TXACTIVE, MODULATION;
    }

    public TransmitEvent(State state, Sender sender, int transmissionSampleRate,
                         long transmissionFrequency, boolean amplifier, boolean antennaPowerPort,
                         int vgaGain) {
        this.state = state;
        this.sender = sender;
        this.transmissionSampleRate = transmissionSampleRate;
        this.transmissionFrequency = transmissionFrequency;
        this.amplifier = amplifier;
        this.antennaPowerPort = antennaPowerPort;
        this.vgaGain = vgaGain;
    }

    public State getState() {
        return state;
    }


    public Sender getSender() {
        return sender;
    }

    public int getTransmissionSampleRate() {
        return transmissionSampleRate;
    }
    public long getTransmissionFrequency() {
        return transmissionFrequency;
    }

    public boolean isAmplifier() {
        return amplifier;
    }

    public boolean isAntennaPowerPort() {
        return antennaPowerPort;
    }
    public int getVgaGain() {
        return vgaGain;
    }
}
