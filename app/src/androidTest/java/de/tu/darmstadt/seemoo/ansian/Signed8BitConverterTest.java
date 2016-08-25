package de.tu.darmstadt.seemoo.ansian;

import android.app.Application;
import android.test.ApplicationTestCase;

import de.tu.darmstadt.seemoo.ansian.model.SamplePacket;
import de.tu.darmstadt.seemoo.ansian.tools.Signed8BitIQConverter;

/**
 * Created by dennis on 8/25/16.
 */
public class Signed8BitConverterTest extends ApplicationTestCase<Application> {

    public Signed8BitConverterTest() {
        super(Application.class);
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();
    }

    public void testFillSamplePacketIntoByteBuffer() {
        byte[] buffer = new byte[8];
        SamplePacket samplePacket = new SamplePacket(4);
        float[] re = samplePacket.getRe();
        float[] im = samplePacket.getIm();
        re[0] = 0;
        re[1] = -1;
        re[2] = 0.999f;
        re[3] = 0.5f;
        im[0] = 0.008f;
        im[1] = 0.016f;
        im[2] = 0.024f;
        im[3] = 0.032f;
        samplePacket.setSize(4);
        Signed8BitIQConverter converter = new Signed8BitIQConverter();
        converter.fillSamplePacketIntoByteBuffer(samplePacket, buffer);

        System.out.printf("Buffer: %3d %3d %3d %3d %3d %3d %3d %3d\n", buffer[0], buffer[1], buffer[2], buffer[3], buffer[4], buffer[5], buffer[6], buffer[7]);
        assertEquals(0, buffer[0]);
        assertEquals(1, buffer[1]);
        assertEquals(-128, buffer[2]);
        assertEquals(2, buffer[3]);
        assertEquals(127, buffer[4]);
        assertEquals(3, buffer[5]);
        assertEquals(64, buffer[6]);
        assertEquals(4, buffer[7]);
    }
}
