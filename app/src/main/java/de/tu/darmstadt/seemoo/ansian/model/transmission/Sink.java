package de.tu.darmstadt.seemoo.ansian.model.transmission;

import android.content.Context;
import android.os.Environment;
import android.util.Log;

import com.mantz_it.hackrf_android.Hackrf;
import com.mantz_it.hackrf_android.HackrfCallbackInterface;
import com.mantz_it.hackrf_android.HackrfUsbException;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;

import de.greenrobot.event.EventBus;
import de.greenrobot.event.Subscribe;
import de.tu.darmstadt.seemoo.ansian.MainActivity;
import de.tu.darmstadt.seemoo.ansian.control.events.morse.TransmitEvent;
import de.tu.darmstadt.seemoo.ansian.model.preferences.Preferences;

/**
 * Created by Max on 23.08.2016.
 */

public class Sink implements HackrfCallbackInterface, Runnable {

    private static final String LOGTAG = "Sink";

    private boolean stopRequested;
    private Hackrf hackrf;

    public Sink() {
        EventBus.getDefault().register(this);
        stopRequested = false;
    }

    @Override
    public void onHackrfError(String s) {
        Log.d(LOGTAG, "HackRF Error: " + s);
    }

    @Override
    public void onHackrfReady(Hackrf hackrf) {
        Log.d(LOGTAG, "HackRF ready!");
        this.hackrf = hackrf;
        new Thread(this).start();
    }

    @Subscribe
    public void onEvent(final TransmitEvent event) {

        if (!event.isTransmitting()) {
            this.stopRequested = true;
            if (hackrf != null) {
                try {
                    hackrf.stop();
                } catch (HackrfUsbException e) {
                    Log.d(LOGTAG, "Error when stopping HackRF");
                    e.printStackTrace();
                }
            }
            return;
        }

        open();

    }

    @Override
    public void run() {
        this.stopRequested = false;
        transmit();
    }

    private void finishedTransmitting() {
        EventBus.getDefault().post(new TransmitEvent(false, TransmitEvent.Sender.TX));
    }

    private void transmit() {

        int sampRate = Preferences.MISC_PREFERENCE.getSend_sampleRate();
        int frequency = Preferences.MISC_PREFERENCE.getSend_frequency();
        boolean amp = Preferences.MISC_PREFERENCE.isSend_amplifier();
        boolean antennaPower = Preferences.MISC_PREFERENCE.isSend_antennaPower();
        int vgaGain = Preferences.MISC_PREFERENCE.getSend_vgaGain();
        String filename = Preferences.MISC_PREFERENCE.getSend_filename();

        // vgaGain is still a value from 0-100; scale it to the right range:
        vgaGain = (vgaGain * 47) / 100;

        int basebandFilterWidth = Hackrf.computeBasebandFilterBandwidth((int) (0.75 * sampRate));
        int i = 0;
        long lastTransceiverPacketCounter = 0;
        long lastTransceivingTime = 0;


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

            // Check if external memory is available:
            if (!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
                Log.d(LOGTAG, "External Media Storage not available.\n\n");
                return;
            }

            // Open a file ...
            File file = new File(filename);
            Log.d(LOGTAG, "Reading samples from " + file.getAbsolutePath() + "\n");
            if (!file.exists()) {
                Log.d(LOGTAG, "Error: File does not exist!");
                return;
            }

            // ... and open it with a buffered input stream
            BufferedInputStream bufferedInputStream = new BufferedInputStream(new FileInputStream(file));

            // Start Transmitting:
            Log.d(LOGTAG, "Start Transmitting... \n");
            ArrayBlockingQueue<byte[]> queue = hackrf.startTX();

            // Run until user hits the 'Stop' button
            while (!this.stopRequested) {
                i++;    // only for statistics

                // IMPORTANT: We don't allocate the buffer for a packet ourself. We use the getBufferFromBufferPool()
                // method of the hackrf instance! This might give us an already allocated buffer from the buffer pool
                // and save a lot of time and memory! You will get a java.lang.OutOfMemoryError if you don't do that
                // trust me ;) If no buffer is available in the pool, this method will automatically allocate a buffer
                // of the correct size!
                byte[] packet = hackrf.getBufferFromBufferPool();

                // Read one packet from the file:
                if (bufferedInputStream.read(packet, 0, packet.length) != packet.length) {
                    Log.d(LOGTAG, "Reached End of File. Stop.\n");
                    break;
                }

                // Put the packet into the queue:
                if (queue.offer(packet, 1000, TimeUnit.MILLISECONDS) == false) {
                    Log.d(LOGTAG, "Error: Queue is full. Stop transmitting.\n");
                    break;
                }

                // print statistics
                if (i % 1000 == 0) {
                    long bytes = (hackrf.getTransceiverPacketCounter() - lastTransceiverPacketCounter) * hackrf.getPacketSize();
                    double time = (hackrf.getTransceivingTime() - lastTransceivingTime) / 1000.0;
                    Log.d(LOGTAG, String.format("Current Transfer Rate: %4.1f MB/s\n", (bytes / time) / 1000000.0));
                    lastTransceiverPacketCounter = hackrf.getTransceiverPacketCounter();
                    lastTransceivingTime = hackrf.getTransceivingTime();
                }
            }

            // After loop ended: close the file and print more statistics:
            bufferedInputStream.close();
            Log.d(LOGTAG, String.format("Finished! (Average Transfer Rate: %4.1f MB/s)\n",
                    hackrf.getAverageTransceiveRate() / 1000000.0));
            Log.d(LOGTAG, String.format("Transmitted %d packets (each %d Bytes) in %5.3f Seconds.\n\n",
                    hackrf.getTransceiverPacketCounter(), hackrf.getPacketSize(),
                    hackrf.getTransceivingTime() / 1000.0));
            finishedTransmitting();
        } catch (HackrfUsbException e) {
            Log.d(LOGTAG, "Error (USB)!\n");
            finishedTransmitting();
        } catch (IOException e) {
            Log.d(LOGTAG, "Error (File IO)!\n");
            finishedTransmitting();
        } catch (InterruptedException e) {
            Log.d(LOGTAG, "Error (Queue interrupted)!\n");
            finishedTransmitting();
        }
    }


    private boolean open() {
        int queueSize = 1000000;
        Context context = MainActivity.instance;
        // Initialize the HackRF (i.e. open the USB device, which requires the
        // user to give permissions)
        Log.d(LOGTAG, "Initializing HackRF");
        return Hackrf.initHackrf(context, this, queueSize);
    }
}
