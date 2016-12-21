package de.tu.darmstadt.seemoo.ansian.model.modulation;

import de.tu.darmstadt.seemoo.ansian.model.SamplePacket;

/**
 * Helper class for frequency modulation
 * @author Matthias Kannwischer
 */

public class FM {
    /**
     * @param x the signal that should be modulated
     * @param fs the sampling rate of the input and the output signal
     * @param freqdev the maximal frequency deviation of the FM, 75kHz for conventional radio broadcast
     * @return
     */
    public static SamplePacket fmmod(float[] x, float fs, float freqdev ){
        SamplePacket packet = new SamplePacket(x.length);
        packet.setSize(x.length);
        float[] sum = cumsum(x);
        for(int i=0;i<sum.length;i++){
            sum[i] = sum[i]/fs;
        }

        float[] re = packet.getRe();
        float[] im = packet.getIm();
        for(int i=0;i<sum.length;i++){
            re[i] = (float) Math.cos(2*Math.PI*freqdev*sum[i]);
            im[i] = (float) Math.sin(2*Math.PI*freqdev*sum[i]);
        }
        return packet;

    }


    /**
     * Calculates the cumulative sum. Just like the Matlab/Octave function cumsum
     * @param re the array that will be summed up
     * @return newly allocated array containing the sums
     */
    private static float[] cumsum(float[] re) {
        float[] sum = new float[re.length];
        sum[0] = re[0];
        for(int i=1;i<re.length;i++){
            sum[i]=re[i]+sum[i-1];
        }
        return sum;
    }
}

