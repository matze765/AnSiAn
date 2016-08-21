package de.tu.darmstadt.seemoo.ansian.model.demodulation;

import android.util.Log;
import android.widget.Toast;

import java.util.Arrays;

import de.greenrobot.event.EventBus;
import de.greenrobot.event.Subscribe;
import de.tu.darmstadt.seemoo.ansian.control.events.DemodInfoEvent;
import de.tu.darmstadt.seemoo.ansian.control.events.DemodulationEvent;
import de.tu.darmstadt.seemoo.ansian.gui.misc.MyToast;
import de.tu.darmstadt.seemoo.ansian.model.ErrorBitSet;
import de.tu.darmstadt.seemoo.ansian.model.SamplePacket;
import de.tu.darmstadt.seemoo.ansian.model.preferences.DemodPreference;
import de.tu.darmstadt.seemoo.ansian.model.preferences.Preferences;
import de.tu.darmstadt.seemoo.ansian.tools.morse.Decoder;
import de.tu.darmstadt.seemoo.ansian.tools.morse.MorseCodeCharacterGetter;

public class Morse extends Demodulation {

    public static enum State {
        INIT, COLLECT_SAMPLES, INIT_STATS, DEMOD, STOPPED
    }

    public static enum Mode {
        AUTOMATIC, MANUAL
    }

    private static final int CODE_SUCCESS_BUFFER_SIZE = 100;
    private static final int SYMBOL_SUCCESS_BUFFER_SIZE = 30;
    private static final float REINIT_THRESHOLD = 0.5f;


    private String LOGTAG = "Morse";

    private boolean amDemod;
    private boolean automaticReinit;

    private DemodPreference prefs;
    private State state;
    private Mode mode;

    // buffers for data that is used for dit duration calibration
    private float[] initSamples;
    private boolean[] binaryInitSamples;

    // buffers for currently demodulated data; are re-used for efficiency
    private float[] currentEnvelope;
    private boolean[] currentHighLow;
    private int currentSampleCount;

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
    private int initTime;
    private int initSamplesRequired;
    private int initSamplesCollected;

    private StringBuilder currentSymbolCode;
    private Decoder decoder;

    private ErrorBitSet codeSuccess;
    private ErrorBitSet symbolSuccess;

    public Morse() {
        this.prefs = Preferences.DEMOD_PREFERENCE;
        this.decoder = new Decoder();

        // filters from AM demodulator
        MIN_USER_FILTER_WIDTH = 3000;
        MAX_USER_FILTER_WIDTH = 15000;

        EventBus.getDefault().register(this);
    }

    private void init() {
        this.state = State.INIT;
        this.amDemod = prefs.isAmDemod();
        this.initTime = prefs.getInitTime();
        this.mode = Mode.values()[prefs.getMode()];
        this.automaticReinit = prefs.isAutomaticReinit();

        this.peak = Float.MIN_VALUE;
        this.bottom = Float.MAX_VALUE;
        this.threshold = Float.NaN;

        // this is relevant for automatic dit duration only
        this.sampleRate = -1;
        this.initSamplesRequired = -1;
        this.initSamplesCollected = 0;

        this.dit = -1;
        this.dah = -1;
        this.word = -1;
        this.margin = -1;

        this.lastStreakLength = 0;
        this.lastStreakValue = false;

        this.currentSymbolCode = new StringBuilder();

        this.currentEnvelope = new float[0];
        this.currentHighLow = new boolean[0];

        this.codeSuccess = new ErrorBitSet(CODE_SUCCESS_BUFFER_SIZE);
        this.symbolSuccess = new ErrorBitSet(SYMBOL_SUCCESS_BUFFER_SIZE);

        //Log.d(LOGTAG, "initialiting Morse Demodulator; initTime: " + initTime);
        this.state = State.COLLECT_SAMPLES;
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

        // initialize sampleRate and initSamples if they are not yet initialized
        if (this.sampleRate == -1) {
            this.sampleRate = input.getSampleRate();
            this.initSamplesRequired = (int) Math.round((double) initTime * (double) sampleRate / 1000d);
            //Log.d(LOGTAG, "SampleRate " + sampleRate + ", initTime " + initTime + ", need " + initSamplesRequired + " samples.");
            this.initSamples = new float[initSamplesRequired];
        }

        switch (this.state) {
            case INIT:
                if (amDemod) {
                    envelopeToBuffer(input);
                    amDemodFromBuffer(output);
                }
                break;
            case COLLECT_SAMPLES:
                envelopeToBuffer(input);
                if (amDemod)
                    amDemodFromBuffer(output);
                collectSamplesFromBuffer();
                if (!(initSamplesCollected < initSamplesRequired)) {
                    initializeStats();
                }
                break;
            case INIT_STATS:
                if (amDemod) {
                    envelopeToBuffer(input);
                    amDemodFromBuffer(output);
                }
                break;
            case DEMOD:
                envelopeToBuffer(input);
                if (amDemod)
                    amDemodFromBuffer(output);
                updateThresholdFromBuffer();
                binarizeBuffer();
                demodulateBuffer();
                if (automaticReinit && needsReinit()) {
                    Log.d(LOGTAG, "Error rate too high; reinitializing");
                    init();
                }
                break;
            case STOPPED:
                // discard packets
                break;
        }

    }

    private boolean needsReinit() {
        return codeSuccess.needsReinit(REINIT_THRESHOLD) || symbolSuccess.needsReinit(REINIT_THRESHOLD);
    }

    private void ensureBufferCapacity(int size) {
        if (currentEnvelope == null || size > currentEnvelope.length) {
            currentEnvelope = new float[size];
            currentHighLow = new boolean[size];
        }
    }


    public void amDemodFromBuffer(SamplePacket output) {
        float[] reOut = output.getRe();

        System.arraycopy(currentEnvelope, 0, reOut, 0, currentSampleCount);

        output.setSize(currentSampleCount);
        output.setSampleRate(quadratureRate);
    }

    private void collectSamplesFromBuffer() {
        int freeSlots = initSamples.length - initSamplesCollected;
        int count = Math.min(currentSampleCount, freeSlots);
        System.arraycopy(currentEnvelope, 0, initSamples, initSamplesCollected, count);

        initSamplesCollected += count;
        //(LOGTAG, "Collected " + count + " samples, now I have " + initSamplesCollected + " samples.");
    }

    private void demodulateBuffer() {
        if (currentSampleCount < 1)
            return;

        int currentIndex = 0;
        int currentStreak = 0;

        // handle continued streak from last packet
        if (currentHighLow[0] == this.lastStreakValue) {
            currentStreak = getStreakLength(currentHighLow, 0);
            if (currentStreak == -1) { // array terminated before streak was interrupted
                this.lastStreakLength += currentSampleCount;
                return; // array ended, nothing more to demodulate
            } else {
                this.lastStreakLength += currentStreak;
                currentIndex = currentStreak; // start iterating the rest of the array from this point
            }
        }

        decode(this.lastStreakValue, this.lastStreakLength);

        // iterate over the array and new find streaks
        while (currentIndex < currentSampleCount) {
            currentStreak = getStreakLength(currentHighLow, currentIndex);

            if (currentStreak == -1) { // array terminated before streak was interrupted
                break; // array ended, nothing more to demodulate
            }

            decode(currentHighLow[currentIndex], currentStreak);
            currentIndex += currentStreak;
        }

        // set lastStreakValue and lastStreakLength for next packet
        this.lastStreakLength = currentSampleCount - currentIndex;
        this.lastStreakValue = currentHighLow[currentSampleCount - 1];
    }

    public void decode(boolean high, int streak) {
        //Log.d(LOGTAG, "Decoding streak of " + streak);

        if (streak < dit - margin) { // this cannot be a symbol we know
            codeSuccess.setBit(false);
            return;
        }

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

        //Log.d(LOGTAG, "Decoded streak was a  " + code);

        if (code == null) { // streak was not recognized
            codeSuccess.setBit(false);
            return;
        }


        EventBus.getDefault().postSticky(DemodInfoEvent.newAppendStringEvent(DemodInfoEvent.Position.TOP, code));
        codeSuccess.setBit(true);

        // check if a new symbol was completed
        if (" ".equals(code))
            createSymbol();
        else
            currentSymbolCode.append(code);

    }

    private void createSymbol() {
        String currentSymbolCodeString = currentSymbolCode.toString();
        String symbol = decoder.decode(currentSymbolCodeString);
        boolean recognized = !(symbol.contains(MorseCodeCharacterGetter.ESCAPE_START)
                || symbol.contains(MorseCodeCharacterGetter.ESCAPE_END));
        symbolSuccess.setBit(recognized);
        if (recognized)
            EventBus.getDefault().postSticky(DemodInfoEvent.newAppendStringEvent(DemodInfoEvent.Position.BOTTOM, symbol));
        currentSymbolCode = new StringBuilder();
    }

    /**
     * Returns the length of consecutive values from a defined startIndex in a boolean array.
     * Returns -1 if the streak does not terminate within the array or if startIndex is not in the array
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

    private void updateThresholdFromBuffer() {
        updateThreshold(currentEnvelope, currentSampleCount);
    }

    private void initThreshold() {
        updateThreshold(initSamples, initSamples.length);
    }

    private void updateThreshold(float[] envelope, int sampleCount) {
        for (int i = 0; i < sampleCount; i++) {
            if (envelope[i] > this.peak) {
                this.peak = envelope[i];
            }
            if (envelope[i] < this.bottom) {
                this.bottom = envelope[i];
            }
        }
        this.threshold = this.bottom + ((this.peak - this.bottom) / 2f);
    }


    /**
     * Initializes threshold and timings
     */
    private void initializeStats() {
        //Log.d(LOGTAG, "Initializing Stats");
        this.state = State.INIT_STATS;
        initThreshold();
        //Log.d(LOGTAG, "peak " + peak + ", bottom " + bottom + ", threshold " + threshold);

        if (this.mode == Mode.MANUAL) {
            int dit_duration = prefs.getDitDuration();
            int samples_per_dit = (int) Math.round(((double) dit_duration * (double) sampleRate) / 1000d);
            setTimings(samples_per_dit);
        } else { // this.mode == Mode.AUTOMATIC
            binarizeInitSamples();
            estimateTimings();
        }

        initSamples = null; // release memory
        binaryInitSamples = null; // release memory

        Log.d(LOGTAG, "Initialized Stats; Threshold: " + threshold + ", dit:" + dit + " samples");

        this.state = State.DEMOD;
    }


    /**
     * Turns collected init samples to high/low information and writes them into this.binaryInitSamples
     */
    private void binarizeInitSamples() {
        this.binaryInitSamples = binarize(initSamples);
    }

    private boolean[] binarize(float[] envelope) {
        boolean[] result = new boolean[envelope.length];
        for (int i = 0; i < result.length; i++) {
            result[i] = envelope[i] >= threshold;
        }
        return result;
    }

    private void binarizeBuffer() {
        for (int i = 0; i < currentSampleCount; i++) {
            currentHighLow[i] = currentEnvelope[i] >= threshold;
        }
    }

    private void envelopeToBuffer(SamplePacket input) {
        int count = input.size();
        float[] re = input.getRe();
        float[] im = input.getIm();

        ensureBufferCapacity(count);
        for (int i = 0; i < count; i++) {
            currentEnvelope[i] = (float) Math.sqrt(re[i] * re[i] + im[i] * im[i]);
        }
        currentSampleCount = count;
    }

    private void estimateTimings() {
        int[] streaks = new int[binaryInitSamples.length + 1]; //possibly overkill?

        int currentIndex = 0;
        int streakLength = 0;

        while (currentIndex < binaryInitSamples.length) {
            streakLength = getStreakLength(binaryInitSamples, currentIndex);

            if (streakLength == -1) // array terminated before streak was interrupted
                streakLength = binaryInitSamples.length - currentIndex;

            streaks[streakLength]++;
            currentIndex += streakLength;
        }

        // number of samples per dit
        int samples = indexOfMax(sumNeighbours(streaks, 1)) + 1;

        setTimings(samples);

        // calculate duration in ms for UI output
        int dit_duration = (int) Math.round(((double) samples / (double) sampleRate) * 1000d);
        MyToast.makeText("Timings initialized, one dit is about " + dit + " ms.", Toast.LENGTH_LONG);
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

    @Subscribe
    public void onEvent(DemodulationEvent event) {
        if (event.getDemodulation() == DemoType.MORSE)
            init();
        else
            this.state = State.STOPPED;
    }


}
