package de.tu.darmstadt.seemoo.ansian.model.modulation;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.util.Log;

import de.tu.darmstadt.seemoo.ansian.model.SamplePacket;
import de.tu.darmstadt.seemoo.ansian.tools.ArrayHelper;

/**
 * Helper class for frequency modulation
 * @author Matthias Kannwischer
 */

public class FM  extends Modulation {
    private static final String LOGTAG = "FM";
    private AudioRecord recorder;
    private int sampleRate;


    // constants for audio recording
    private static final int BUFFER_SIZE = 8192;
    private static final int AUDIO_SAMPLERATE = 44100;
    private static final int RECORDER_CHANNELS = AudioFormat.CHANNEL_IN_MONO;
    private static final int RECORDER_AUDIO_ENCODING = AudioFormat.ENCODING_PCM_FLOAT;

    public FM(int sampleRate){
        this.sampleRate = sampleRate;


        this.recorder = new AudioRecord(MediaRecorder.AudioSource.MIC,
                AUDIO_SAMPLERATE, RECORDER_CHANNELS,
                RECORDER_AUDIO_ENCODING, 7168);
        Log.d(LOGTAG, "starting to record");

        if (this.recorder.getState() == AudioRecord.STATE_INITIALIZED) {
            this.recorder.startRecording();
        } else {
            this.recorder = null;
            Log.e(LOGTAG, "unable to initalize Audio Recorder");
        }
    }

    @Override
    public SamplePacket getNextSamplePacket() {
        Log.d(LOGTAG, "getNextSamplePacket()");
        if(this.recorder == null) return null;
        float[] buffer = new float[BUFFER_SIZE];
        this.recorder.read(buffer, 0, buffer.length, AudioRecord.READ_BLOCKING);

        float[] upsampled = ArrayHelper.upsample(buffer, (int) Math.ceil((float) this.sampleRate / AUDIO_SAMPLERATE));

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
    private static float[] cumsum(float[] re) {
        float[] sum = new float[re.length];
        sum[0] = re[0];
        for(int i=1;i<re.length;i++){
            sum[i]=re[i]+sum[i-1];
        }
        return sum;
    }


}

