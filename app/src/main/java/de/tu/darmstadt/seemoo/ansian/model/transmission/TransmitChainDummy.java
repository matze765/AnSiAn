package de.tu.darmstadt.seemoo.ansian.model.transmission;

import android.os.Environment;
import android.util.Log;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import de.greenrobot.event.EventBus;
import de.greenrobot.event.Subscribe;
import de.tu.darmstadt.seemoo.ansian.control.events.morse.TransmitEvent;
import de.tu.darmstadt.seemoo.ansian.model.SamplePacket;
import de.tu.darmstadt.seemoo.ansian.model.modulation.Modulation;
import de.tu.darmstadt.seemoo.ansian.model.modulation.Morse;
import de.tu.darmstadt.seemoo.ansian.model.preferences.Preferences;
import de.tu.darmstadt.seemoo.ansian.tools.Signed8BitIQConverter;

/**
 * Created by dennis on 8/25/16.
 */
public class TransmitChainDummy implements Runnable {

    private static final String LOGTAG = "TransmitChainDummy";
    private static final int BUFFERSIZE = 6000000;
    private static final String TMPFILENAME = ".tmp_tx_file.iq";

    private boolean stopRequested;
    private byte[] buffer = new byte[BUFFERSIZE];

    public TransmitChainDummy() {
        EventBus.getDefault().register(this);
        stopRequested = false;
    }

    @Subscribe
    public void onEvent(final TransmitEvent event) {
        switch (event.getState()) {
            case TXOFF:
                this.stopRequested = true;
                break;
            case MODULATION:
                new Thread(this).start();
                break;
            default:
                break;
        }
    }

    @Override
    public void run() {
        this.stopRequested = false;
        modulation();
    }

    private void modulation() {
        String extDir = Environment.getExternalStorageDirectory().getAbsolutePath();
        Modulation modulationInstance;
        Modulation.TxMode mode = Preferences.MISC_PREFERENCE.getSend_txMode();
        String payloadString = Preferences.MISC_PREFERENCE.getSend_payloadText();
        String filename = Preferences.MISC_PREFERENCE.getSend_filename();

        switch (mode) {
            case MORSE:
                modulationInstance = new Morse(payloadString, Preferences.MISC_PREFERENCE.getMorse_wpm(), 1000000);
                break;
            case RAWIQ:
                TransmitEvent event = new TransmitEvent(TransmitEvent.State.TXACTIVE, TransmitEvent.Sender.TXCHAIN);
                event.setIqFile(filename);
                EventBus.getDefault().post(event);
                return;
            default:
                Log.e(LOGTAG, "modulation: invalid mode: " + mode + "; abort!");
                EventBus.getDefault().post(new TransmitEvent(TransmitEvent.State.TXOFF, TransmitEvent.Sender.TXCHAIN));
                return;
        }

        Signed8BitIQConverter converter = new Signed8BitIQConverter();
        SamplePacket samplePacket;
        int counter = 0;
        try {
            File file = new File(extDir + "/AnSiAn/" + TMPFILENAME);
            file.getParentFile().mkdir();    // Create directory if it does not yet exist
            BufferedOutputStream outputStream = new BufferedOutputStream(new FileOutputStream(file));
            while ((samplePacket = modulationInstance.getNextSamplePacket()) != null) {
                int bufferLen = converter.fillSamplePacketIntoByteBuffer(samplePacket, buffer);
                // write to file:
                outputStream.write(buffer, 0, bufferLen);
                counter++;
                Log.d(LOGTAG, "modulation: [" + counter + "] got sample pkt of size " + samplePacket.size() + " and filled it in a buffer of len " + bufferLen);
            }
            outputStream.close();
        } catch (IOException e) {
            Log.e(LOGTAG, "modulation: Error while writting file: " + e.getMessage());
            EventBus.getDefault().post(new TransmitEvent(TransmitEvent.State.TXOFF, TransmitEvent.Sender.TXCHAIN));
            return;
        }
        Log.d(LOGTAG, "modulation: Done after " + counter + " packets!");


        TransmitEvent event = new TransmitEvent(TransmitEvent.State.TXACTIVE, TransmitEvent.Sender.TXCHAIN);
        event.setIqFile(extDir + "/AnSiAn/" + TMPFILENAME);
        EventBus.getDefault().post(event);
    }
}
