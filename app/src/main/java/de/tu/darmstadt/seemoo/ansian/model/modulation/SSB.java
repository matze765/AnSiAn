package de.tu.darmstadt.seemoo.ansian.model.modulation;

import android.util.Log;

import de.tu.darmstadt.seemoo.ansian.model.AudioSource;
import de.tu.darmstadt.seemoo.ansian.model.SamplePacket;

/**
 * Created by MATZE on 15.01.2017.
 */

public class SSB extends Modulation {
    private static final String LOGTAG = "SSB";
    AudioSource audioSource;

    public SSB(int sampleRate){
        audioSource = new AudioSource(sampleRate);
        if(!audioSource.startRecording()){
            Log.e(LOGTAG, "unable to initalize Audio Recorder");
        }
    }
    @Override
    public SamplePacket getNextSamplePacket() {
        float[] upsampled = audioSource.getNextSamplesUpsampled();
        if(upsampled == null) return null;
        return new SamplePacket(upsampled);
    }
}
