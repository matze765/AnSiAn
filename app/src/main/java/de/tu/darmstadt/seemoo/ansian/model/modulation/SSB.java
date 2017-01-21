package de.tu.darmstadt.seemoo.ansian.model.modulation;

import android.util.Log;

import java.lang.reflect.Array;

import de.tu.darmstadt.seemoo.ansian.model.AudioSource;
import de.tu.darmstadt.seemoo.ansian.model.SamplePacket;
import de.tu.darmstadt.seemoo.ansian.model.filter.ComplexFirFilter;
import de.tu.darmstadt.seemoo.ansian.tools.ArrayHelper;

/**
 * Created by MATZE on 15.01.2017.
 */

public class SSB extends Modulation {
    private static final String LOGTAG = "SSB";
    private AudioSource audioSource;
    private int sampleRate;
    private boolean isUpperSideband;


    public SSB(int sampleRate, boolean isUpperSideBand){
        audioSource = new AudioSource(sampleRate);
        this.sampleRate = sampleRate;
        this.isUpperSideband = isUpperSideBand;
        if(!audioSource.startRecording()){
            Log.e(LOGTAG, "unable to initalize Audio Recorder");
        }
    }
    @Override
    public SamplePacket getNextSamplePacket() {
        // get audio samples
        SamplePacket packet = audioSource.getNextSamplePacket();

        if(packet == null) return null;

        // filter out the upper or lower side band
        ComplexFirFilter filter = ComplexFirFilter.createBandPass(1,1, audioSource.getAudioSamplerate(),
                isUpperSideband ? 0 : -8000,
                isUpperSideband ? 8000: 0,
                audioSource.getAudioSamplerate()*0.01f, 40);

        SamplePacket output = new SamplePacket(packet.size());
        filter.filter(packet,output, 0, packet.size());

        // upsample from audio frequency (usually 44.1kHz) to our transmission frequency (1MHz)
        SamplePacket upsampled = output.upsample((int) Math.ceil((float) this.sampleRate / audioSource.getAudioSamplerate()));
        return upsampled;
    }
}
