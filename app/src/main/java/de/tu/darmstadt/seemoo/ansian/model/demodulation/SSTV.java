package de.tu.darmstadt.seemoo.ansian.model.demodulation;

import android.graphics.Bitmap;

import java.io.File;
import java.io.IOException;

import de.tu.darmstadt.seemoo.ansian.model.SamplePacket;

/**
 * This class is supposed to implement the SSTV demodulation
 * THIS IS NOT FUNCTIONAL
 * It is just for reference how a demodulation could work.
 * It expects the samples of the entire sstv image. When using the reception chain this
 * will be received in small parts that need to be combined. The demodulated lines
 * should be passed to the UI one-by-one, not only once the bitmap is complete.
 *
 * However, this can only be done once the fmdemod is implemented correctly. Currently, this part
 * is not working properly.
 *
 *
 * @author Matthias Kannwischer
 */

public class SSTV {
    private final static float SYNC_FREQUENCY = 1200;
    private final static float BLACK_FREQUENCY = 1500;
    private final static float WHITE_FREQUNCY = 2300;
    private final static int NUM_LINES = 120;
    private final static int NUM_PIXELS_PER_LINE = 256;

    private final static float LINE_SYNC_SECONDS = 0.005f;
    private final static float FRAME_SYNC_SECONDS = 0.03f;
    private final static float LINE_DURATION_SECONDS = (1/15.0f)-LINE_SYNC_SECONDS;


    private static int quadratureRate = 1_000_000;
    private static int deviation = 2300;
    private static SamplePacket demodulatorHistory;


    /**
     * Demodulates a incoming Sample packet
     * Packet is expected to hold the entire image.
     * TODO: This need to work with smaller chunks of samples
     * @param in the samples that need to be demodulated
     * @return demodulated bitmap
     */
    private Bitmap demodulate(SamplePacket in){

        SamplePacket out = new SamplePacket(in.size());
        fmDemodulate(in, out);

        float[] samples = out.getRe();
        // find start of a frame
        int startOfLine = findStartOfLine(out.getRe(), 0, SYNC_FREQUENCY, FRAME_SYNC_SECONDS);


        int[] data = new int[NUM_LINES*NUM_PIXELS_PER_LINE];
        int samplesPerPixel = Math.round((LINE_DURATION_SECONDS*quadratureRate)/NUM_PIXELS_PER_LINE);
        for (int i = 0; i < NUM_LINES; i++) {
            for(int j=0;j<NUM_PIXELS_PER_LINE;j++){
                // get a float between 1 and 0
                // average of the sampled frequencies
                float pixel = 0.0f;
                for(int z=0;z<samplesPerPixel;z++){
                    pixel += (samples[startOfLine+j*samplesPerPixel+z]-BLACK_FREQUENCY)/(WHITE_FREQUNCY-BLACK_FREQUENCY)/samplesPerPixel;
                }

                // correct bounds of pixel
                if(pixel > 1.0f) pixel =1.0f;
                if(pixel < -0.0f) pixel = 0.0f;

                // convert to an int
                data[i*256+j] = (int) (255*(pixel));
            }

            // adjust start of the next line
            startOfLine = findStartOfLine(samples, startOfLine+NUM_PIXELS_PER_LINE*samplesPerPixel, SYNC_FREQUENCY, LINE_SYNC_SECONDS);
        }

        return writeByteArrayToImage(data);
    }


    /**
     * Searches for the start of a sstv line.
     * TODO: Needs to be tuned to work with actual signals.
     *
     * @param samples the samples (frequency domain)
     * @param offset where to start searching for
     * @param syncFrequency the sync frequency (usually 1200 Hz)
     * @param syncDuration the duration of the sync sequence (0.03s for frame sync, 0.005s for line sync)
     * @return the first index of the line
     */
    private int findStartOfLine(float[] samples, int offset, float syncFrequency, float syncDuration) {
        int ctr = 0;
        // find a sequence of at least
        int requiredSyncSamples = Math.round((0.8f * syncDuration)*quadratureRate);
        for (int i = offset; i < samples.length; i++) {

            // check if current frequency is in sync frequency range
            if(samples[i] < syncFrequency+150 &&  samples[i] > syncFrequency-150) {
                ctr++;
            } else {
                // check if we found a sync sequence that is long enough
                if(ctr >= requiredSyncSamples) {
                    // if yes, return the end index of the sync sequence, which is the start of the line
                    return i;
                } else {
                    // if no, reset the ctr
                    // this might be bad if we have single sample outliers
                    ctr=0;
                }
            }
        }
        return -1;
    }

    /**
     *  Converts gray scale byte data to a image
     * @param data containing gray scale values (0-255). Expected to have correct size for mode
     * @return the bitmap containing the gray scale sstv image
     */
    private Bitmap writeByteArrayToImage(int[] data) {
        Bitmap image = Bitmap.createBitmap(NUM_PIXELS_PER_LINE, NUM_LINES, Bitmap.Config.RGB_565);

        for(int y=0;y< NUM_LINES;y++){
            for(int x=0;x<NUM_PIXELS_PER_LINE;x++){
                int pixel = data[y*NUM_PIXELS_PER_LINE+x];
                image.setPixel(x,y,pixel<<16+pixel<<8+pixel);
            }
        }
        return image;
    }


    /**
     * Implements the frequency demodulation (copied from {@link FM}
     *
     * @param input the input samples
     * @param output the output samples
     */
    private void fmDemodulate(SamplePacket input, SamplePacket output) {
        float[] reIn = input.getRe();
        float[] imIn = input.getIm();
        float[] reOut = output.getRe();
        float[] imOut = output.getIm();
        int inputSize = input.size();

        float quadratureGain = quadratureRate / (2 * (float) Math.PI * deviation);

        if (inputSize < 1)
            return;

        if (demodulatorHistory == null) {
            demodulatorHistory = new SamplePacket(1);
            demodulatorHistory.getRe()[0] = reIn[0];
            demodulatorHistory.getIm()[0] = reOut[0];
        }

        // Quadrature demodulation:
        reOut[0] = reIn[0] * demodulatorHistory.re(0) + imIn[0] * demodulatorHistory.im(0);
        imOut[0] = imIn[0] * demodulatorHistory.re(0) - reIn[0] * demodulatorHistory.im(0);
        reOut[0] = quadratureGain * (float) Math.atan2(imOut[0], reOut[0]);
        for (int i = 1; i < inputSize; i++) {
            reOut[i] = reIn[i] * reIn[i - 1] + imIn[i] * imIn[i - 1];
            imOut[i] = imIn[i] * reIn[i - 1] - reIn[i] * imIn[i - 1];
            reOut[i] = quadratureGain * (float) Math.atan2(imOut[i], reOut[i]);
        }
        demodulatorHistory.getRe()[0] = reIn[inputSize - 1];
        demodulatorHistory.getIm()[0] = imIn[inputSize - 1];
        output.setSize(inputSize);
        output.setSampleRate(quadratureRate);
        output.setFrequency(input.getFrequency());
    }
}
