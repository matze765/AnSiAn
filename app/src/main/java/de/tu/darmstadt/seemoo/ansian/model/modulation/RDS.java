package de.tu.darmstadt.seemoo.ansian.model.modulation;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Environment;
import android.support.v4.content.ContextCompat;
import android.support.v4.util.Pools;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.Array;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.jar.Manifest;

import de.tu.darmstadt.seemoo.ansian.model.AudioSource;
import de.tu.darmstadt.seemoo.ansian.model.SamplePacket;
import de.tu.darmstadt.seemoo.ansian.model.filter.FirFilter;
import de.tu.darmstadt.seemoo.ansian.tools.ArrayHelper;

/**
 * Implementation of the FM+RDS modulation.
 * Modulates an audio signal with additional RDS information, such that it can be received
 * with the RDS demodulation of AnSiAn or any RDS capable radio.
 *
 * @author Matthias Kannwischer
 */

public class RDS extends Modulation {
    private static final String LOGTAG = "RDS";

    private static final float BAUDRATE = 1187.5f;
    private static final float PILOT_FREQUENCY = 19000;
    private static final float TONE_FREQUENCY = 57000;
    private static final int AUDIO_SAMPLERATE = 44100;

    private static final int RDS_FREQUENCY = 200000;
    private int sampleRate;

    private float[] preCalculatedPacket;
    private AudioSource audioSource;
    private BufferedInputStream audioFile = null;
    private boolean readFromAudioFile;


    /**
     * Initializes the RDS transmission.
     * This precalculates a few groups of RDS which later justs needs to be added to the audio signal.
     * This is done to save time later.
     *
     * @param stationName the station name. expected to have exactly 8 chars
     * @param sampleRate the sample rate that should be used to modulate the RDS signal. currently fixed to 1MHz
     * @param fileAudioSource true if the audio should be read from a file, otherwise we will use the microphone
     */
    public RDS(String stationName, int sampleRate, boolean fileAudioSource) {
        super();
        Log.d(LOGTAG,"starting precalculation time="+System.currentTimeMillis());
        this.readFromAudioFile = fileAudioSource;


        this.sampleRate = sampleRate;

        // generate groups
        byte[] groups = this.createBitsFromStationName(stationName);

        Log.d(LOGTAG, "groups="+Arrays.toString(groups));


        // calculate check words
        byte[] groupWithCheckwords = this.calculateCheckwords(groups);

        groupWithCheckwords = ArrayHelper.repeat(groupWithCheckwords, 4);


        // do differential encoding
        byte[] differentialEncoded = this.calculateDifferentialEncoding(groupWithCheckwords);


        // do manchester encoding and upsample

        preCalculatedPacket = this.calculateManchesterEncodingAndUpsampling(differentialEncoded);

        SamplePacket packetManchester = new SamplePacket(preCalculatedPacket.length);
        packetManchester.setSize(preCalculatedPacket.length);
        System.arraycopy(preCalculatedPacket, 0, packetManchester.getRe(), 0, preCalculatedPacket.length);


        // filter frequencies higher than 2800 Hz

        SamplePacket filteredPacket = new SamplePacket(preCalculatedPacket.length);
        float cutoff = 2800;
        float transWidth = 300;
        float attenuation = 3;
        FirFilter filter1 = FirFilter.createLowPass(1, 1,RDS_FREQUENCY, cutoff, transWidth, attenuation);
        filter1.filterReal(packetManchester, filteredPacket, 0, preCalculatedPacket.length);

        preCalculatedPacket = filteredPacket.getRe();
        ArrayHelper.packetNormalize(preCalculatedPacket);


        // create pilot and tone

        float[] pilot = this.createTone(PILOT_FREQUENCY, RDS_FREQUENCY, this.preCalculatedPacket.length);
        float[] tone = this.createTone(TONE_FREQUENCY, RDS_FREQUENCY, this.preCalculatedPacket.length);

        ArrayHelper.packetMultiply(preCalculatedPacket, tone);

        ArrayHelper.packetMultiply(preCalculatedPacket, 0.3f);

        ArrayHelper.packetAdd(preCalculatedPacket, pilot);

        preCalculatedPacket = ArrayHelper.upsample(preCalculatedPacket, 1000000 / RDS_FREQUENCY);


        Log.d(LOGTAG, "finished preparing a packet with length=" + preCalculatedPacket.length);

        if (readFromAudioFile) {

            String extDir = Environment.getExternalStorageDirectory().getAbsolutePath();
            File file = new File(extDir + "/AnSiAn/audio.iq");

            try {
                this.audioFile = new BufferedInputStream(new FileInputStream(file));
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        } else {
            // init recorder to get audio from microphone
            int audioBufferSize = this.preCalculatedPacket.length / (this.sampleRate / AUDIO_SAMPLERATE);
            this.audioSource = new AudioSource(1000000, audioBufferSize);
            this.audioSource.startRecording();
        }

        Log.d(LOGTAG,"finished precalculation time="+System.currentTimeMillis());
    }


    @Override
    public void stop() {
        if(this.audioSource != null) {
            this.audioSource.stopRecording();
            this.audioSource = null;
        }


        if(this.audioFile != null){
            try {
                this.audioFile.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            this.audioFile = null;
        }
    }

    /**
     * Calculates a new Sample Packet
     * Either reads samples from the microphone or from an audio file and adds them to the
     * pre calculated RDS signal
     *
     * @return the sample packet containing the FM modulated signal which includes the RDS Signal
     */
    @Override
    public SamplePacket getNextSamplePacket() {
        Log.d(LOGTAG, "getNextSamplePacket()");

        if (this.audioSource == null && this.audioFile == null) return null;
        float[] upsampled;
        if (readFromAudioFile) {

            int requiredAudioSamples = this.preCalculatedPacket.length;
            upsampled = new float[requiredAudioSamples];
            byte[] samplesFromFile = new byte[requiredAudioSamples];
            try {
                Log.d(LOGTAG, "reading samples from file");
                if (this.audioFile.available() >= requiredAudioSamples) {
                    this.audioFile.read(samplesFromFile, 0, requiredAudioSamples);
                } else {
                    this.audioFile.close();
                    Log.d(LOGTAG, "end of file. finished");
                    this.audioFile = null;
                    return null;
                }

                for (int i = 0; i < samplesFromFile.length; i++) {
                    upsampled[i] = ((float) samplesFromFile[i]) / 128.0f;
                }

                Log.d(LOGTAG, "first_samples=" + upsampled[0] + "," + upsampled[1] + "," + upsampled[2] + "," + upsampled[3]);
                //this.packetNormalize(upsampled);

            } catch (IOException e) {
                e.printStackTrace();
            }

        } else {
            int requiredAudioSamples = this.preCalculatedPacket.length / (this.sampleRate / AUDIO_SAMPLERATE);
            Log.d(LOGTAG, "this.preCalculatedPacket.size()=" + this.preCalculatedPacket.length);
            Log.d(LOGTAG, "requiredAudioSamples=" + requiredAudioSamples);
            Log.d(LOGTAG, "AUDIO_SAMPLERATE=" + AUDIO_SAMPLERATE);
            Log.d(LOGTAG, "this.sampleRate=" + this.sampleRate);
            Log.d(LOGTAG, "now waiting for audio samples");
            upsampled = this.audioSource.getNextSamplesUpsampled();
            Log.d(LOGTAG, "finished waiting for audio samples");
            Log.d(LOGTAG, "upsampled_length=" + upsampled.length);
            //this.packetNormalize(upsampled);
        }
        // upsampled is a bit too long, so we throw away a few samples here
        float[] result = new float[preCalculatedPacket.length];
        System.arraycopy(upsampled, 0, result, 0, result.length);
        ArrayHelper.packetNormalize(upsampled);
        ArrayHelper.packetAdd(result, preCalculatedPacket);
        ArrayHelper.packetNormalize(result);


        /*
        SamplePacket fake = new SamplePacket(result.length);
        fake.setSize(result.length);
        System.arraycopy(preCalculatedPacket, 0, fake.getRe(), 0, result.length);
        if(true) return fake; */


        SamplePacket resultPacket = FM.fmmod(result, 1000000, 75000);
        Log.d(LOGTAG, "result.size()=" + resultPacket.size());
        return resultPacket;
    }

    private byte[] createBitsFromStationName(String stationName){
        if(stationName.length() > 8){
            stationName = stationName.substring(0, 9);
        }

        while(stationName.length() < 8){
            stationName = stationName.concat(" ");
        }


        // chose a random program identifier
        char PI = (char) (Math.random()*255);


        char[] bytes = new char[32];

        // GROUP 1
        bytes[0] = 0x10;
        bytes[1] = PI;

        bytes[2] = 0x9;
        bytes[3] = 0x48;

        bytes[4] = 0x10;
        bytes[5] = PI;

        bytes[6] = stationName.charAt(0);
        bytes[7] = stationName.charAt(1);

        // GROUP 2
        bytes[8] = 0x10;
        bytes[9] = PI;

        bytes[10] = 0x9;
        bytes[11] = 0x49;

        bytes[12] = 0x10;
        bytes[13] = PI;

        bytes[14] = stationName.charAt(2);
        bytes[15] = stationName.charAt(3);

        // GROUP 3
        bytes[16] = 0x10;
        bytes[17] = PI;

        bytes[18] = 0x9;
        bytes[19] = 0x4a;

        bytes[20] = 0x10;
        bytes[21] = PI;

        bytes[22] =  stationName.charAt(4);
        bytes[23] =  stationName.charAt(5);

        // GROUP 4

        bytes[24] = 0x10;
        bytes[25] = PI;

        bytes[26] = 0x9;
        bytes[27] = 0x4b;

        bytes[28] = 0x10;
        bytes[29] = PI;

        bytes[30] = stationName.charAt(6);
        bytes[31] =  stationName.charAt(7);


        // convert to bits

        byte[] bits = new byte[bytes.length*8];
        for(int i=0;i<bytes.length;i++){
            char c = 128;
            for(int j=0;j<8;j++){
                bits[i*8+j] = (byte) ((bytes[i]&c)>>(7  -j));
                c >>= 1;
            }
        }
        return bits;
    }




    /**
     * Calculates the check words and moves bits + check words into a new byte array
     *
     * @param bits containing data without check words, i.e. length should be divisible by 4*16 bits (i.e. only 0 or 1)
     * @return freshly allocated byte array containing data and checkwords (only 0 or 1)
     * (i.e. length = bits.length + 10*bits.length/16)
     */
    private byte[] calculateCheckwords(byte[] bits) {
        if ((bits.length % 4 * 16) != 0) {
            Log.e(LOGTAG, "group length must be divislbe by 4*16 bytes");
            throw new IllegalArgumentException("group length must be divislbe by 4*16 bytes");
        }
        // we need 10 extra bits per block (16 bits)
        // this is divisible by 8, because we have 4 blocks for each group
        int additionalBitsRequired = (bits.length / 16) * 10;
        byte[] result = new byte[(bits.length + additionalBitsRequired)];


        // calculate checkword (see specification)
        byte[][] G = {{0, 0, 0, 1, 1, 1, 0, 1, 1, 1},
                {1, 0, 1, 1, 1, 0, 0, 1, 1, 1},
                {1, 1, 1, 0, 1, 0, 1, 1, 1, 1},
                {1, 1, 0, 0, 0, 0, 1, 0, 1, 1},
                {1, 1, 0, 1, 0, 1, 1, 0, 0, 1},
                {1, 1, 0, 1, 1, 1, 0, 0, 0, 0},
                {0, 1, 1, 0, 1, 1, 1, 0, 0, 0},
                {0, 0, 1, 1, 0, 1, 1, 1, 0, 0},
                {0, 0, 0, 1, 1, 0, 1, 1, 1, 0},
                {0, 0, 0, 0, 1, 1, 0, 1, 1, 1},
                {1, 0, 1, 1, 0, 0, 0, 1, 1, 1},
                {1, 1, 1, 0, 1, 1, 1, 1, 1, 1},
                {1, 1, 0, 0, 0, 0, 0, 0, 1, 1},
                {1, 1, 0, 1, 0, 1, 1, 1, 0, 1},
                {1, 1, 0, 1, 1, 1, 0, 0, 1, 0},
                {0, 1, 1, 0, 1, 1, 1, 0, 0, 1}
        };

        byte[][] offsetWords = {{0, 0, 1, 1, 1, 1, 1, 1, 0, 0}, // A
                {0, 1, 1, 0, 0, 1, 1, 0, 0, 0},                 // B
                {0, 1, 0, 1, 1, 0, 1, 0, 0, 0},                 // C
                {0, 1, 1, 0, 1, 1, 0, 1, 0, 0}};                // D
        for (int blockIdx = 0; blockIdx < bits.length / 16; blockIdx++) {
            byte[] checksum = {0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
            for (int i = 0; i < checksum.length; i++) {
                for (int j = 0; j < G.length; j++)
                    checksum[i] += (byte) (bits[blockIdx*16 + j] * G[j][i]);
            }

            // add offset word
            byte[] offsetWord = offsetWords[blockIdx % 4];
            for (int i = 0; i < checksum.length; i++) {
                checksum[i] = (byte) (((checksum[i]) + offsetWord[i]) % 2);
            }

            // write to result buffer
            // write data first - copy 8 bits from idx*16 to blockidx*26
            System.arraycopy(bits, blockIdx * 16, result, blockIdx * 26, 16);
            // then write checksum
            System.arraycopy(checksum, 0, result, (blockIdx * 26) + 16, 10);
        }

        return result;
    }


    /**
     * Implements the differential encoding required for RDS.
     *
     * @param bits the bits (i.e. only 0 or 1)
     * @return encoded bits with same length of input
     */
    private byte[] calculateDifferentialEncoding(byte[] bits) {
        byte[] result = new byte[bits.length];
        result[0] = bits[0];
        for (int i = 1; i < bits.length; i++) {
            result[i] = (byte) (bits[i] ^ result[i - 1]);
        }
        return result;
    }

    /**
     * Takes the differentially encoded RDS bits, does the manchester encoding and upsamples the
     * signal to 1 MHz.
     *
     * @param differentialEncoded the bits  (i.e. only 0 or 1)
     * @return the manchester encoded signal at 1 MHz. -1 < signal < +1
     */
    private float[] calculateManchesterEncodingAndUpsampling(byte[] differentialEncoded) {
        int halfSamplesPerBit = (int) (RDS_FREQUENCY / BAUDRATE) / 2;
        float[] packet = new float[halfSamplesPerBit * 2 * differentialEncoded.length];


        // create templates for a zero and a one, such that the calculation is done only once
        float[] templateOne = new float[halfSamplesPerBit * 2];
        float[] templateZero = new float[halfSamplesPerBit * 2];
        for (int i = 0; i < templateOne.length; i++) {
            // 1 gets [1->-1]
            // 0 gets [-1->1]
            if (i < templateOne.length / 2) { // first half
                templateOne[i] = 0.999f;
                templateZero[i] = -0.999f;
            } else { // second half
                templateOne[i] = -0.999f;
                templateZero[i] = 0.999f;
            }
        }


        for (int i = 0; i < differentialEncoded.length; i++) {
            if (differentialEncoded[i] == 0) {
                System.arraycopy(templateZero, 0, packet, templateZero.length * i, templateZero.length);
            } else if (differentialEncoded[i] == 1) { // 1
                System.arraycopy(templateOne, 0, packet, templateOne.length * i, templateOne.length);
            } else {
                throw new IllegalArgumentException("expecting only bits");
            }
        }
        return packet;
    }

    /**
     *  Creates a sinus signal with amplitude 1.0 with a specified frequency and length.
     * @param frequency frequency of the sinus in Hz
     * @param sampleRate the sample rate of the signal
     * @param length length of the signal in samples
     * @return float array containing the signal -1 < signal < +1
     */
    private float[] createTone(float frequency, int sampleRate, int length) {
        float[] result = new float[length];
        for (int i = 0; i < length; i++) {
            float time = i / (float) sampleRate;
            result[i] = (float) Math.sin(2 * Math.PI * frequency * time) * 0.999f;
        }

        return result;
    }



}
