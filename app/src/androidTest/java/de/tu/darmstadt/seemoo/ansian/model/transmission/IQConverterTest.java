package de.tu.darmstadt.seemoo.ansian.model.transmission;

import android.app.Application;
import android.test.ApplicationTestCase;
import android.util.Log;

import junit.framework.TestCase;

import java.util.Arrays;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import de.tu.darmstadt.seemoo.ansian.control.TxDataHandler;
import de.tu.darmstadt.seemoo.ansian.model.SamplePacket;

/**
 * Created by MATZE on 19.11.2016.
 */
public class IQConverterTest extends ApplicationTestCase<Application> {

    private final String LOGTAG = "IQConverterTest";
    public IQConverterTest() {
        super(Application.class);
    }

    public void testSmallPacket() throws InterruptedException {

        // get queues
        BlockingQueue<byte[]> transmitIQQueue = TxDataHandler.getInstance().getTransmitIQQueue();
        BlockingQueue<SamplePacket> transmitPacketQueue = TxDataHandler.getInstance().getTransmitPacketQueue();

        // make sure they are empty
        transmitIQQueue.clear();
        transmitPacketQueue.clear();


        // create iq converter - depends on IQSink
        // we create a fake iqsink to mock the hackrf calls
        IQSinkFake iqSinkFake = new IQSinkFake();
        IQConverter iqConverter = new IQConverter(iqSinkFake);

        // we start the iqconverter thread, but no iq sink thread
        Thread iqConverterThread = new Thread(iqConverter);
        iqConverterThread.start();

        // create 3 short fake packets
        SamplePacket p1 = createPacket(iqSinkFake.getPacketSize()/2, 0.0f, 0.1f);
        SamplePacket p2 = createPacket(iqSinkFake.getPacketSize()/2, 0.2f, 0.3f);
        SamplePacket p3 = createPacket(iqSinkFake.getPacketSize()/2, -0.2f, -0.3f);

        // enqueue these packets for processing
        transmitPacketQueue.put(p1);
        transmitPacketQueue.put(p2);
        transmitPacketQueue.put(p3);

        // validate each buffer

        byte[] b1 = transmitIQQueue.poll(1, TimeUnit.SECONDS);
        byte[] b2 = transmitIQQueue.poll(1, TimeUnit.SECONDS);
        byte[] b3 = transmitIQQueue.poll(1, TimeUnit.SECONDS);
        byte[] b4 = transmitIQQueue.poll(100, TimeUnit.MILLISECONDS);


        // b1-b3 should exist, but b4 not
        assertNotNull(b1);
        assertNotNull(b2);
        assertNotNull(b3);
        assertNull(b4);

        Log.d(LOGTAG,"b1="+ Arrays.toString(b1));
        Log.d(LOGTAG,"b2="+ Arrays.toString(b2));
        Log.d(LOGTAG,"b3="+ Arrays.toString(b3));

        // validate
        for(int i=0;i<b1.length;i++){
            if(i%2==0){ // real part
                assertEquals(0, b1[i]);
                assertEquals(25, b2[i]);
                assertEquals(-25, b3[i]);
            } else { // imag part
                assertEquals(12, b1[i]);
                assertEquals(38, b2[i]);
                assertEquals(-38, b3[i]);
            }

        }

        // clean up
        iqConverterThread.interrupt();

    }

    public void testPacketFragmentation() throws InterruptedException {
        // get queues
        BlockingQueue<byte[]> transmitIQQueue = TxDataHandler.getInstance().getTransmitIQQueue();
        BlockingQueue<SamplePacket> transmitPacketQueue = TxDataHandler.getInstance().getTransmitPacketQueue();

        // make sure they are empty
        transmitIQQueue.clear();
        transmitPacketQueue.clear();


        // create iq converter - depends on IQSink
        // we create a fake iqsink to mock the hackrf calls
        IQSinkFake iqSinkFake = new IQSinkFake();
        IQConverter iqConverter = new IQConverter(iqSinkFake);

        // we start the iqconverter thread, but no iq sink thread
        Thread iqConverterThread = new Thread(iqConverter);
        iqConverterThread.start();

        // create 2 packets, in total they will result in 3 hackrf buffers
        int bufferLength = iqSinkFake.getPacketSize();
        int packetLength = (bufferLength*3)/4;
        SamplePacket p1 = createPacket(packetLength, 0.5f, 0.7f);
        SamplePacket p2 = createPacket(packetLength, -0.5f, -0.7f);

        // enqueue packets
        transmitPacketQueue.put(p1);
        transmitPacketQueue.put(p2);

        // retrieve results.
        byte[] b1 = transmitIQQueue.poll(1, TimeUnit.SECONDS);
        byte[] b2 = transmitIQQueue.poll(1, TimeUnit.SECONDS);
        byte[] b3 = transmitIQQueue.poll(1, TimeUnit.SECONDS);
        byte[] b4 = transmitIQQueue.poll(100, TimeUnit.MILLISECONDS);

        // b1-b3 should exist, but b4 not
        assertNotNull(b1);
        assertNotNull(b2);
        assertNotNull(b3);
        assertNull(b4);

        Log.d(LOGTAG,"b1="+ Arrays.toString(b1));
        Log.d(LOGTAG,"b2="+ Arrays.toString(b2));
        Log.d(LOGTAG,"b3="+ Arrays.toString(b3));

        // validate
        for(int i=0;i<bufferLength;i++){
            if(i%2==0){ // real
                assertEquals(64, b1[i]);
                assertEquals(-64, b3[i]);
                if(i<bufferLength/2){ // first half of b2
                    assertEquals(64, b2[i]);
                } else { // second half of b2
                    assertEquals(-64, b2[i]);
                }
            } else{ // imag
                assertEquals(89, b1[i]);
                assertEquals(-89, b3[i]);
                if(i<bufferLength/2){ // first half of b2
                    assertEquals(89, b2[i]);
                } else { // second half of b2
                    assertEquals(-89, b2[i]);
                }
            }
        }


        // clean up
        iqConverterThread.interrupt();
    }


    public void testVeryShortPacket() throws  InterruptedException {
        // get queues
        BlockingQueue<byte[]> transmitIQQueue = TxDataHandler.getInstance().getTransmitIQQueue();
        BlockingQueue<SamplePacket> transmitPacketQueue = TxDataHandler.getInstance().getTransmitPacketQueue();

        // make sure they are empty
        transmitIQQueue.clear();
        transmitPacketQueue.clear();


        // create iq converter - depends on IQSink
        // we create a fake iqsink to mock the hackrf calls
        IQSinkFake iqSinkFake = new IQSinkFake();
        IQConverter iqConverter = new IQConverter(iqSinkFake);

        // we start the iqconverter thread, but no iq sink thread
        Thread iqConverterThread = new Thread(iqConverter);
        iqConverterThread.start();

        // create one packet that is shorter than buffer
        SamplePacket p1 = createPacket(1000, 0.3f, 0.6f);

        // enqueue
        transmitPacketQueue.put(p1);

        //get results
        byte[] b1 = transmitIQQueue.poll(10000, TimeUnit.MILLISECONDS);
        byte[] b2 = transmitIQQueue.poll(100, TimeUnit.MILLISECONDS);

        assertNotNull(b1);
        assertNull(b2);

        // validate
        for(int i=0;i<b1.length;i++){
            if(i < 2000){
                if(i%2 == 0) { // real
                    assertEquals(38, b1[i]);
                } else { // imag
                    assertEquals(76, b1[i]);
                }
            } else {
                assertEquals(0, b1[i]);
            }
        }

        // clean up
        iqConverterThread.interrupt();
    }

    public void testTwoVeryShortPacket() throws  InterruptedException {
        // get queues
        BlockingQueue<byte[]> transmitIQQueue = TxDataHandler.getInstance().getTransmitIQQueue();
        BlockingQueue<SamplePacket> transmitPacketQueue = TxDataHandler.getInstance().getTransmitPacketQueue();

        // make sure they are empty
        transmitIQQueue.clear();
        transmitPacketQueue.clear();


        // create iq converter - depends on IQSink
        // we create a fake iqsink to mock the hackrf calls
        IQSinkFake iqSinkFake = new IQSinkFake();
        IQConverter iqConverter = new IQConverter(iqSinkFake);

        // we start the iqconverter thread, but no iq sink thread
        Thread iqConverterThread = new Thread(iqConverter);
        iqConverterThread.start();

        // create one packet that is shorter than buffer
        SamplePacket p1 = createPacket(1000, 0.3f, 0.6f);
        SamplePacket p2 = createPacket(1000, -0.3f, -0.6f);

        // enqueue
        transmitPacketQueue.put(p1);
        transmitPacketQueue.put(p2);

        //get results
        byte[] b1 = transmitIQQueue.poll(10000, TimeUnit.MILLISECONDS);
        byte[] b2 = transmitIQQueue.poll(100, TimeUnit.MILLISECONDS);

        assertNotNull(b1);
        assertNull(b2);

        // validate
        for(int i=0;i<b1.length;i++){
            if(i < 2000){
                if(i%2 == 0) { // real
                    assertEquals(38, b1[i]);
                } else { // imag
                    assertEquals(76, b1[i]);
                }
            } else if (i< 4000){
                if(i%2 == 0) { // real
                    assertEquals(-38, b1[i]);
                } else { // imag
                    assertEquals(-76, b1[i]);
                }
            } else {
                assertEquals(0, b1[i]);
            }
        }


        // clean up
        iqConverterThread.interrupt();
    }

    private SamplePacket createPacket(int packetSize, float reElement, float imElement) {
        SamplePacket packet = new SamplePacket(packetSize);
        packet.setSize(packetSize);
        for(int i=0;i<packetSize;i++){
            packet.getRe()[i] = reElement;
            packet.getIm()[i] = imElement;
        }
        return packet;
    }


    private class IQSinkFake extends IQSink{
        public IQSinkFake(){
            super(null);
        }

        public byte[] getBufferFromBufferPool() {
            return new byte[this.getPacketSize()];
        }

        public int getPacketSize() {
            return 1024*16;
        }


    }
}