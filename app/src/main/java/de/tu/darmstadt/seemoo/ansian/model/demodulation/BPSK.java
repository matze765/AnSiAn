package de.tu.darmstadt.seemoo.ansian.model.demodulation;

import android.util.Log;

import de.tu.darmstadt.seemoo.ansian.model.SamplePacket;

public class BPSK {
    private static final String LOGTAG = "BPSK";

    private float baudrate;
    private float[] magnitudes;
    private int magnitudesSize;
    private int symbolStart;

    public BPSK(float baudrate) {
        super();
        this.baudrate = baudrate;
    }

    /**
     * Will calculate the index of the minimum value in the range between
     * start and end index (both included).
     *
     * @param buffer    float array holding the values
     * @param startIdx  index to start from (inclusive)
     * @param endIdx    index to end (inclusive)
     * @return index of the first minimum that was found
     */
    private int findMin(float[] buffer, int startIdx, int endIdx) {
        int min = startIdx;
        for(int i = startIdx+1; i <= endIdx; i++)
            if(buffer[i] < buffer[min])
                min = i;
        return min;
    }

    private float calcMean(float[] buffer, int startIdx, int endIdx) {
        float avg = 0;
        for(int i = startIdx+1; i <= endIdx; i++)
            avg += buffer[i];
        return avg / (endIdx-startIdx+1);
    }

    /**
     * This will demodulate the given samples into bits
     * (Uses Manchester Encoding, e.g. in RDS)S
     *
     * @param input         input sample packet (baseband BPSK, already filtered)
     * @param output        output buffer for the demodulated bits (one bit per array element)
     * @param outStartIdx   index at which the output buffer shall be filled
     * @return number of demodulated bits
     */
    public int demodulate(SamplePacket input, byte[] output, int outStartIdx) {
        int outputIndex = outStartIdx;

        // if the magnitudes array is uninitialized or too small, recreate:
        if(magnitudes == null || magnitudes.length < 2*input.size()) {
            magnitudes = new float[input.size()*2];
            magnitudesSize = 0;
            symbolStart = -1;
        }

        // Calculate the average number of samples which make up one symbol (one bit):
        float samplesPerSymbol = input.getSampleRate() / baudrate;
        int samplesPerSymbolMin = (int) (samplesPerSymbol * 0.75);
        int samplesPerSymbolMax = (int) (samplesPerSymbol * 1.25);

        // Remove DC offset and calculate the magnitude of the signal
        float[] reIn = input.getRe();
        float avg = calcMean(reIn, 0, input.size()-1);
        for(int i = 0; i < input.size(); i++) {
            magnitudes[magnitudesSize+i] = Math.abs(reIn[i]-avg);
        }
        magnitudesSize += input.size();

        // If we do not know the start of the next symbol, find it by looking for the
        // minimum in the next samplesPerSymbol samples
        if(symbolStart < 0) {
            symbolStart = findMin(magnitudes, 0, (int) Math.ceil(samplesPerSymbol));
        }

        while (symbolStart + 2*samplesPerSymbol < magnitudesSize) {
            // find the end of the symbol
            int symbolEnd = findMin(magnitudes, symbolStart+samplesPerSymbolMin, symbolStart+samplesPerSymbolMax);
            int symbolLen = symbolEnd-symbolStart;

            // calculate the mean over the samples within the symbol to derive the threshold
            float mean = calcMean(magnitudes, symbolStart, symbolEnd);
            float threshold = mean / 2;

            // find the minimum value in the middle of the sample
            int symbolCenter = findMin(magnitudes, (int)(symbolStart+symbolLen*0.25), (int)(symbolStart+symbolLen*0.75));

            // Check for a timing error (that means, symbolStart and symbolEnd are not correct
            // but rather of by half the symbol length):
            if(magnitudes[symbolEnd] > threshold) {
                //Log.d(LOGTAG, "demodulate: Wrong timing!");
                symbolStart = symbolCenter;
                continue;
            }

            // before we demodulate the bit, check if output has room left:
            if(outputIndex >= output.length) {
                Log.w(LOGTAG, "demodulate: output is full. abort.");
                break;
            }

            // Now decide if the symbol is a 1 or a 0:
            if(magnitudes[symbolCenter] > threshold)
                output[outputIndex] = 1;
            else
                output[outputIndex] = 0;
            outputIndex++;

            // continue with the next symbol:
            symbolStart = symbolEnd;
        }

        // Save remaining samples:
        int remainingSamples = magnitudesSize-symbolStart;
        System.arraycopy(magnitudes, symbolStart, magnitudes, 0, remainingSamples);
        magnitudesSize = remainingSamples;
        symbolStart = 0;

        return outputIndex-outStartIdx; // Return number of demodulated bits
    }
}
