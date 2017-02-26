package de.tu.darmstadt.seemoo.ansian.model.transmission;

import android.util.Log;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.concurrent.BlockingQueue;

import de.greenrobot.event.EventBus;
import de.tu.darmstadt.seemoo.ansian.control.TxDataHandler;
import de.tu.darmstadt.seemoo.ansian.control.events.tx.TransmitEvent;
import de.tu.darmstadt.seemoo.ansian.control.events.tx.TransmitStatusEvent;
import de.tu.darmstadt.seemoo.ansian.control.events.tx.data.morse.MorseTransmitEvent;
import de.tu.darmstadt.seemoo.ansian.control.events.tx.data.psk31.PSK31TransmitEvent;
import de.tu.darmstadt.seemoo.ansian.control.events.tx.data.rds.RDSTransmitEvent;
import de.tu.darmstadt.seemoo.ansian.control.events.tx.image.sstv.SSTVTransmitEvent;
import de.tu.darmstadt.seemoo.ansian.control.events.tx.rawiq.RawIQTransmitEvent;
import de.tu.darmstadt.seemoo.ansian.control.events.tx.speech.fm.FMTransmitEvent;
import de.tu.darmstadt.seemoo.ansian.control.events.tx.speech.lsb.LSBTransmitEvent;
import de.tu.darmstadt.seemoo.ansian.control.events.tx.speech.usb.USBTransmitEvent;
import de.tu.darmstadt.seemoo.ansian.model.SamplePacket;
import de.tu.darmstadt.seemoo.ansian.model.modulation.FM;
import de.tu.darmstadt.seemoo.ansian.model.modulation.LSB;
import de.tu.darmstadt.seemoo.ansian.model.modulation.Modulation;
import de.tu.darmstadt.seemoo.ansian.model.modulation.Morse;
import de.tu.darmstadt.seemoo.ansian.model.modulation.PSK31;
import de.tu.darmstadt.seemoo.ansian.model.modulation.RDS;
import de.tu.darmstadt.seemoo.ansian.model.modulation.SSTV;
import de.tu.darmstadt.seemoo.ansian.model.modulation.USB;
import de.tu.darmstadt.seemoo.ansian.model.preferences.Preferences;

import static android.R.attr.mode;
import static de.tu.darmstadt.seemoo.ansian.model.modulation.Modulation.TxMode.RAWIQ;


/**
 * Modulator that is responsible for getting new SamplePackets {@link Modulation#getNextSamplePacket()}
 * and adding them to the TransmissionPacketQueue when they are required by the TransmissionChain.
 *
 * @author Matthias Kannwischer
 */
public class Modulator implements Runnable {
    private static final String LOGTAG = "Modulator";
    private IQSink iqSink;
    Modulation modulationInstance;
    String filename;

    /**
     * @param iqSink requires IQSink because it needs to get buffers from the buffer pool.
     *               Using own buffers here is not recommended by the author of the HackRF driver.
     */
    public Modulator(IQSink iqSink, TransmitEvent event, int sampleRate) {
        this.iqSink = iqSink;

        this.modulationInstance = null;
        if (event instanceof MorseTransmitEvent) {
            MorseTransmitEvent mte = (MorseTransmitEvent) event;
            this.modulationInstance = new Morse(mte.getPayload(), mte.getWPM(), sampleRate, mte.getMorseFrequency());
        } else if (event instanceof PSK31TransmitEvent) {
            PSK31TransmitEvent pte = (PSK31TransmitEvent) event;
            this.modulationInstance = new PSK31(pte.getPayload(), sampleRate);
        } else if (event instanceof RDSTransmitEvent) {
            RDSTransmitEvent rte = (RDSTransmitEvent) event;
            this.modulationInstance = new RDS(rte.getPayload(), sampleRate, rte.getFileAudioSource());
        } else if (event instanceof FMTransmitEvent) {
            FMTransmitEvent fte = (FMTransmitEvent) event;
            this.modulationInstance = new FM(sampleRate);
        } else if (event instanceof USBTransmitEvent) {
            USBTransmitEvent ute = (USBTransmitEvent) event;
            this.modulationInstance = new USB(sampleRate, ute.getFilterBandwidth());
        } else if (event instanceof LSBTransmitEvent) {
            LSBTransmitEvent lte = (LSBTransmitEvent) event;
            this.modulationInstance = new LSB(sampleRate, lte.getFilterBandwidth());
        } else if (event instanceof RawIQTransmitEvent) {
            // special case
            // we need to skip the IQConverter step and directly push them to the iq queue
            // but that is done in the other thread, so do nothing here
            RawIQTransmitEvent rte = (RawIQTransmitEvent) event;
            modulationInstance = null;
            filename = rte.getTransmitFileName();
        } else if (event instanceof SSTVTransmitEvent) {
            SSTVTransmitEvent ste = (SSTVTransmitEvent) event;
            this.modulationInstance = new SSTV(sampleRate, ste.getImage(), ste.isRepeat(), ste.isCrop(), ste.getType());
        } else {
            Log.e(LOGTAG, "modulation: invalid mode: " + mode + "; abort!");
            EventBus.getDefault().post(new TransmitStatusEvent(TransmitEvent.State.TXOFF, TransmitEvent.Sender.TXCHAIN));
            return;
        }
    }


    /**
     * Run method that needs to be executed in a separate thread. It repeatedly calls
     * {@link Modulation#getNextSamplePacket()} for the correct Modulation scheme if there is space
     * available in the TransmissionPacketQueue. If no more packets are available (i.e. {@link Modulation#getNextSamplePacket()}
     * returns null) the thread is terminanted.
     */
    @Override
    public void run() {
        BlockingQueue<SamplePacket> transmitQueue = TxDataHandler.getInstance().getTransmitPacketQueue();


        Log.d(LOGTAG, "starting to modulate " + modulationInstance);

        if (modulationInstance != null) {
            // main modulation loop. get and enqueue new packets until Modulation does not return more
            // packets or user interrupts the modulation
            try {
                SamplePacket samplePacket;
                while ((samplePacket = modulationInstance.getNextSamplePacket()) != null) {
                    transmitQueue.put(samplePacket);
                    Log.d(LOGTAG, "added packet to transmitPacketQueue remainingCapacity=" + transmitQueue.remainingCapacity());

                }
            } catch (InterruptedException e) {
                // this happens if the Thread is interrupted.
                // may be caused by a user pressing stop
                Log.d(LOGTAG, "interupted.");
                modulationInstance.stop();
            }
        } else {
            Log.d(LOGTAG, "reading IQ file.");
            try {
                BufferedInputStream bufferedInputStream = new BufferedInputStream(new FileInputStream(filename));
                BlockingQueue<byte[]> transmitIQQueue = TxDataHandler.getInstance().getTransmitIQQueue();
                while (true) {
                    byte[] packet = this.iqSink.getBufferFromBufferPool();
                    if (bufferedInputStream.read(packet, 0, packet.length) != packet.length) {
                        Log.d(LOGTAG, "Reached End of File. Stop.\n");
                        return;
                    }
                    transmitIQQueue.put(packet);
                }
            } catch (IOException e) {
                Log.e(LOGTAG, "IQFile not found " + filename);
            } catch (InterruptedException e) {
                Log.e(LOGTAG, "unable to put packet in transmitIQQueue");
            }
        }
        Log.d(LOGTAG, "finished to modulate");
    }
}
