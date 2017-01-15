package de.tu.darmstadt.seemoo.ansian.model;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.util.Log;

import de.tu.darmstadt.seemoo.ansian.tools.ArrayHelper;

/**
 * Created by MATZE on 15.01.2017.
 */

public class AudioSource {
    private static final String LOGTAG = "AudioSource";
    private static final int DEFAULT_BUFFER_SIZE = 8192;
    private static final int AUDIO_SAMPLERATE = 44100;
    private static final int RECORDER_CHANNELS = AudioFormat.CHANNEL_IN_MONO;
    private static final int RECORDER_AUDIO_ENCODING = AudioFormat.ENCODING_PCM_FLOAT;


    private int bufferSize;
    private int targetSampleRate;
    private AudioRecord recorder;

    public AudioSource(int targetSampleRate){
        this(targetSampleRate, DEFAULT_BUFFER_SIZE);
    }
    public AudioSource(int targetSampleRate, int bufferSize){
        this.targetSampleRate = targetSampleRate;
        this.bufferSize = bufferSize;
    }

    public boolean startRecording(){
        this.recorder = new AudioRecord(MediaRecorder.AudioSource.MIC,
                AUDIO_SAMPLERATE, RECORDER_CHANNELS,
                RECORDER_AUDIO_ENCODING, 7168);
        Log.d(LOGTAG, "starting to record");
        if (this.recorder.getState() == AudioRecord.STATE_INITIALIZED) {
            this.recorder.startRecording();
            return true;
        } else {
            this.recorder = null;
            Log.e(LOGTAG, "unable to initalize Audio Recorder");
            return false;
        }
    }

    public void stopRecording(){
        this.recorder.stop();
        this.recorder = null;
    }

    public float[] getNextSamplesUpsampled(){
        float[] buffer = this.getNextSamples();
        if(buffer == null) return null;
        float[] upsampled = ArrayHelper.upsample(buffer, (int) Math.ceil((float) this.targetSampleRate / AUDIO_SAMPLERATE));
        return upsampled;
    }

    public float[] getNextSamples(){
        if(this.recorder == null) return null;
        float[] buffer = new float[bufferSize];
        this.recorder.read(buffer, 0, buffer.length, AudioRecord.READ_BLOCKING);
        return buffer;
    }
}
