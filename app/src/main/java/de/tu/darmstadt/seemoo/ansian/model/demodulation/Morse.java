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

    public enum State {
        INIT, COLLECT_SAMPLES, INIT_STATS, DEMOD, STOPPED
    }

    public enum Mode {
        AUTOMATIC, MANUAL
    }

    // parameters for automatic re-initialization
    public static final int CODE_SUCCESS_BUFFER_SIZE = 100;
    public static final int SYMBOL_SUCCESS_BUFFER_SIZE = 30;
    public static final float REINIT_SUCCESS_THRESHOLD = 0.5f;
    // minimal time for automatically detected dit duration; if detected duration is smaller, timings get re-initialized
    private static final int MIN_DIT_TIME_MS = 7;

    public static final String LOGTAG = "Morse";

    private DemodPreference prefs;
    private State state;
    private Mode mode;
    private Decoder decoder;

    private ErrorBitSet codeSuccess;
    private ErrorBitSet symbolSuccess;

    private boolean amDemod;
    private boolean automaticReinit;
    private int sampleRate;
    private int initTime;
    private int initSamplesRequiredForInit;
    private int initSamplesCollected;

    // buffers for initialization data
    private float[] initSamples;
    private boolean[] binaryInitSamples;

    // buffers for currently demodulated data; are re-used for efficiency
    private float[] currentEnvelope;
    private boolean[] currentHighLow;
    private int currentSampleCount; // buffer usage

    // highest/lowest sample seen; threshold = (peak + bottom) / 2
    private float peak;
    private float bottom;
    private float threshold;

    // duration of dits, dahs, etc. in #samples
    private int dit;
    private int dah;
    private int word;
    private int margin;

    // for keeping track of high/low streaks across several SamplePackets
    private int lastStreakLength;
    private boolean lastStreakValue;

    // currently detected dits/dahs
    private StringBuilder currentSymbolCode;

    public Morse() {
        this.prefs = Preferences.DEMOD_PREFERENCE;
        this.decoder = new Decoder();

        // filters from AM demodulator
        MIN_USER_FILTER_WIDTH = 3000;
        MAX_USER_FILTER_WIDTH = 15000;
        userFilterCutOff = (MAX_USER_FILTER_WIDTH + MIN_USER_FILTER_WIDTH) / 2;

        EventBus.getDefault().register(this);
    }

    /**
     * Initializes this Morse demodulator so it can begin collecting samples and determine a
     * threshold and, optionally, the dit duration. Can be called on a already-initialized Morse
     * Demodulator to re-initialize it.
     */
    private void init() {
        this.state = State.INIT;
        this.amDemod = prefs.isAmDemod();
        this.initTime = prefs.getInitTime();
        this.mode = Mode.values()[prefs.getMode()];
        this.automaticReinit = prefs.isAutomaticReinit();

        this.peak = Float.MIN_VALUE;
        this.bottom = Float.MAX_VALUE;
        this.threshold = Float.NaN;

        this.sampleRate = -1;
        this.initSamplesRequiredForInit = -1;
        this.initSamplesCollected = 0;

        this.dit = -1;
        this.dah = -1;
        this.word = -1;
        this.margin = -1;

        this.lastStreakLength = 0;
        this.lastStreakValue = false;

        this.currentSymbolCode = new StringBuilder();

        this.currentEnvelope = new float[0]; // gets enlarged as needed
        this.currentHighLow = new boolean[0]; // gets enlarged as needed

        this.codeSuccess = new ErrorBitSet(CODE_SUCCESS_BUFFER_SIZE);
        this.symbolSuccess = new ErrorBitSet(SYMBOL_SUCCESS_BUFFER_SIZE);

        this.state = State.COLLECT_SAMPLES;
    }

    @Override
    public DemoType getType() {
        return DemoType.MORSE;
    }

    /**
     * Sets the duration of a dit, dah, etc. based on how many samples one dit is
     *
     * @param dit Number of samples per dit
     */
    private void setDurations(int dit) {
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
            this.initSamplesRequiredForInit = (int) Math.round((double) initTime * (double) sampleRate / 1000d);
            this.initSamples = new float[initSamplesRequiredForInit];
        }

        // if demodulate is called before init, do nothing
        if (this.state == null) {
            return;
        }

        if (!amDemod)
            output.setSize(0);

        switch (this.state) {
            case INIT:
                if (amDemod) {
                    // do nothing except AM Demodulation; Demodulator might be in inconsistent state
                    envelopeToBuffer(input);
                    amDemodFromBuffer(output);
                }
                break;
            case COLLECT_SAMPLES:
                // collect samples and write them to initSamples until we have enough
                envelopeToBuffer(input);
                if (amDemod)
                    amDemodFromBuffer(output);
                collectSamplesFromBuffer();
                if (!(initSamplesCollected < initSamplesRequiredForInit)) {
                    initializeStats();
                }
                break;
            case INIT_STATS:
                if (amDemod) {
                    // do nothing except AM Demodulation; Demodulator might be in inconsistent state
                    envelopeToBuffer(input);
                    amDemodFromBuffer(output);
                }
                break;
            case DEMOD:
                // demodulate samples
                envelopeToBuffer(input);
                if (amDemod)
                    amDemodFromBuffer(output);
                updateThresholdFromBuffer();
                binarizeBuffer();
                demodulateBuffer();
                if (automaticReinit && needsReinit()) {
                    MyToast.makeText("High decoding error rate; reinitializing...", Toast.LENGTH_LONG);
                    init();
                }
                break;
            case STOPPED:
                // discard samples; Demodulator should not be running
                break;
        }

    }

    /**
     * Determines if the Demodulator needs to be re-initialized based on decoding and demodulation error rates
     *
     * @return true if the Demodulator needs to be re-initialized, false otherwise
     */
    private boolean needsReinit() {
        return codeSuccess.needsReinit(REINIT_SUCCESS_THRESHOLD) || symbolSuccess.needsReinit(REINIT_SUCCESS_THRESHOLD);
    }

    /**
     * Makes sure that currentEnvelope and currentHighLow can hold at least size entries
     *
     * @param size the requested minimal buffer size
     */
    private void ensureBufferCapacity(int size) {
        if (currentEnvelope == null || size > currentEnvelope.length) {
            currentEnvelope = new float[size];
            currentHighLow = new boolean[size];
        }
    }


    /**
     * Writes demodulated AM data from currently demodulated input to a supplied SamplePacket.
     * Essentially, this method just copies the envelope, which is already in the buffer, to the
     * real part of the SamplePacket.
     *
     * @param output the SamplePacket to write the demodulated AM data to
     */
    public void amDemodFromBuffer(SamplePacket output) {
        float[] reOut = output.getRe();

        System.arraycopy(currentEnvelope, 0, reOut, 0, currentSampleCount);

        output.setSize(currentSampleCount);
        output.setSampleRate(quadratureRate);
    }

    /**
     * Collects envelope data from current buffer and saves them in initSamples.
     */
    private void collectSamplesFromBuffer() {
        int freeSlots = initSamples.length - initSamplesCollected;
        int count = Math.min(currentSampleCount, freeSlots);
        System.arraycopy(currentEnvelope, 0, initSamples, initSamplesCollected, count);

        initSamplesCollected += count;
    }

    /**
     * Demodulates the data in currentHighLow
     */
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

    /**
     * Decodes a streak of high/low samples into a dit/dah/pause
     *
     * @param high   is the streak a high or a low streak?
     * @param streak length of the streak
     */
    public void decode(boolean high, int streak) {

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

        if (code == null) { // streak was not recognized
            codeSuccess.setBit(false);
            return;
        }

        // send code to GUI
        EventBus.getDefault().postSticky(DemodInfoEvent.newAppendStringEvent(DemodInfoEvent.Position.TOP, code));

        codeSuccess.setBit(true);

        // check if a new symbol was completed
        if (" ".equals(code))
            createSymbol();
        else
            currentSymbolCode.append(code);

    }

    /**
     * Decodes the current streak of dits/dahs in currentSymbolCode and sends the decoded symbol to
     * GUI
     */
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
     *
     * @param array      the array to iterate over
     * @param startIndex the index to start looking for consecutive values
     * @return the number of consecutive values; -1 if the streak does not terminate within the
     * array or if startIndex is not in the array
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

    /**
     * Updates peak, bottom and threshold based on data in currentEnvelope
     */
    private void updateThresholdFromBuffer() {
        updateThreshold(currentEnvelope, currentSampleCount);
    }

    /**
     * Initializes peak, bottom and threshold based on data in initSamples
     */
    private void initThreshold() {
        updateThreshold(initSamples, initSamples.length);
    }

    /**
     * Updates peak, bottom and threshold based on data in supplied array
     *
     * @param envelope    array to check for new peak/bottom
     * @param sampleCount number of samples in envelope array
     */
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
     * Initializes threshold and timings based on data in initSamples
     */
    private void initializeStats() {
        this.state = State.INIT_STATS;
        initThreshold();

        if (this.mode == Mode.MANUAL) {
            // dit duration (in ms) can be read from preferences
            int dit_duration = prefs.getDitDuration();
            int samples_per_dit = (int) Math.round(((double) dit_duration * (double) sampleRate) / 1000d);
            setDurations(samples_per_dit);
        } else {
            // dit duration needs to be estimated based on initSamples
            binarizeInitSamples();
            if (!estimateTimings()) { // re-initialize if dit time estimate is out of range
                initSamples = null; // release memory
                binaryInitSamples = null; // release memory
                init();
                return;
            }
        }

        initSamples = null; // release memory
        binaryInitSamples = null; // release memory

        Log.d(LOGTAG, "Initialized Stats; Threshold: " + threshold + ", dit:" + dit + " samples");

        this.state = State.DEMOD;
    }


    /**
     * Turns collected init samples into high/low information and writes it into this.binaryInitSamples
     */
    private void binarizeInitSamples() {
        this.binaryInitSamples = binarize(initSamples);
    }

    /**
     * Returns a binary representation of the supplied array with true for values >= threshold
     *
     * @param envelope the array to binarize
     * @return a binary representation of the supplied array
     */
    private boolean[] binarize(float[] envelope) {
        boolean[] result = new boolean[envelope.length];
        for (int i = 0; i < result.length; i++) {
            result[i] = envelope[i] >= threshold;
        }
        return result;
    }

    /**
     * Turns current buffer into high/low information and writes it into this.currentHighLow
     */
    private void binarizeBuffer() {
        for (int i = 0; i < currentSampleCount; i++) {
            currentHighLow[i] = currentEnvelope[i] >= threshold;
        }
    }

    /**
     * Performs envelope detection on the supplied SamplePacket and writes the result into
     * currentEnvelope
     *
     * @param input the SamplePacket to process
     */
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

    /**
     * Estimates the duration of a dit based on the collected initialization samples in binaryInitSamples
     *
     * @return true if the estimated dit duration is a plausible value, false otherwise
     */
    private boolean estimateTimings() {
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

        setDurations(samples);

        // calculate duration in ms for UI output
        int dit_duration = (int) Math.round(((double) samples / (double) sampleRate) * 1000d);

        if (dit_duration < MIN_DIT_TIME_MS) {
            MyToast.makeText("Detected timings out of range; reinitializing...", Toast.LENGTH_LONG);
            return false;
        } else {
            MyToast.makeText("Timings initialized, one dit is about " + dit_duration + " ms.", Toast.LENGTH_LONG);
            return true;
        }


    }

    /**
     * Returns the (last) index of the biggest value in this array
     *
     * @param array the array to process
     * @return the (last) index of the biggest value in this array
     */
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

    /**
     * For every value in the array, the <code>width</code> left and right are added to this value.
     * Returns the result as a new array; does not manipulate the supplied array.     *
     *
     * @param array the array to process
     * @param width the number of left and right neighbors to add for each value
     * @return a new array in which every value is the sum of the corresponding value and its
     * <code>width</code> neighbors in the original array
     */
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
        if (event.getDemodulation() == DemoType.MORSE) {

            // clear DemodulationInfoView
            EventBus.getDefault().postSticky(DemodInfoEvent.newReplaceStringEvent(DemodInfoEvent.Position.TOP, "", false));
            EventBus.getDefault().postSticky(DemodInfoEvent.newReplaceStringEvent(DemodInfoEvent.Position.BOTTOM, "", false));

            init();
        } else {
            this.state = State.STOPPED;
        }
    }


}
