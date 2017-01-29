package de.tu.darmstadt.seemoo.ansian.model.transmission;

import android.util.Log;

import com.mantz_it.hackrf_android.Hackrf;
import com.mantz_it.hackrf_android.HackrfUsbException;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import de.greenrobot.event.EventBus;
import de.tu.darmstadt.seemoo.ansian.control.TxDataHandler;
import de.tu.darmstadt.seemoo.ansian.control.events.morse.TransmitEvent;
import de.tu.darmstadt.seemoo.ansian.model.preferences.Preferences;

/**
 * IQSink for handling buffers over to the HackRF. It's main task is to initialize the HackRF and
 * then wait for frames to arrive in the TransmitIQQueue. It needs to be run in a separate Thread
 * and then polls the queue and passes them to the HackRF. After no frames arrive for 1000 ms the
 * IQSinks terminates - this is sensible since the HackRF does the same.
 *
 * This class should be extended if other hardware should be supported.
 *
 * @author Matthias Kannwischer
 */

public class IQSink implements Runnable {
    private static final String LOGTAG = "IQSink";


    /**
     * Defines the time the IQSink waits for data to arrive in the queue.
     * After this time the IQSink kills itself
     */
    private static final int TIMEOUT_MILLISECONDS = 5000;

    private Hackrf hackrf = null;
    private BlockingQueue<byte[]> hackRFSink = null;


    /**
     * Sets the required parameters of the HackRF like sampleRate, frequency, gain, etc.
     * The values are taken from the application preferences.
     * Also notifies the front end that we are starting transmission now by calling startTX() on
     * the HackRF.
     *
     * @return true if HackRF parameters are successfully set, false otherwise
     */
    public boolean setup() {
        // get the preferences
        // TODO: don't do this in here. This makes this module very dependent of the app
        int sampRate = Preferences.MISC_PREFERENCE.getSend_sampleRate();
        int frequency = Preferences.MISC_PREFERENCE.getSend_frequency();
        boolean amp = Preferences.MISC_PREFERENCE.isSend_amplifier();
        boolean antennaPower = Preferences.MISC_PREFERENCE.isSend_antennaPower();
        int vgaGain = Preferences.MISC_PREFERENCE.getSend_vgaGain();

        // vgaGain is still a value from 0-100; scale it to the right range:
        vgaGain = (vgaGain * 47) / 100;

        int basebandFilterWidth = Hackrf.computeBasebandFilterBandwidth((int) (0.75 * sampRate));
        int i = 0;
        try {
            Log.d(LOGTAG, "Setting Sample Rate to " + sampRate + " Sps ... ");
            hackrf.setSampleRate(sampRate, 1);
            Log.d(LOGTAG, "ok.\nSetting Frequency to " + frequency + " Hz ... ");
            hackrf.setFrequency(frequency);
            Log.d(LOGTAG, "ok.\nSetting Baseband Filter Bandwidth to " + basebandFilterWidth + " Hz ... ");
            hackrf.setBasebandFilterBandwidth(basebandFilterWidth);
            Log.d(LOGTAG, "ok.\nSetting TX VGA Gain to " + vgaGain + " ... ");
            hackrf.setTxVGAGain(vgaGain);
            Log.d(LOGTAG, "ok.\nSetting Amplifier to " + amp + " ... ");
            hackrf.setAmp(amp);
            Log.d(LOGTAG, "ok.\nSetting Antenna Power to " + antennaPower + " ... ");
            hackrf.setAntennaPower(antennaPower);
            Log.d(LOGTAG, "ok.\n\n");

            EventBus.getDefault().post(new TransmitEvent(TransmitEvent.State.TXACTIVE, TransmitEvent.Sender.TX));
            return true;
        } catch (HackrfUsbException e) {
            Log.d(LOGTAG, "Error (USB)!\n");
            return false;
        }

    }

    /**
     * Run method that needs to be executed in a separate thread. The setup() method needs to be
     * called beforehand to init the HackRF. If the thread is launched and the HackRF is initialized
     * correctly this waits for frames to arrive in the TransmitIQQueue and pushes them to the
     * HackRF queue.
     *
     * After no frames arrive for 1000 ms the thread terminates.
     */
    @Override
    public void run() {
        BlockingQueue<byte[]> source = TxDataHandler.getInstance().getTransmitIQQueue();
        try {
            while (true) {
                byte[] buffer = source.poll(TIMEOUT_MILLISECONDS, TimeUnit.MILLISECONDS);
                if (buffer == null) {
                    Log.d(LOGTAG, "no more packets left in queue. I'm done.");
                    break;
                } else {
                    if(this.hackRFSink == null) {
                        this.hackRFSink = this.hackrf.startTX();
                    }

                    this.hackRFSink.put(buffer);
                }
            }
        } catch (InterruptedException e) {
            // this happens if the Thread is interrupted.
            // may be caused by a user pressing stop
            Log.d(LOGTAG, "interupted.");
        } catch (HackrfUsbException e) {
            e.printStackTrace();
        }
        Log.d(LOGTAG, "finished to send");
        // notify UI
        EventBus.getDefault().post(new TransmitEvent(TransmitEvent.State.TXOFF, TransmitEvent.Sender.TX));
    }

    public byte[] getBufferFromBufferPool() {
        return this.hackrf.getBufferFromBufferPool();
    }

    public int getPacketSize() {
        return this.hackrf.getPacketSize();
    }

    public void setHackrf(Hackrf hackrf) {
        this.hackrf = hackrf;
    }
}

