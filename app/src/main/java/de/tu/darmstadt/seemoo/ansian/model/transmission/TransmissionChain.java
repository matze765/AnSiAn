package de.tu.darmstadt.seemoo.ansian.model.transmission;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import com.mantz_it.hackrf_android.Hackrf;
import com.mantz_it.hackrf_android.HackrfCallbackInterface;

import de.greenrobot.event.EventBus;
import de.greenrobot.event.Subscribe;
import de.tu.darmstadt.seemoo.ansian.MainActivity;
import de.tu.darmstadt.seemoo.ansian.control.TxDataHandler;
import de.tu.darmstadt.seemoo.ansian.control.events.tx.TransmitEvent;
import de.tu.darmstadt.seemoo.ansian.control.events.tx.TransmitStatusEvent;
import de.tu.darmstadt.seemoo.ansian.gui.misc.MyToast;

/**
 * Representation of the entire transmission chain. It is responsible for listening to TransmitEvents
 * by the front end and then start up the transmission chain (i.e. separate threads for each part
 * of the chain) or stopping the chain (i.e. killing all threads and clean up the queues).
 *
 * @author Matthias Kannwischer
 */

public class TransmissionChain implements HackrfCallbackInterface {
    private static final String LOGTAG = "TransmissionChain";

    /**
     * HackRF transmit queue size in bytes
     */
    private static final int QUEUE_SIZE = 8000000;

    private Thread modulatorThread;
    private Thread iqConverterThread;
    private Thread iqSinkThread;
    private IQSink iqSink;
    private Modulator modulator;


    public TransmissionChain() {
        EventBus.getDefault().register(this);
    }


    /**
     * called by the EventBus if TransmitEvents occur.
     * Mainly called by the front end when the user starts or stops a transmission
     *
     * @param event
     */
    @Subscribe
    public void onEvent(final TransmitEvent event) {
        Log.d(LOGTAG, "onEvent state=" + event.getState());
        switch (event.getState()) {
            case TXOFF:
                stop();
                break;
            case MODULATION:
                /*
                if(event.getSender() == TransmitEvent.Sender.TX) break;
                EventBus.getDefault().post(new TransmitEvent(TransmitEvent.State.MODULATION, TransmitEvent.Sender.TX));
                */
                TxDataHandler.getInstance().clearAll();
                Context context = MainActivity.instance;
                iqSink = new IQSink(event.getTransmissionSampleRate(), event.getTransmissionFrequency(),
                        event.isAmplifier(), event.isAntennaPowerPort(), event.getVgaGain());
                //iqSink = new FileSink();
                modulator = new Modulator(iqSink,event, event.getTransmissionSampleRate());

                // Initialize the HackRF (i.e. open the USB device, which requires the
                // user to give permissions)
                Log.d(LOGTAG, "Initializing HackRF");
                // init HackRF - will call back when it is initialized and ready
                if (!open()) {
                    EventBus.getDefault().post(new TransmitStatusEvent(TransmitEvent.State.TXOFF, TransmitEvent.Sender.TX));
                    MyToast.makeText("Cannot open HackRF", Toast.LENGTH_LONG);
                }
                break;
            default:
                break;
        }
    }

    /**
     * Callback {@link HackrfCallbackInterface} function that is called when HackRF is ready.
     *
     * @param hackrf
     */
    @Override
    public void onHackrfReady(Hackrf hackrf) {
        Log.d(LOGTAG, "HackRF ready!");
        start(hackrf);
    }

    /**
     * Callback {@link HackrfCallbackInterface} function that is called if an error occurs while
     * HackRF is initialized.
     * @param s
     */
    @Override
    public void onHackrfError(String s) {
        //TODO: handle error properly
        Log.e(LOGTAG, "HackRF Error: " + s);
    }


    /**
     * Starts up the Transmission chain and launches all threads.
     * @param hackrf
     */
    private void start(Hackrf hackrf) {
        // clear all queues
        TxDataHandler.getInstance().clearAll();


        Log.d(LOGTAG, "starting up transmission chain");

        // we need an iqSink instance to access the buffer pool from Modulator and IQConverter
        //IQSink iqSink = new FileSink(hackrf);

        iqSink.setHackrf(hackrf);
        iqSink.setup();
        // create threads
        this.modulatorThread = new Thread(modulator);
        this.iqConverterThread = new Thread(new IQConverter(iqSink));
        this.iqSinkThread = new Thread(iqSink);


        // start threads, order does not matter because of blocking queues
        this.modulatorThread.start();
        this.iqConverterThread.start();
        this.iqSinkThread.start();
    }

    /**
     * Tries to kill all threads. Each thread needs to handle the interrupt properly.
     */
    private void stop() {
        Log.d(LOGTAG, "clearing up transmission chain. killing all parts");
        // kill modulator
        if (this.modulatorThread != null) {
            this.modulatorThread.interrupt();
            this.modulatorThread = null;
        }

        if (this.iqConverterThread != null) {
            this.iqConverterThread.interrupt();
            this.iqConverterThread = null;
        }

        if (this.iqSinkThread != null) {
            this.iqSinkThread.interrupt();
            this.iqSinkThread = null;
        }
    }

    /**
     * Tries to initialize the HackRF. {@link Hackrf#initHackrf(Context, HackrfCallbackInterface, int)}
     * @return false if no Hackrf could be found
     */
    private boolean open() {
        Context context = MainActivity.instance;
        // Initialize the HackRF (i.e. open the USB device, which requires the
        // user to give permissions)
        Log.d(LOGTAG, "Initializing HackRF");
        return Hackrf.initHackrf(context, this, QUEUE_SIZE);
    }


}
