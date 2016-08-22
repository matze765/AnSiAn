package de.tu.darmstadt.seemoo.ansian.model;

import android.os.Environment;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

import de.greenrobot.event.EventBus;
import de.greenrobot.event.Subscribe;
import de.tu.darmstadt.seemoo.ansian.control.events.DemodInfoEvent;
import de.tu.darmstadt.seemoo.ansian.model.preferences.DemodPreference;
import de.tu.darmstadt.seemoo.ansian.model.preferences.Preferences;

/**
 * Logs text displayed on DemodulationInfoView by demodulators to a logfile
 * <p/>
 * Created by Max on 22.08.2016.
 */

public class Logger {

    private PrintWriter log_writer_top;
    private PrintWriter log_writer_bot;

    private String logfile_path_top;
    private String logfile_path_bot;

    private DemodPreference prefs;

    public Logger() {
        prefs = Preferences.DEMOD_PREFERENCE;
        if (prefs.isLogging()) {
            ensureLogfileTop();
            ensureLogfileBot();
        }
        EventBus.getDefault().register(this);
    }

    private void ensureLogfileTop() {
        // has logfile path changed?

        if (!prefs.getTopLogfilePath().equals(logfile_path_top)) {
            logfile_path_top = prefs.getTopLogfilePath();

            if (log_writer_top != null) {
                log_writer_top.close();
            }

            try {
                log_writer_top = new PrintWriter(new BufferedWriter(new FileWriter(logfile_path_top, true)));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void ensureLogfileBot() {
        // has logfile path changed?
        if (!prefs.getBotLogfilePath().equals(logfile_path_bot)) {
            logfile_path_bot = prefs.getBotLogfilePath();

            if (log_writer_bot != null) {
                log_writer_bot.close();
            }

            try {
                log_writer_bot = new PrintWriter(new BufferedWriter(new FileWriter(logfile_path_bot, true)));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    protected void finalize() throws Throwable {
        log_writer_top.close();
        super.finalize();
    }

    @Subscribe
    public void onEvent(final DemodInfoEvent event) {

        if (!prefs.isLogging()) // don't log anything
            return;

        PrintWriter writer;
        if (event.getTextPosition() == DemodInfoEvent.Position.TOP) {
            ensureLogfileTop();
            writer = log_writer_top;
        } else {
            ensureLogfileBot();
            writer = log_writer_bot;
        }

        switch (event.getMode()) {
            case APPEND_STRING:
                writer.print(event.getText());
                writer.flush();
                break;
            case WRITE_STRING:
                writer.print('\n' + event.getText());
                writer.flush();
                break;
        }

    }


}
