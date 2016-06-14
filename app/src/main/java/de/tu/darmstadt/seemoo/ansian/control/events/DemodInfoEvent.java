package de.tu.darmstadt.seemoo.ansian.control.events;


public class DemodInfoEvent {

    public static enum Mode {
        APPEND_STRING, WRITE_STRING, REPLACE_CHAR;
    }

    public static enum Position {
        TOP, BOTTOM;
    }

    private String text;
    private int characterPosition;
    private Mode mode;
    private boolean marquee;
    private Position textPosition;

    // this class can be instantiated with the following factory methods

    public static DemodInfoEvent newAppendStringEvent(Position textPosition, String s) {
        return new DemodInfoEvent(textPosition, s, -1, Mode.APPEND_STRING, false);
    }

    public static DemodInfoEvent newReplaceStringEvent(Position textPosition, String s, boolean marquee) {
        return new DemodInfoEvent(textPosition, s, -1, Mode.WRITE_STRING, marquee);
    }

    public static DemodInfoEvent newReplaceCharEvent(Position textPosition, char c, int position) {
        return new DemodInfoEvent(textPosition, Character.toString(c), position, Mode.REPLACE_CHAR, true);
    }

    private DemodInfoEvent() {}

    private DemodInfoEvent(Position position, String text, int characterPosition, Mode mode, boolean marquee) {
        this.textPosition = position;
        this.text = text;
        this.characterPosition = characterPosition;
        this.mode = mode;
        this.marquee = marquee;
    }

    public String getText() {
        return text;
    }

    public int getCharacterPosition() {
        return characterPosition;
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

