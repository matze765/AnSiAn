package de.tu.darmstadt.seemoo.ansian.model.modulation;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.net.Uri;

import java.io.File;
import java.io.IOException;

import de.tu.darmstadt.seemoo.ansian.model.SamplePacket;
import de.tu.darmstadt.seemoo.ansian.tools.ArrayHelper;

/**
 * Created by MATZE on 22.02.2017.
 */

public class SSTV  extends  Modulation{



    public enum SSTV_TYPE {ROBOT_SSTV_BW_120};

    private final static int INTERNAL_SAMPLING_RATE = 50000;

    // parameters of ROBOT SSTV BW 120
    // c.f. https://en.wikipedia.org/wiki/Slow-scan_television
    private final static float SYNC_FREQUENCY = 1200;
    private final static float BLACK_FREQUENCY = 1500;
    private final static float WHITE_FREQUENCY = 2300;
    private final static int NUM_LINES = 120;
    private final static int NUM_PIXELS_PER_LINE = 256;
    private final static float LINE_SYNC_SECONDS = 0.005f;
    private final static float FRAME_SYNC_SECONDS = 0.03f;
    private final static float LINE_DURATION_SECONDS = (1/15.0f)-LINE_SYNC_SECONDS;

    private int samplingRate;
    private boolean repeat;
    private int[] imageData;
    private int currentLineIdx;

    /**
     * @param outputSamplingRate sampling rate of the generated SamplePackets (usually 1MHz),
     *                           intermediate modulation is done at lower frequencies for better performance
     * @param image image that should be transmitted
     * @param repeat should the transmission be repeated, if false: only send once
     * @param crop crop the input image to required 256x120, if false: image is scaled to 256x120
     */
    public SSTV(int outputSamplingRate, Bitmap image, boolean repeat, boolean crop, SSTV_TYPE type){
        this.samplingRate = outputSamplingRate;
        this.repeat = repeat;
        // -1 -> start with frame sync
        this.currentLineIdx = -1;
        try {
            this.imageData = readImageAndConvertToGrayScale(image, crop);
        } catch (IOException e) {
            this.imageData = null;
            e.printStackTrace();
        }


    }
    @Override
    public SamplePacket getNextSamplePacket() {
        if(this.imageData == null) return null;

        float[] samples;
        if(this.currentLineIdx == -1){ // frame sync
            int frameSyncSamples = Math.round(FRAME_SYNC_SECONDS*INTERNAL_SAMPLING_RATE);
            samples = new float[frameSyncSamples];
            insertConstant(samples, SYNC_FREQUENCY, 0, frameSyncSamples);
        } else {
            int lineSyncSamples = Math.round(LINE_SYNC_SECONDS*INTERNAL_SAMPLING_RATE);
            int samplesPerPixel = Math.round(INTERNAL_SAMPLING_RATE*LINE_DURATION_SECONDS/NUM_PIXELS_PER_LINE);
            int totalLength = lineSyncSamples+NUM_PIXELS_PER_LINE*samplesPerPixel;
            samples = new float[totalLength];
            // insert pixel values
            for(int i=0;i<NUM_PIXELS_PER_LINE;i++) {
                // 0 <= pixel <= 255, 0= black, 255 = white
                int pixel = this.imageData[this.currentLineIdx*NUM_PIXELS_PER_LINE+i];

                float interpolatedFrequency = BLACK_FREQUENCY + (WHITE_FREQUENCY-BLACK_FREQUENCY)* (pixel/255.0f);
                insertConstant(samples, interpolatedFrequency,i*samplesPerPixel, samplesPerPixel);
            }
            // insert line sync
            insertConstant(samples, SYNC_FREQUENCY, totalLength-lineSyncSamples, lineSyncSamples);
        }

        // normalize;  0 <= sample <= 1
        ArrayHelper.packetMultiply(samples, 1/WHITE_FREQUENCY);

        // upsample
        if(this.samplingRate != INTERNAL_SAMPLING_RATE){
            samples = ArrayHelper.upsample(samples, Math.round(this.samplingRate/ (float)INTERNAL_SAMPLING_RATE));
        }

        SamplePacket fm = FM.fmmod(samples, this.samplingRate, WHITE_FREQUENCY);

        this.currentLineIdx++;
        if(this.currentLineIdx == NUM_LINES){
            if(this.repeat){
                // start over with frame sync next time
                this.currentLineIdx = -1;
            } else {
                // we are done, return null next time
                this.imageData = null;
            }

        }
        return fm;
    }

    private void insertConstant(float[] array, float value, int offset, int count){
        for(int i=0;i<count;i++){
            array[offset+i] = value;
        }
    }
    private int[] readImageAndConvertToGrayScale(Bitmap source, boolean crop) throws IOException {
        Bitmap scaled;
        // scale or crop
        if(crop){ // crop
            scaled = Bitmap.createBitmap(source, 0,0,NUM_PIXELS_PER_LINE, NUM_LINES);
        } else { // scale
            scaled = Bitmap.createScaledBitmap(source, NUM_PIXELS_PER_LINE, NUM_LINES, true);
        }


        Bitmap gray = Bitmap.createBitmap(NUM_PIXELS_PER_LINE, NUM_LINES, Bitmap.Config.RGB_565);
        Canvas canvas = new Canvas(gray);
        Paint paint = new Paint();
        ColorMatrix colorMatrix = new ColorMatrix();
        colorMatrix.setSaturation(0);
        ColorMatrixColorFilter f = new ColorMatrixColorFilter(colorMatrix);
        paint.setColorFilter(f);
        canvas.drawBitmap(scaled, 0, 0, paint);

        int[] data = new int[NUM_PIXELS_PER_LINE*NUM_LINES];

        for(int y=0;y<NUM_LINES;y++) {
            for (int x = 0; x < NUM_PIXELS_PER_LINE; x++) {
                data[y*NUM_PIXELS_PER_LINE+x] = Color.red(gray.getPixel(x,y));
            }
        }


        return data;
    }


    @Override
    public void stop() {
        this.imageData = null;
    }
}