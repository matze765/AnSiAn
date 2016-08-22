package de.tu.darmstadt.seemoo.ansian.control.events;


public class DemodInfoEvent {

    public static enum Mode {
        APPEND_STRING, WRITE_STRING;
    }

    public static enum Position {
        TOP, BOTTOM;
    }

    private String text;
    private Mode mode;
    private boolean marquee;
    private Position textPosition;

    // this class can be instantiated with the following factory methods

    public static DemodInfoEvent newAppendStringEvent(Position textPosition, String s) {
        return new DemodInfoEvent(textPosition, s, Mode.APPEND_STRING, false);
    }

    public static DemodInfoEvent newReplaceStringEvent(Position textPosition, String s, boolean marquee) {
        return new DemodInfoEvent(textPosition, s, Mode.WRITE_STRING, marquee);
    }

    private DemodInfoEvent() {
    }

    private DemodInfoEvent(Position position, String text, Mode mode, boolean marquee) {
        this.textPosition = position;
        this.text = text;
        this.mode = mode;
        this.marquee = marquee;
    }

    public String getText() {
        return text;
    }

    public Mode getMode() {
        return mode;
    }

    public boolean isMarquee() {
        return marquee;
    }

    public Position getTextPosition() {
        return textPosition;
    }
}

