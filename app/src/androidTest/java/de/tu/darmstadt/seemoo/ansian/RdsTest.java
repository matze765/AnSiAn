package de.tu.darmstadt.seemoo.ansian;

import android.app.Application;
import android.provider.Settings;
import android.test.ApplicationTestCase;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;

import de.tu.darmstadt.seemoo.ansian.model.SamplePacket;
import de.tu.darmstadt.seemoo.ansian.model.demodulation.RDS;
import de.tu.darmstadt.seemoo.ansian.tools.Unsigned8BitIQConverter;

public class RdsTest extends ApplicationTestCase<Application> {
    public RdsTest() {
        super(Application.class);
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();
    }

    public void testRDSCheckword() {
        byte[] blocka       = {0,0,0,1,0,0,1,1,0,1,1,0,0,0,1,0,1,1,0,0,0,1,0,0,0,1};
        byte[] blockinvalid = {1,0,0,1,0,0,1,1,0,1,1,0,0,0,1,0,1,1,0,0,0,1,0,0,0,1};
        RDS rds = new RDS();
        int ret = rds.checkBits(blocka, 0);
        assertEquals(0, ret);
        ret = rds.checkBits(blockinvalid, 0);
        assertEquals(-1, ret);
    }

    public void testRDS() {
        RDS rds = new RDS();
        SamplePacket[] packets = readRtlSdrIqFile("/storage/sdcard0/Download/rds_baseband_62500sps.iq", 512, 62500, 57000);

        System.out.printf("Demodulating packets...\n");
        long ts = System.currentTimeMillis();
        for (SamplePacket packet : packets) {
            rds.demodulate(packet, null);
        }
        long te = System.currentTimeMillis();
        System.out.printf("Demodulating packets took %d ms\n", te-ts);
    }

    public SamplePacket[] readRtlSdrIqFile(String path, int packetsize, int samplerate, long frequency) {
        Unsigned8BitIQConverter converter = new Unsigned8BitIQConverter();
        byte[] packet = new byte[packetsize*2];
        ArrayList<SamplePacket> samplePackets = new ArrayList<SamplePacket>();

        converter.setSampleRate(samplerate);
        converter.setFrequency(frequency);

        try {
            File file = new File(path);
            BufferedInputStream bufferedInputStream = new BufferedInputStream(new FileInputStream(file));

            while (bufferedInputStream.read(packet, 0, packet.length) == packet.length) {
                SamplePacket samplePacket = new SamplePacket(packetsize);
                converter.fillPacketIntoSamplePacket(packet, samplePacket);
                samplePackets.add(samplePacket);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return samplePackets.toArray(new SamplePacket[samplePackets.size()]);
    }

}
