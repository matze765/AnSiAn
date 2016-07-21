package de.tu.darmstadt.seemoo.ansian.model.demodulation.morse;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import de.greenrobot.event.EventBus;
import de.tu.darmstadt.seemoo.ansian.control.events.DemodInfoEvent;
import de.tu.darmstadt.seemoo.ansian.control.events.morse.MorseCodeEvent;
import de.tu.darmstadt.seemoo.ansian.control.events.morse.MorseDitEvent;
import de.tu.darmstadt.seemoo.ansian.control.events.morse.MorseSymbolEvent;
import de.tu.darmstadt.seemoo.ansian.model.SamplePacket;
import de.tu.darmstadt.seemoo.ansian.model.demodulation.AM;
import de.tu.darmstadt.seemoo.ansian.model.demodulation.Demodulation;
import de.tu.darmstadt.seemoo.ansian.model.preferences.MorsePreference;
import de.tu.darmstadt.seemoo.ansian.model.preferences.Preferences;
import de.tu.darmstadt.seemoo.ansian.tools.morse.Decoder;
import de.tu.darmstadt.seemoo.ansian.tools.morse.MorseCodeCharacterGetter;

public class Morse extends Demodulation {

    // TODO: idea for memory optimization - reuse buffer for current envelope and binary envelope!


    public enum State {
        COLLECT_SAMPLES, INIT_STATS, DEMOD, STOPPED
    }

    private AM amDemodulator;
    private MorsePreference prefs;
    private State state;

    private int initTime;
    private long firstInitTimestamp;
    private List<EnvelopePacket> initPackets;
    private boolean[] binaryInitData;

    private float peak;
    private float bottom;
    private float threshold;

    // number of samples each
    private int dit;
    private int dah;
    private int word;
    private int margin;

    private int lastStreakLength;
    private boolean lastStreakValue;

    private int sampleRate;

    private StringBuilder currentSymbolCode;
    private Decoder decoder;


    public Morse() {
        this.amDemodulator = new AM();
        this.prefs = Preferences.MORSE_PREFERENCE;
        this.state = State.COLLECT_SAMPLES;
        this.initTime = prefs.getInitTime();
        this.initPackets = new ArrayList<EnvelopePacket>(); // sensible init-value for better performance?
        this.binaryInitData = null;

        this.firstInitTimestamp = -1;
        this.peak = Float.MIN_VALUE;
        this.bottom = Float.MAX_VALUE;
        this.threshold = Float.NaN;

        this.dit = -1;
        this.dah = -1;
        this.word = -1;
        this.margin = -1;

        this.sampleRate = -1;

        this.lastStreakLength = 0;
        this.lastStreakValue = false;

        this.currentSymbolCode = new StringBuilder();
        this.decoder = new Decoder();
    }

    @Override
    public DemoType getType() {
        return DemoType.MORSE;
    }

    private void setTimings(int dit) {
        this.dit = dit;
        this.dah = 3 * dit;
        this.word = 7 * dit;
        this.margin = (int) Math.round(dit * 0.5);

    }

    @Override
    public void demodulate(SamplePacket input, SamplePacket output) {
        if (prefs.isAmDemod()) {
            amDemodulator.demodulate(input, output);
        }

        // TODO: don't do this if you don't need the envelope
        this.sampleRate = input.getSampleRate();
        float[] envelope = getEnvelope(input.getRe(), input.getIm(), input.size());

        switch (this.state) {
            case COLLECT_SAMPLES:
                long packetTimestamp = input.getTimestamp();
                initPackets.add(new EnvelopePacket(envelope, packetTimestamp));
                if (firstInitTimestamp == -1) {
                    firstInitTimestamp = packetTimestamp;
                } else {
                    long time = (packetTimestamp - firstInitTimestamp);
                    if (time > initTime) {
                        initializeStats();
                    }
                }
                break;
            case INIT_STATS:
                // discard packets while demodulator thread is busy initializing
                break;
            case DEMOD:
                updateThreshold(envelope);
                boolean[] binaryEnvelope = binarize(envelope);
                demodulate(binaryEnvelope);
                break;
            case STOPPED:
                // discard packets
                break;
        }


    }

    private void demodulate(boolean[] envelope) {
        if (envelope.length < 1)
            return;

        int currentIndex = 0;
        int currentStreak = 0;

        // handle continued streak from last packet
        if (envelope[0] == this.lastStreakValue) {
            currentStreak = getStreakLength(envelope, 0);
            if (currentStreak == -1) { // array terminated before streak was interrupted
                this.lastStreakLength += envelope.length;
                return; // array ended, nothing more to demodulate
            } else {
                this.lastStreakLength += currentStreak;
                currentIndex = currentStreak; // start iterating the rest of the array from this point
            }
        }
        decode(this.lastStreakValue, this.lastStreakLength);

        // iterate over the array and new find streaks
        while (currentIndex < envelope.length) {
            currentStreak = getStreakLength(envelope, currentIndex);

            if (currentStreak == -1) { // array terminated before streak was interrupted
                break; // array ended, nothing more to demodulate
            }

            decode(envelope[currentIndex], currentStreak);
            currentIndex += currentStreak;
        }

        // set lastStreakValue and lastStreakLength for next packet
        this.lastStreakLength = envelope.length - currentIndex;
        this.lastStreakValue = envelope[envelope.length - 1];
    }

    public boolean decode(boolean high, int streak) {

        if (streak < dit - margin)
            return false; // this cannot be a

        String code = null;

        if (high) { // streak of high samples
            if (dit - margin < streak && streak < dit + margin)
                code = "."; // dit
            else if (dah - dit < streak && streak < dah + dit)
                code = "-"; // dah
        } else { // streak of low samples
            if (dit - margin < streak && streak < dit + margin)
                code = ""; // dit
            else if (dah - dit < streak && streak < dah + dit)
                code = " "; // dah
            else if (word - 2 * dit < streak)
                code = "/"; // word
            else if (word + margin < streak)
                code = ""; // pause/no signal
        }

        if (code == null) { // streak was not recognized
            EventBus.getDefault().postSticky(new MorseCodeEvent(1.0f, this.threshold)); // TODO: why is this important?
            return false;
        }


        EventBus.getDefault().postSticky(DemodInfoEvent.newAppendStringEvent(DemodInfoEvent.Position.TOP, code));
        EventBus.getDefault().postSticky(new MorseCodeEvent(1.0f, this.threshold)); // TODO: why is this important?

        // check if a new symbol was completed
        if (code == " ")
            createSymbol();
        else
            currentSymbolCode.append(code);

        return true;
    }

    private void createSymbol() {
        String currentSymbolCodeString = currentSymbolCode.toString();
        String symbol = decoder.decode(currentSymbolCodeString);
        boolean recognized = !(symbol.contains(MorseCodeCharacterGetter.ESCAPE_START)
                || symbol.contains(MorseCodeCharacterGetter.ESCAPE_END));
        EventBus.getDefault().postSticky(new MorseSymbolEvent(1.0f)); // TODO: why is this important?
        if (recognized)
            EventBus.getDefault().postSticky(DemodInfoEvent.newAppendStringEvent(DemodInfoEvent.Position.BOTTOM, symbol));
        currentSymbolCode = new StringBuilder();
    }

    /**
     * Returns the length of consecutive values from a defined startIndex in a boolean array.
     * Returns -1 if the streak does not terminate within the array of if startIndex is not in the array
     */
    private int getStreakLength(boolean[] array, int startIndex) {
        int result = 1;
        boolean value = array[startIndex];

        for (int i = startIndex + 1; i < array.length; i++) {
            if (array[i] == value) {
                result++; // streak goes on
            } else {
                return result; // streak terminated
            }
        }

        return -1; // array ended without streak termination
    }

    private void initializeThreshold() {
        for (int i = 0; i < initPackets.size(); i++) {
            float[] envelope = initPackets.get(i).getEnvelope();
            updateThreshold(envelope);
        }
    }

    private void updateThreshold(float[] envelope) {
        for (int i = 0; i < envelope.length; i++) {
            if (envelope[i] > this.peak) {
                this.peak = envelope[i];
            }
            if (envelope[i] < this.bottom) {
                this.bottom = envelope[i];
            }
        }
        this.threshold = this.bottom + ((this.peak - this.bottom) * 2f);
    }


    /**
     * Initializes threshold and timings
     */
    private void initializeStats() {
        this.state = State.INIT_STATS;
        initializeThreshold();
        binarizeInitData();
        initPackets = null; // release memory
        initializeTimings();
    }

    /**
     * Initializes this.binaryInitData with an empty boolean array of appropriate size
     */
    private void initializeBinaryInitArray() {
        int totalSize = 0;
        for (int i = 0; i < initPackets.size(); i++) {
            totalSize += initPackets.get(i).getEnvelope().length;
        }
        this.binaryInitData = new boolean[totalSize];
    }


    /**
     * Turns collected init samples to high/low information and writes them into this.binaryInitData
     */
    private void binarizeInitData() {
        int ctr = 0;
        initializeBinaryInitArray();
        for (int i = 0; i < initPackets.size(); i++) {
            float[] envelope = initPackets.get(i).getEnvelope();
            for (int j = 0; j < envelope.length; j++) {
                binaryInitData[ctr++] = envelope[j] >= threshold;
            }
        }
    }

    private boolean[] binarize(float[] envelope) {
        boolean[] result = new boolean[envelope.length];
        for (int i = 0; i < result.length; i++) {
            result[i] = envelope[i] < threshold;
        }
        return result;
    }

    private float[] getEnvelope(float[] re, float[] im, int size) {
        // if this is too inefficient, use envelope from am demodulator instead
        float[] env = new float[size];

        for (int i = 0; i < size; i++) {
            env[i] = (float) Math.sqrt(re[i] * re[i] + im[i] * im[i]);
        }

        return env;
    }

    private void initializeTimings() {
        int[] streaks = new int[binaryInitData.length]; // overkill?

        int currentIndex = 0;
        int streakLength = 0;

        while (currentIndex < binaryInitData.length) {
            streakLength = getStreakLength(binaryInitData, currentIndex);

            if (streakLength == -1) // array terminated before streak was interrupted
                streakLength = binaryInitData.length - currentIndex;

            streaks[streakLength]++;
            currentIndex += streakLength;
        }

        // release memory
        this.binaryInitData = null;

        // number of samples per dit, hopefully
        int samples = indexOfMax(sumNeighbours(streaks, 1)) + 1;

        setTimings(samples);

        // calculate duration in ms for UI output
        int dit_duration = (int) Math.round(((double) samples / (double) sampleRate) * 1000d);
        EventBus.getDefault().postSticky(new MorseDitEvent(dit_duration));
    }

    private int indexOfMax(int[] array) {
        int index = 0;
        int max = Integer.MIN_VALUE;
        for (int i = 0; i < array.length; i++) {
            if (array[i] >= max) {
                index = i;
                max = array[i];
            }
        }
        return index;
    }

    private int[] sumNeighbours(int[] array, int width) {
        int[] result = Arrays.copyOf(array, array.length);

        for (int i = 0; i < result.length; i++) {
            for (int j = 1; j <= width; j++) {
                if (i - j > 0)
                    result[i] += array[i - j];
                if (i + j < array.length)
                    result[i] += array[i + j];

            }
        }
        return result;
    }


}
