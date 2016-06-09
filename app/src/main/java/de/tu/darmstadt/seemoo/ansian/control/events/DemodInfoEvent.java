package de.tu.darmstadt.seemoo.ansian.control.events;


public class DemodInfoEvent {

    public static enum Mode {
        APPEND_STRING, WRITE_STRING, REPLACE_CHAR;
    }

    private String text;
    private int position;
    private Mode mode;

    // this class can be instantiated with the following factory methods

    public static DemodInfoEvent newAppendStringEvent(String s) {
        return new DemodInfoEvent(s, -1, Mode.APPEND_STRING);
    }

    public static DemodInfoEvent newReplaceStringEvent(String s) {
        return new DemodInfoEvent(s, -1, Mode.WRITE_STRING);
    }

    public static DemodInfoEvent newReplaceCharEvent(char c, int position) {
        return new DemodInfoEvent(Character.toString(c), position, Mode.REPLACE_CHAR);
    }

    private DemodInfoEvent() {}

    private DemodInfoEvent(String text, int position, Mode mode) {
        this.text = text;
        this.position = position;
        this.mode = mode;
    }

    public String getText() {
        return text;
    }

    public int getPosition() {
        return position;
    }

    public Mode getMode() {
        return mode;
    }

}

