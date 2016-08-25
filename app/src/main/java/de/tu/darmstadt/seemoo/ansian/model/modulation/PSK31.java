package de.tu.darmstadt.seemoo.ansian.model.modulation;

import java.util.Arrays;

import de.tu.darmstadt.seemoo.ansian.model.SamplePacket;

/**
 * Created by dennis on 8/25/16.
 */
public class PSK31 extends Modulation {
    private String payloadString;
    private char[] bits;
    private int samplesPerSymbol;
    private int currentSymbolIndex;
    private SamplePacket currentSymbol;
    private float[] cosine;
    private float[] cosineNegative;
    private int phase = 1;

    public PSK31(String payload, int sampleRate) {
        this.payloadString = payload;
        this.sampleRate = sampleRate;
        this.currentSymbolIndex = 0;
        updateValues();

        prepareBits();
    }

    @Override
    public SamplePacket getNextSamplePacket() {
        if(currentSymbolIndex >= bits.length)
            return null;
        float[] re = currentSymbol.getRe();
        float[] im = currentSymbol.getIm();
        if(bits[currentSymbolIndex] == '1') {
            Arrays.fill(re, 0, samplesPerSymbol, 0.99f*phase);
        } else {
            if(phase > 0)
                System.arraycopy(cosine, 0, re, 0, samplesPerSymbol);
            else
                System.arraycopy(cosineNegative, 0, re, 0, samplesPerSymbol);
            phase *= -1;
        }
        Arrays.fill(im, 0, samplesPerSymbol, 0f);
        currentSymbol.setSampleRate(sampleRate);
        currentSymbol.setSize(samplesPerSymbol);
        currentSymbolIndex++;
        return currentSymbol;
    }

    @Override
    public void setSampleRate(int sampleRate) {
        super.setSampleRate(sampleRate);
        updateValues();
    }

    private void updateValues() {
        this.samplesPerSymbol = (int) (sampleRate / 31.25f);
        this.currentSymbol = new SamplePacket(samplesPerSymbol);
        this.cosine = new float[samplesPerSymbol];
        this.cosineNegative = new float[samplesPerSymbol];
        for(int i = 0; i < cosine.length; i++) {
            cosine[i] = (float) Math.cos(Math.PI * i / (float) samplesPerSymbol) * 0.999f;
            cosineNegative[i] = (float) Math.cos(Math.PI * i / (float) samplesPerSymbol) * -0.999f;
        }
    }

    private void prepareBits() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("00000000000000000000");
        for(char letter: payloadString.toCharArray()) {
            for(String[] entry: de.tu.darmstadt.seemoo.ansian.model.demodulation.PSK31.lookupTable) {
                if(letter == entry[1].charAt(0)) {
                    stringBuilder.append(entry[0]);
                    stringBuilder.append("00");
                }
            }
        }
        stringBuilder.append("00000000000000000000");
        bits = stringBuilder.toString().toCharArray();
    }
}
