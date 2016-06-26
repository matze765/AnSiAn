package de.tu.darmstadt.seemoo.ansian;

import android.app.Application;
import android.test.ApplicationTestCase;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;

import de.tu.darmstadt.seemoo.ansian.model.SamplePacket;
import de.tu.darmstadt.seemoo.ansian.model.demodulation.RDS;
import de.tu.darmstadt.seemoo.ansian.tools.Signed12BitIQConverter;
import de.tu.darmstadt.seemoo.ansian.tools.Unsigned8BitIQConverter;

public class Signed12BitConverterTest extends ApplicationTestCase<Application> {
    public Signed12BitConverterTest() {
        super(Application.class);
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();
    }

    public void testRDSCheckword() {
                    //    0   -1       -2048   2047
                    // => 0   -0.0004          -1     1
        byte[] buffer = {0,0, -1,-16,  -128,0, 127,-16};
        Signed12BitIQConverter converter = new Signed12BitIQConverter();
        SamplePacket samplePacket = new SamplePacket(2);
        converter.fillPacketIntoSamplePacket(buffer, samplePacket);

        System.out.println("Sample 1:  " + samplePacket.re(0) + "  ,  " + samplePacket.im(0));
        System.out.println("Sample 2:  " + samplePacket.re(1) + "  ,  " + samplePacket.im(1));
        assertEquals(0, samplePacket.re(0), 0.000001f);
        assertEquals(-0.000488281, samplePacket.im(0), 0.000001f);
        assertEquals(-1, samplePacket.re(1), 0.000001f);
        assertEquals(0.9995, samplePacket.im(1), 0.0001f);
    }

}
