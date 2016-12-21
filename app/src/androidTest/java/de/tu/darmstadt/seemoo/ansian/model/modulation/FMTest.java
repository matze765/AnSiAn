package de.tu.darmstadt.seemoo.ansian.model.modulation;

import junit.framework.TestCase;

import java.util.Arrays;

import de.tu.darmstadt.seemoo.ansian.model.SamplePacket;

/**
 * @author Matthias Kannwischer
 */
public class FMTest extends TestCase {
    public void testCumSum(){
        float[] test = new float[]{1.0f, 1.2f, 1.3f, 1.4f, 1.5f};
        float[] expectedResult = new float[]{1.0f, 2.2f, 3.5f, 4.9f, 6.4f};
        float[] result = FM.cumsum(test);
        assertEquals(test.length, result.length);
        assertEquals(expectedResult.length, result.length);
        for(int i=0;i<expectedResult.length;i++){
            assertEquals(Math.round(expectedResult[i]*100), Math.round(result[i]*100));
        }

    }

    public void testCumSum2(){
        float[] test = new float[]{0.5878f,0.9511f, 0.9511f, 0.5878f, 0.0000f,-0.5878f,-0.9511f,-0.9511f,-0.5878f,-0.0000f, 0.5878f, 0.9511f,0.9511f, 0.5878f, 0.0000f,-0.5878f,-0.9511f,-0.9511f,-0.5878f,-0.0000f};
        float[] expectedResult = new float[]{0.5878f, 1.5388f, 2.4899f, 3.0777f, 3.0777f, 2.4899f, 1.5388f, 0.5878f,0f,-0.0000f, 0.5878f, 1.5388f, 2.4899f, 3.0777f, 3.0777f, 2.4899f, 1.5388f, 0.5878f,0f,-0.0000f};
        float[] result = FM.cumsum(test);
        assertEquals(test.length, result.length);
        assertEquals(expectedResult.length, result.length);
        for(int i=0;i<expectedResult.length;i++){
            assertEquals(Math.round(expectedResult[i]*100), Math.round(result[i]*100));
        }
    }

    public void testFM(){
        float[] test = new float[]{1.0f, 1.2f, 1.3f, 1.4f, 1.5f};
        float[] expectedResult = new float[]{ 0.891007f, 0.509041f, -0.078459f, -0.673013f,  -0.992115f};
        float fs = 1000000;
        float freqdev = 75000;
        SamplePacket packet = FM.fmmod(test,fs, freqdev);
        float[] result = packet.getRe();
        assertEquals(test.length, result.length);
        assertEquals(expectedResult.length, result.length);
        for(int i=0;i<expectedResult.length;i++){
            assertEquals(Math.round(expectedResult[i]*100), Math.round(result[i]*100));
        }
    }

    public void testFM2(){
        float[] test = new float[]{0.5878f,0.9511f, 0.9511f, 0.5878f, 0.0000f,-0.5878f,-0.9511f,-0.9511f,-0.5878f,-0.0000f, 0.5878f, 0.9511f,0.9511f, 0.5878f, 0.0000f,-0.5878f,-0.9511f,-0.9511f,-0.5878f,-0.0000f};
        float[] expectedResult = new float[]{0.96677f,0.77957f,0.45794f,0.21546f,0.21546f,0.45794f,0.77957f,0.96677f,1.00000f,1.00000f,0.96677f,0.77957f,0.45794f,0.21546f,0.21546f,0.45794f,0.77957f,0.96677f,1.00000f,1.00000f};
        float fs = 100;
        float freqdev = 7;
        SamplePacket packet =FM.fmmod(test, fs, freqdev);
        float[] result = packet.getRe();
        System.out.println(Arrays.toString(result));
        assertEquals(test.length, result.length);
        assertEquals(expectedResult.length, result.length);
        for(int i=0;i<expectedResult.length;i++){
            assertEquals(Math.round(expectedResult[i]*100), Math.round(result[i]*100));
        }
    }
    /*
    public void testFM3(){
        float[] test = new float[]{}

    } */

}