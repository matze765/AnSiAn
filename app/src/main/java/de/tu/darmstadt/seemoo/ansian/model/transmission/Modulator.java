package de.tu.darmstadt.seemoo.ansian.model.transmission;

import android.util.Log;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.concurrent.BlockingQueue;

import de.greenrobot.event.EventBus;
import de.tu.darmstadt.seemoo.ansian.control.TxDataHandler;
import de.tu.darmstadt.seemoo.ansian.control.events.BandwidthEvent;
import de.tu.darmstadt.seemoo.ansian.control.events.morse.TransmitEvent;
import de.tu.darmstadt.seemoo.ansian.model.SamplePacket;
import de.tu.darmstadt.seemoo.ansian.model.modulation.FM;
import de.tu.darmstadt.seemoo.ansian.model.modulation.Modulation;
import de.tu.darmstadt.seemoo.ansian.model.modulation.Morse;
import de.tu.darmstadt.seemoo.ansian.model.modulation.PSK31;
import de.tu.darmstadt.seemoo.ansian.model.modulation.RDS;
import de.tu.darmstadt.seemoo.ansian.model.preferences.Preferences;


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
    public Modulator(IQSink iqSink) {
        this.iqSink = iqSink;

        // get preferences
        Modulation.TxMode mode = Preferences.MISC_PREFERENCE.getSend_txMode();
        String payloadString = Preferences.MISC_PREFERENCE.getSend_payloadText();
        filename = Preferences.MISC_PREFERENCE.getSend_filename();
        int sampleRate = Preferences.MISC_PREFERENCE.getSend_sampleRate();
        int rdsAudioSource = Preferences.MISC_PREFERENCE.getRds_audio_source();





        this.modulationInstance = null;


        // determine correct modulation
        switch (mode) {
            case MORSE:
                modulationInstance = new Morse(payloadString, Preferences.MISC_PREFERENCE.getMorse_wpm(), sampleRate);
                break;
            case PSK31:
                modulationInstance = new PSK31(payloadString, sampleRate);
                break;
            case RDS:
                modulationInstance = new RDS(payloadString, sampleRate, rdsAudioSource==0);
                break;
            case FM:
                modulationInstance = new FM(sampleRate);
                break;
            case RAWIQ:
                // special case
                // we need to skip the IQConverter step and directly push them to the iq queue
                // but that is done in the other thread, so do nothing here
                modulationInstance = null;
                break;

            default:
                Log.e(LOGTAG, "modulation: invalid mode: " + mode + "; abort!");
                EventBus.getDefault().post(new TransmitEvent(TransmitEvent.State.TXOFF, TransmitEvent.Sender.TXCHAIN));
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


        Log.d(LOGTAG, "starting to modulate "+modulationInstance);

        if(modulationInstance != null) {
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
            }
        } else {
            Log.d(LOGTAG, "reading IQ file.");
            try {
                BufferedInputStream bufferedInputStream = new BufferedInputStream(new FileInputStream(filename));
                BlockingQueue<byte[]> transmitIQQueue = TxDataHandler.getInstance().getTransmitIQQueue();
                while(true) {
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
