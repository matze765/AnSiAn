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

import de.tu.darmstadt.seemoo.ansian.model.SamplePacket;
import de.tu.darmstadt.seemoo.ansian.model.filter.FirFilter;

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

    private static final int MAX_PACKETS = 1;

    private int sampleRate;

    private float[] preCalculatedPacket;
    private int packetCtr;
    private AudioRecord recorder = null;
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
        this.readFromAudioFile = fileAudioSource;


        this.sampleRate = sampleRate;

        // generate groups
        byte[] groups = this.createBitsFromStationName(stationName);

        Log.d(LOGTAG, "groups="+Arrays.toString(groups));


        // calculate check words
        byte[] groupWithCheckwords = this.calculateCheckwords(groups);

        groupWithCheckwords = this.repeat(groupWithCheckwords, 4);


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
        FirFilter filter1 = FirFilter.createLowPass(1, 1, 1000000, cutoff, transWidth, attenuation);
        filter1.filterReal(packetManchester, filteredPacket, 0, preCalculatedPacket.length);

        preCalculatedPacket = filteredPacket.getRe();
        this.packetNormalize(preCalculatedPacket);


        // create pilot and tone

        float[] pilot = this.createTone(PILOT_FREQUENCY, this.sampleRate, this.preCalculatedPacket.length);
        float[] tone = this.createTone(TONE_FREQUENCY, this.sampleRate, this.preCalculatedPacket.length);

        this.packetMultiply(preCalculatedPacket, tone);

        this.packetMultiply(preCalculatedPacket, 0.3f);

        this.packetAdd(preCalculatedPacket, pilot);


        Log.d(LOGTAG, "finished preparing a packet with length=" + preCalculatedPacket.length);
        this.packetCtr = 0;

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


            int RECORDER_CHANNELS = AudioFormat.CHANNEL_IN_MONO;
            int RECORDER_AUDIO_ENCODING = AudioFormat.ENCODING_PCM_FLOAT;

            Log.d(LOGTAG, "min buffer size=" + AudioRecord.getMinBufferSize(AUDIO_SAMPLERATE, RECORDER_CHANNELS, RECORDER_AUDIO_ENCODING));

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

        if (this.recorder == null && this.audioFile == null) return null;
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


            if (packetCtr++ >= MAX_PACKETS) {
                Log.d(LOGTAG, "stopping to send");
                this.recorder.release();
                return null;
            }


            int requiredAudioSamples = this.preCalculatedPacket.length / (this.sampleRate / AUDIO_SAMPLERATE);
            Log.d(LOGTAG, "this.preCalculatedPacket.size()=" + this.preCalculatedPacket.length);
            Log.d(LOGTAG, "requiredAudioSamples=" + requiredAudioSamples);
            Log.d(LOGTAG, "AUDIO_SAMPLERATE=" + AUDIO_SAMPLERATE);
            Log.d(LOGTAG, "this.sampleRate=" + this.sampleRate);
            float[] buffer = new float[requiredAudioSamples];

            this.recorder.read(buffer, 0, buffer.length, AudioRecord.READ_BLOCKING);
            upsampled = this.upsample(buffer, (int) Math.ceil((float) this.sampleRate / AUDIO_SAMPLERATE));


            Log.d(LOGTAG, "upsampled_length=" + upsampled.length);

            //this.packetNormalize(upsampled);
        }
        // upsampled is a bit too long, so we throw away a few samples here
        float[] result = new float[preCalculatedPacket.length];
        System.arraycopy(upsampled, 0, result, 0, result.length);
        this.packetNormalize(upsampled);
        this.packetAdd(result, preCalculatedPacket);
        this.packetNormalize(result);


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
     * Repeats an arbitrary byte array.
     *
     * @param array input array that will be repeated
     * @param repeatCount number of repetitions (0= no repetition)
     * @return the repeated array (length = array.length * (repeatCount+1)
     */
    private byte[] repeat(byte[] array, int repeatCount) {
        repeatCount++;
        byte[] repeated = new byte[array.length * repeatCount];
        for (int i = 0; i < repeatCount; i++) {
            System.arraycopy(array, 0, repeated, i * array.length, array.length);
        }

        return repeated;
    }

    /**
     * Implements the up sampling by a integer factor
     * @param buffer the real signal
     * @param factor integer factor
     * @return upsampled signal (length = buffer.length*factor)
     */
    private float[] upsample(float[] buffer, int factor) {
        Log.d(LOGTAG, "upsampling buffer from " + buffer.length + " floats to " + factor * buffer.length + " floats");
        float[] result = new float[buffer.length * factor];
        for (int i = 0; i < buffer.length; i++) {
            for (int j = 0; j < factor; j++) {
                result[i * 23 + j] = buffer[i];
            }
        }
        return result;
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
        int halfSamplesPerBit = (int) (this.sampleRate / BAUDRATE) / 2;
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
    /**
     * Multiplies two float arrays. They are expected to have equal length.
     *
     * @param firstOperandAndResult the first operand will also be used as result
     * @param secondOperand second operand of the addition
     */
    private void packetMultiply(float[] firstOperandAndResult, float[] secondOperand) {
        for (int i = 0; i < firstOperandAndResult.length; i++) {
            firstOperandAndResult[i] = firstOperandAndResult[i] * secondOperand[i];
        }
    }

    /**
     * Multiplies each element of a float array with a constant float.
     *
     * @param firstOperandAndResult the first operand will also be used as result
     * @param constant that will be multiplied with each element of the array
     */
    private void packetMultiply(float[] firstOperandAndResult, float constant) {
        for (int i = 0; i < firstOperandAndResult.length; i++) {
            firstOperandAndResult[i] = firstOperandAndResult[i] * constant;
        }
    }

    /**
     * Adds two float arrays. They are expected to have equal length.
     *
     * @param firstOperandAndResult the first operand will also be used as result
     * @param secondOperand second operand of the addition
     */
    private void packetAdd(float[] firstOperandAndResult, float[] secondOperand) {
        for (int i = 0; i < firstOperandAndResult.length; i++) {
            firstOperandAndResult[i] = firstOperandAndResult[i] + secondOperand[i];
        }
    }

    /**
     * Normalizes a packet, such that the highest peak is slightly under 1.0
     *
     * @param operandAndResult source and target of the operation
     */
    private void packetNormalize(float[] operandAndResult) {

        // get max
        float max = Float.MIN_VALUE;
        for (int i = 0; i < operandAndResult.length; i++) {
            if (operandAndResult[i] > max) {
                max = operandAndResult[i];
            }

        }

        // increase max a bit to prevent 1.0
        max += 0.001;

        Log.d(LOGTAG, "normalization_factor=1/" + max);


        // normalize
        for (int i = 0; i < operandAndResult.length; i++) {
            operandAndResult[i] = operandAndResult[i] / max;
        }
    }


}
