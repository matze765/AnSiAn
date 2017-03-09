package de.tu.darmstadt.seemoo.ansian.model.modulation;

import android.util.Log;
import de.tu.darmstadt.seemoo.ansian.model.AudioSource;
import de.tu.darmstadt.seemoo.ansian.model.SamplePacket;

/**
 * Helper class for frequency modulation
 * @author Matthias Kannwischer
 */

public class FM  extends Modulation {
    private static final String LOGTAG = "FM";
    private int sampleRate;
    private AudioSource audioSource;



    public FM(int sampleRate){
        this.sampleRate = sampleRate;
        this.audioSource = new AudioSource(this.sampleRate);
        if(!this.audioSource.startRecording()){
            Log.e(LOGTAG, "unable to initalize Audio Recorder");
        }
    }

    @Override
    public void stop() {
        audioSource.stopRecording();
    }

    @Override
    public SamplePacket getNextSamplePacket() {
        Log.d(LOGTAG, "getNextSamplePacket()");
        if(this.audioSource == null) return null;
        float[] upsampled = this.audioSource.getNextSamplesUpsampled();
        if(upsampled == null) return null;
        SamplePacket resultPacket = FM.fmmod(upsampled, sampleRate, 75000);
        Log.d(LOGTAG, "result.size()=" + resultPacket.size());
        return resultPacket;
    }


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
    public static float[] cumsum(float[] re) {
        float[] sum = new float[re.length];
        sum[0] = re[0];
        for(int i=1;i<re.length;i++){
            sum[i]=re[i]+sum[i-1];
        }
        return sum;
    }


}

