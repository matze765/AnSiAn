package de.tu.darmstadt.seemoo.ansian;

import android.app.Application;
import android.test.ApplicationTestCase;

import de.tu.darmstadt.seemoo.ansian.model.SamplePacket;
import de.tu.darmstadt.seemoo.ansian.model.demodulation.PSK31;

public class PSK31Test extends ApplicationTestCase<Application> {

    public PSK31Test() {
        super(Application.class);
    }

    public void testPSK31() {
        PSK31 psk31 = new PSK31();
        SamplePacket[] packets = RdsTest.readRtlSdrIqFile("/storage/sdcard0/Download/psk31_baseband_clean_7812_5sps.iq", 512/4, (int) 7812.5, 57000);

        System.out.printf("Demodulating PSK31...\n");
        long ts = System.currentTimeMillis();
        for (SamplePacket packet : packets) {
            psk31.demodulate(packet, null);
        }
        long te = System.currentTimeMillis();
        System.out.printf("Demodulating packets took %d ms\n", te-ts);
    }
}
