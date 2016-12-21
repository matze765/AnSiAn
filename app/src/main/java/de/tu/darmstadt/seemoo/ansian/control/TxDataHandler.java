package de.tu.darmstadt.seemoo.ansian.control;

import com.mantz_it.hackrf_android.Hackrf;

import java.util.concurrent.ArrayBlockingQueue;

import de.tu.darmstadt.seemoo.ansian.model.SamplePacket;

/**
 * Similar to {@link DataHandler}. Used to provide global access to the queues of the transmission
 * chain. Currently we have mainly two queues:
 *  1. TransmitPacketQueue: hold SamplePacket that have been created by the {@link de.tu.darmstadt.seemoo.ansian.model.transmission.Modulator}
 *  and need to be processed by the {@link de.tu.darmstadt.seemoo.ansian.model.transmission.IQConverter} next.
 *  2. TransmitIQQueue: hold byte[] buffers (provided by the Hackrf BufferPool) that contain I/Q samples
 *  by the {@link de.tu.darmstadt.seemoo.ansian.model.transmission.IQConverter} and wait to be passed on to
 *  the {@link Hackrf} by the {@link de.tu.darmstadt.seemoo.ansian.model.transmission.IQSink}.
 * @author Matthias Kannwischer
 */

public class TxDataHandler {
    private static final int TRANSMIT_PACKET_QUEUE_SIZE = 5;
    private static final int TRANSMIT_IQ_QUEUE_SIZE = 200;
    private static TxDataHandler instance;


    private ArrayBlockingQueue<SamplePacket> transmitPacketQueue;
    private ArrayBlockingQueue<byte[]> transmitIQQueue;

    public TxDataHandler(){
        // create queues
        this.transmitPacketQueue = new ArrayBlockingQueue<>(TRANSMIT_PACKET_QUEUE_SIZE);
        this.transmitIQQueue = new ArrayBlockingQueue<>(TRANSMIT_IQ_QUEUE_SIZE);
    }

    /**
     * @return existing instance if one exists, creates new otherwise
     */
    public static TxDataHandler getInstance() {
        if (instance == null)
            instance = new TxDataHandler();
        return instance;
    }


    /**
     * removes all elements from all queues
     */
    public void clearAll() {
        this.transmitIQQueue.clear();
        this.transmitPacketQueue.clear();
    }

    public ArrayBlockingQueue<SamplePacket> getTransmitPacketQueue() {
        return transmitPacketQueue;
    }

    public ArrayBlockingQueue<byte[]> getTransmitIQQueue() {
        return transmitIQQueue;
    }
}
