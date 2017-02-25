package de.tu.darmstadt.seemoo.ansian.model.transmission;

import android.os.Environment;
import android.util.Log;

import com.mantz_it.hackrf_android.Hackrf;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import de.greenrobot.event.EventBus;
import de.tu.darmstadt.seemoo.ansian.control.TxDataHandler;
import de.tu.darmstadt.seemoo.ansian.control.events.tx.TransmitEvent;
import de.tu.darmstadt.seemoo.ansian.control.events.tx.TransmitStatusEvent;

/**
 * same as {@link IQSink}, but samples are not sent to hackrf, but written in a file.
 *
 * @author Matthias Kannwischer
 */

public class FileSink extends IQSink {

    private static final String LOGTAG = "FileSink";
    private Hackrf hackrf;

    @Override
    public void run() {
        BlockingQueue<byte[]> source = TxDataHandler.getInstance().getTransmitIQQueue();
        String extDir = Environment.getExternalStorageDirectory().getAbsolutePath();
        File file = new File(extDir + "/AnSiAn/fakeiqsink.iq");
        file.getParentFile().mkdir();
        BufferedOutputStream outputStream = null;
        try {
            outputStream = new BufferedOutputStream(new FileOutputStream(file));

            while (true) {
                byte[] buffer = source.poll(2000, TimeUnit.MILLISECONDS);
                if (buffer == null) {
                    Log.d(LOGTAG, "no more packets left in queue. I'm done.");
                    break;
                } else {
                    Log.d(LOGTAG, "writing "+buffer.length+" bytes to file");
                    outputStream.write(buffer, 0, buffer.length);
                    this.hackrf.returnBufferToBufferPool(buffer);
                }
            }
            outputStream.close();
        } catch (InterruptedException e) {
            // this happens if the Thread is interrupted.
            // may be caused by a user pressing stop
            Log.d(LOGTAG, "interupted.");
        } catch (IOException e) {
            e.printStackTrace();
        }



        Log.d(LOGTAG, "finished to send");
        // notify UI
        EventBus.getDefault().post(new TransmitStatusEvent(TransmitEvent.State.TXOFF, TransmitEvent.Sender.TX));

    }

    @Override
    public void setHackrf(Hackrf hackrf) {
        super.setHackrf(hackrf);
        this.hackrf = hackrf;
    }

    @Override
    public boolean setup() {
        //do nothing
        return true;
    }
}
