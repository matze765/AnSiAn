package de.tu.darmstadt.seemoo.ansian.gui.views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;

import de.greenrobot.event.EventBus;
import de.tu.darmstadt.seemoo.ansian.R;
import de.tu.darmstadt.seemoo.ansian.control.DataHandler;
import de.tu.darmstadt.seemoo.ansian.control.StateHandler;
import de.tu.darmstadt.seemoo.ansian.control.events.FrequencyEvent;
import de.tu.darmstadt.seemoo.ansian.control.events.RequestFrequencyEvent;
import de.tu.darmstadt.seemoo.ansian.control.events.RequestStateEvent;
import de.tu.darmstadt.seemoo.ansian.control.events.SquelchChangeEvent;
import de.tu.darmstadt.seemoo.ansian.control.events.morse.TransmitEvent;
import de.tu.darmstadt.seemoo.ansian.model.FFTSample;
import de.tu.darmstadt.seemoo.ansian.model.demodulation.Demodulation;
import de.tu.darmstadt.seemoo.ansian.model.modulation.Modulation;
import de.tu.darmstadt.seemoo.ansian.model.preferences.GuiPreferences;
import de.tu.darmstadt.seemoo.ansian.model.preferences.MiscPreferences;
import de.tu.darmstadt.seemoo.ansian.model.preferences.Preferences;

/**
 * @author Matthias Kannwischer
 */

public class WalkieTalkieView extends LinearLayout {
    public static final String LOGTAG = "WalkieTalkieView";

    private int selectedFrequencyBand = 0;
    private boolean isTransmitting = false;
    private boolean isReceiving = false;
    private Thread squelchWatchThread;

    public WalkieTalkieView(Context context) {
        this(context, null);
    }

    public WalkieTalkieView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public WalkieTalkieView(Context context, AttributeSet attrs, int defaultStyle) {
        super(context, attrs, defaultStyle);
        init();
        setBackgroundColor(Color.BLACK);
    }

    protected void init() {
        LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.walkietalkie_view, this);

        final MiscPreferences miscPreferences = Preferences.MISC_PREFERENCE;
        final GuiPreferences guiPreferences   = Preferences.GUI_PREFERENCE;
        final EditText frequenyEditText = (EditText) this.findViewById(R.id.et_frequency);
        final SeekBar frequencySeekbar = (SeekBar) this.findViewById(R.id.sb_frequencySeekBar);
        Spinner frequencyBandSpinner = (Spinner) this.findViewById(R.id.sp_frequencyBands);
        SeekBar vgaGainSeekBar = (SeekBar) this.findViewById(R.id.vgaGainSeekBar);
        final Button receiveButton = (Button) this.findViewById(R.id.receiveButton);
        final Button transmitButton = (Button) this.findViewById(R.id.transmitButton);
        CheckBox amplifierCheckBox = (CheckBox) findViewById(R.id.cb_amp);
        CheckBox antennaPowerCheckBox = (CheckBox) findViewById(R.id.cb_antenna);
        SeekBar squelchSeekBar = (SeekBar) this.findViewById(R.id.squelchSeekBar);
        Spinner modulationSpinner = (Spinner) this.findViewById(R.id.sp_modulation);

        vgaGainSeekBar.setProgress(miscPreferences.getSend_vgaGain());
        squelchSeekBar.setProgress((int)guiPreferences.getSquelch()+100);


        modulationSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                miscPreferences.setDemodulation(getCurrentRxMode());
                miscPreferences.setSend_txMode(getCurrentTxMode());
                StateHandler.setDemodulationMode(getCurrentRxMode());
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });


        frequencyBandSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                int previousFrequencyBand = selectedFrequencyBand;
                selectedFrequencyBand = i;
                String[] frequencyBands = getResources().getStringArray(R.array.frequency_bands);
                int[] frequencyBandsMin = getResources().getIntArray(R.array.frequency_bands_min);
                int[] frequencyBandsMax = getResources().getIntArray(R.array.frequency_bands_max);

                if(i==frequencyBands.length-1){
                    // free frequency selection "other"
                    frequenyEditText.setEnabled(true);
                    frequencySeekbar.setEnabled(false);
                } else {
                    frequenyEditText.setEnabled(false);
                    frequencySeekbar.setEnabled(true);
                    frequencySeekbar.setMax(frequencyBandsMax[i] - frequencyBandsMin[i]);
                    frequencySeekbar.setProgress(0);
                }
            }
            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });

        frequencySeekbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                int[] frequencyBandsMin = getResources().getIntArray(R.array.frequency_bands_min);
                int frequencyHz = progress+frequencyBandsMin[selectedFrequencyBand];
                frequenyEditText.setText(Integer.toString(frequencyHz));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });


        receiveButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                if(isReceiving){
                    isReceiving = false;
                    receiveButton.setText("START RECEPTION");
                    killSquelchWatchThread();
                    // if we are currently trasnmitting, we just need to unset the flag to prevent
                    // switching to reception mode
                    if(!isTransmitting) {
                        EventBus.getDefault().post(new RequestStateEvent(StateHandler.State.STOPPED));
                    }
                } else {
                    isReceiving = true;
                    receiveButton.setText("STOP RECEPTION");

                    startSquelchWatchThread();
                    // if we are currently transmitting, we have to wait until that is stopped
                    if(!isTransmitting) {

                        EventBus.getDefault().post(new RequestStateEvent(StateHandler.State.MONITORING));
                    }
                }

            }
        });



        transmitButton.setOnClickListener(new OnClickListener() {


            @Override
            public void onClick(View view) {

                if(!isTransmitting){
                    isTransmitting = true;
                    transmitButton.setText("STOP");
                    if(isReceiving) {
                        // stop the reception
                        EventBus.getDefault().post(new RequestStateEvent(StateHandler.State.STOPPED));
                    }
                    // start the transmission
                    EventBus.getDefault().post(new TransmitEvent(TransmitEvent.State.MODULATION, TransmitEvent.Sender.GUI));
                } else {
                    isTransmitting = false;
                    transmitButton.setText("TRANSMIT");

                    // stop the transmission
                    EventBus.getDefault().post(new TransmitEvent(TransmitEvent.State.TXOFF, TransmitEvent.Sender.GUI));

                    if(isReceiving){
                        // start the reception again
                        Preferences.MISC_PREFERENCE.setDemodulation(Demodulation.DemoType.WFM);
                        EventBus.getDefault().post(new RequestStateEvent(StateHandler.State.MONITORING));
                    }

                }
            }
        });
        squelchSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                if(b) {
                    float squelch = i - 100;
                    Preferences.GUI_PREFERENCE.setSquelch(squelch);
                    updateSquelchLabel();
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });





        // this code is mainly copied over from transmit view
        // TODO: find better solution to avoid code duplication
        vgaGainSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    Preferences.MISC_PREFERENCE.setSend_vgaGain(progress);
                }
                updateVgaGainLabel();
            }


        });


        amplifierCheckBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                Preferences.MISC_PREFERENCE.setSend_amplifier(isChecked);
            }
        });

        antennaPowerCheckBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                Preferences.MISC_PREFERENCE.setSend_antennaPower(isChecked);
            }
        });

        frequenyEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                try {
                    int i = Integer.parseInt(s.toString());
                    // set transmit frequency
                    Preferences.MISC_PREFERENCE.setSend_frequency(i);
                    // set receive frequency
                    EventBus.getDefault().post(new RequestFrequencyEvent(i-100000));
                    guiPreferences.setDemodFrequency(i);
                } catch (NumberFormatException e) {
                    // not an integer; ignore
                }
            }
        });
        updateSquelchLabel();
        updateVgaGainLabel();
        miscPreferences.setDemodulation(getCurrentRxMode());
        miscPreferences.setSend_txMode(getCurrentTxMode());
        StateHandler.setDemodulationMode(getCurrentRxMode());

    }


    private void updateVgaGainLabel() {
        TextView vgaGainLabel = (TextView) this.findViewById(R.id.vgaGainLabel);
        vgaGainLabel.setText(String.format(getContext().getString(R.string.vga_gain_label), Preferences.MISC_PREFERENCE.getSend_vgaGain()));
    }

    private void updateSquelchLabel(){
        Log.d(LOGTAG, "update label "+Preferences.GUI_PREFERENCE.getSquelch());
        TextView squelchLabel = (TextView) this.findViewById(R.id.squelchLabel);
        squelchLabel.setText(String.format("Squelch: %s dB", (int) Preferences.GUI_PREFERENCE.getSquelch()));
    }

    private Modulation.TxMode getCurrentTxMode(){
        Spinner txModeSpinner = (Spinner) this.findViewById(R.id.sp_modulation);
        int idx = txModeSpinner.getSelectedItemPosition();

        switch(idx){
            case 0:
                return Modulation.TxMode.FM;
            case 1:
                return Modulation.TxMode.USB;
            case 2:
                return Modulation.TxMode.LSB;
            default:
                return null;
        }
    }

    private Demodulation.DemoType getCurrentRxMode(){
        Spinner rxModeSpinner = (Spinner) this.findViewById(R.id.sp_modulation);
        int idx = rxModeSpinner.getSelectedItemPosition();
        switch (idx){
            case 0:
                return Demodulation.DemoType.WFM;
            case 1:
                return Demodulation.DemoType.USB;
            case 2:
                return Demodulation.DemoType.LSB;
            default:
                return null;
        }
    }

    private void startSquelchWatchThread(){
        Log.d(LOGTAG, "starting squelchWatchThread");
        squelchWatchThread = new Thread(new Runnable() {
            @Override
            public void run() {
                while(true) {
                    squelchUpdate();
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                        Log.d(LOGTAG, "squelch watch thread is dying");
                        break;
                    }
                }
            }

            public void squelchUpdate() {
                //squelch handling
                if(StateHandler.isDemodulating()) {
                    GuiPreferences guiPreferences = Preferences.GUI_PREFERENCE;
                    float squelch = guiPreferences.getSquelch();
                    long demodFrequency = guiPreferences.getDemodFrequency();
                    DataHandler.getInstance().requestNewFFTSample();
                    FFTSample sample = DataHandler.getInstance().getLastFFTSample();
                    if(sample != null) {

                        float averageSignalStrength =sample.getAverage(demodFrequency,
                                Preferences.GUI_PREFERENCE.getDemodBandwidth());
                        guiPreferences.setSquelchSatisfied(squelch < averageSignalStrength);
                    }
                }
            }

        });
        squelchWatchThread.start();
    }

    private void killSquelchWatchThread(){
        Log.d(LOGTAG, "killing squelch watch thread");
        if(squelchWatchThread != null){
            squelchWatchThread.interrupt();
            squelchWatchThread = null;
        }
    }


}
