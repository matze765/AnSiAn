package de.tu.darmstadt.seemoo.ansian.model.transmission;

import android.util.Log;

import com.mantz_it.hackrf_android.Hackrf;

import java.util.Arrays;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import de.tu.darmstadt.seemoo.ansian.control.TxDataHandler;
import de.tu.darmstadt.seemoo.ansian.model.SamplePacket;
import de.tu.darmstadt.seemoo.ansian.tools.Signed8BitIQConverter;

/**
 * Created by MATZE on 18.11.2016.
 */


/**
 * Part of the {@link de.tu.darmstadt.seemoo.ansian.model.transmission.TransmissionChain}.
 * It is responsible for taking SamplePackets from the TransmitPacketQueue, converting it to the
 * 8-bit signed I/Q samples and passing it to the TransmitIQQueue. The main challenge is that the
 * size of the SamplePackets can be different than and not divisible by the buffer size required
 * by the IQSink. After no packets arrive for 3000ms the IQConverter transmits the last buffer and
 * then kills itself.
 *
 * Currently this only supports 8-bit signed IQ Samples.
 * @author Matthias Kannwischer
 */
public class IQConverter implements Runnable {


    private static final String LOGTAG = "IQConverter";

    // defines the time the IQConverter waits for data to arrive in the queue
    // after this time the IQConverter transmit the last buffer and then kills itself
    private static final int TIMEOUT_MILLISECONDS = 3000;

    private IQSink iqsink;

    /**
     * @param iqsink requires IQSink because it needs to get buffers from the buffer pool.
     *               Using own buffers here is not recommended by the author of the HackRF driver.
     */
    public IQConverter(IQSink iqsink){
        this.iqsink = iqsink;
    }


    /**
     * Run method that needs to be executed in a separate thread. It is responsible for taking
     * SamplePackets from the TransmitPacketQueue, converting it to the 8-bit signed I/Q samples
     * and passing it to the TransmitIQQueue.The main challenge is that the size of the
     * SamplePackets can be different than and not divisible by the buffer size required
     * by the IQSink.
     * After no packets arrive for 3000ms the IQConverter transmits the last buffer and
     * then terminates.
     */
    @Override
    public void run() {
        // get queues
        BlockingQueue<byte[]> target = TxDataHandler.getInstance().getTransmitIQQueue();
        BlockingQueue<SamplePacket> source = TxDataHandler.getInstance().getTransmitPacketQueue();

        Signed8BitIQConverter converter = new Signed8BitIQConverter();
        try {
            int bufferSize = this.iqsink.getPacketSize();

            // byte array to memorize samples in between packets
            byte[] savedSamples  = null;


            while (true) {
                SamplePacket packet = source.poll(1, TimeUnit.SECONDS);

                Log.d(LOGTAG, "removed packet from transmitPacketQueue remainingCapacity="+source.remainingCapacity());
                if(packet == null){
                    Log.d(LOGTAG, "no more packets left in queue. I'm done.");
                    break;
                } else {
                    int packetSize = packet.size();

                    int currentOffset = 0;

                    // if there is an old incomplete buffer
                    if(savedSamples != null){
                        // check if enough samples arrived to fill a new buffer
                        if((savedSamples.length + packetSize*2)>= bufferSize) {
                            byte[] buffer = this.iqsink.getBufferFromBufferPool();
                            // copy old samples in new buffer
                            System.arraycopy(savedSamples, 0, buffer, 0, savedSamples.length);


                            Log.d(LOGTAG, "first_elements of buffer=[" + buffer[0] + "," + buffer[1] + "," + buffer[2] + "," + buffer[3] + "]");
                            // get enough new samples to fill the buffer
                            byte[] tmpbuffer = new byte[bufferSize - savedSamples.length];
                            converter.fillSamplePacketIntoByteBuffer(packet, tmpbuffer, currentOffset);

                            //copy new samples to end buffer
                            System.arraycopy(tmpbuffer, 0, buffer, savedSamples.length, tmpbuffer.length);

                            // set correct offset
                            currentOffset = tmpbuffer.length / 2;

                            savedSamples = null;
                            // put buffer in TransmitIQQueue
                            target.put(buffer);


                        } else {
                            // not enough samples, memorize them for later
                            byte[] oldSaved = savedSamples;
                            byte[] tmpbuffer = new byte[packetSize*2];

                            // convert and save to tmp buffer
                            converter.fillSamplePacketIntoByteBuffer(packet, tmpbuffer, currentOffset);
                            currentOffset = packetSize;

                            // create new buffer which can hold all samples
                            savedSamples = new byte[oldSaved.length+tmpbuffer.length];

                            // copy over data
                            System.arraycopy(oldSaved, 0, savedSamples, 0, oldSaved.length);
                            System.arraycopy(tmpbuffer, 0, savedSamples, oldSaved.length, tmpbuffer.length);
                            // nothing more to do
                        }
                    }

                    // looping through the packet as long as it can fill an entire buffer
                    while(currentOffset + (bufferSize/2) <= packetSize){
                        byte[] buffer = this.iqsink.getBufferFromBufferPool();
                        converter.fillSamplePacketIntoByteBuffer(packet, buffer, currentOffset);
                        target.put(buffer);
                        currentOffset += bufferSize/2;
                    }


                    // save the frames left
                    if((packetSize-currentOffset) != 0) {
                        savedSamples = new byte[(packetSize - currentOffset) * 2];
                        converter.fillSamplePacketIntoByteBuffer(packet, savedSamples, currentOffset);
                        Log.d(LOGTAG, "savedSamples.length="+savedSamples.length);
                        Log.d(LOGTAG, "first_elements of savedSamples=["+savedSamples[0]+","+savedSamples[1]+","+savedSamples[2]+","+savedSamples[3]+"]");
                    }
                }
            }

            // no more packets left.
            // need to send the remaining samples
            if(savedSamples != null){
                byte[] buffer = this.iqsink.getBufferFromBufferPool();
                // copy old samples in new buffer
                System.arraycopy(savedSamples, 0, buffer, 0, savedSamples.length);
                // fill the rest with zeros
                Arrays.fill(buffer, savedSamples.length, buffer.length, (byte) 0);
                target.put(buffer);
            }
        } catch(InterruptedException e){
            // this happens if the Thread is interrupted.
            // may be caused by a user pressing stop
            Log.d(LOGTAG, "interupted.");
        }

        Log.d(LOGTAG, "finished converting packet from float to byte.");
    }
}
