package de.tu.darmstadt.seemoo.ansian.model.modulation;

import android.util.Log;

import java.util.Arrays;

import de.tu.darmstadt.seemoo.ansian.model.SamplePacket;
import de.tu.darmstadt.seemoo.ansian.model.preferences.Preferences;
import de.tu.darmstadt.seemoo.ansian.tools.morse.Encoder;

/**
 * Created by dennis on 8/25/16.
 */
public class Morse extends Modulation {
    private static final String LOGTAG = "Morse";
    private final int morseFrequency;

    private char[] morseCode;
    private int currentSymbolIndex;
    private SamplePacket currentSymbol;
    private int wpm;
    private int samplesPerDit;
    private float[] sine;
    private float[] cosine;

    public Morse(String payload, int wpm, int samplerate, int morseFrequency) {
        this.wpm = wpm;
        this.sampleRate = samplerate;
        this.morseFrequency = morseFrequency;
        Encoder morseEncoder = new Encoder();
        String morseEncoded = "/" + morseEncoder.encode(payload) + "/";
        //String morseEncoded = "//./";
        this.morseCode = morseEncoded.toCharArray();
        this.currentSymbolIndex = 0;
        updateValues();
        Log.i(LOGTAG, "Morse: generated morse code from payload (=" + payload + "): [" + morseEncoded + "]");
    }

    @Override
    public void stop() {
        // no nothing
    }

    @Override
    public SamplePacket getNextSamplePacket() {
        if (currentSymbolIndex >= morseCode.length)
            return null;

        switch (morseCode[currentSymbolIndex]) {
            case '.':
                insertSilenceAndSine(1, 1);
                break;
            case '-':
                insertSilenceAndSine(1, 3);
                break;
            case ' ':
                insertSilenceAndSine(2, 0);
                break;
            case '/':
                insertSilenceAndSine(6, 0);
                break;
            default:
                Log.e(LOGTAG, "getNextSamplePacket: invalid morse code character: " + morseCode[currentSymbolIndex]);
        }
        currentSymbolIndex++;
        return currentSymbol;
    }

    public int getSamplesPerDit() {
        return samplesPerDit;
    }

    public void setWpm(int wpm) {
        this.wpm = wpm;
        updateValues();
    }

    @Override
    public void setSampleRate(int sampleRate) {
        super.setSampleRate(sampleRate);
        updateValues();
    }

    private void updateValues() {
        this.samplesPerDit = (int) (1200f / wpm * sampleRate / 1000);
        this.sine = new float[3 * samplesPerDit];
        this.cosine = new float[3 * samplesPerDit];
        long frequency = morseFrequency;
        for (int i = 0; i < sine.length; i++) {
            sine[i] = (float) Math.sin(2 * Math.PI * frequency * i / (float) sampleRate) * 0.999f;
            cosine[i] = (float) Math.cos(2 * Math.PI * frequency * i / (float) sampleRate) * 0.999f;
        }
        Log.i(LOGTAG, "updateValues: samplesPerDit=" + samplesPerDit + " sine frequency is " + frequency + " Hz");
    }

    private void insertSilenceAndSine(int durationInDitsSilence, int durationInDitsSine) {
        currentSymbol = new SamplePacket((durationInDitsSilence + durationInDitsSine) * samplesPerDit);
        float[] re = currentSymbol.getRe();
        float[] im = currentSymbol.getIm();
        Arrays.fill(re, 0, durationInDitsSilence * samplesPerDit, 0f);
        Arrays.fill(im, 0, durationInDitsSilence * samplesPerDit, 0f);
        System.arraycopy(cosine, 0, re, durationInDitsSilence * samplesPerDit, durationInDitsSine * samplesPerDit);
        System.arraycopy(sine, 0, im, durationInDitsSilence * samplesPerDit, durationInDitsSine * samplesPerDit);
        currentSymbol.setSampleRate(sampleRate);
        currentSymbol.setSize((durationInDitsSilence + durationInDitsSine) * samplesPerDit);
    }
}
