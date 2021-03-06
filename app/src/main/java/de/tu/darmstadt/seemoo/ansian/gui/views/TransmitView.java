package de.tu.darmstadt.seemoo.ansian.gui.views;

import android.content.Context;
import android.graphics.Color;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
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
import de.greenrobot.event.Subscribe;
import de.tu.darmstadt.seemoo.ansian.MainActivity;
import de.tu.darmstadt.seemoo.ansian.R;
import de.tu.darmstadt.seemoo.ansian.control.events.tx.TransmitEvent;
import de.tu.darmstadt.seemoo.ansian.control.events.tx.TransmitStatusEvent;
import de.tu.darmstadt.seemoo.ansian.control.events.tx.data.morse.MorseTransmitEvent;
import de.tu.darmstadt.seemoo.ansian.control.events.tx.data.psk31.PSK31TransmitEvent;
import de.tu.darmstadt.seemoo.ansian.control.events.tx.rawiq.RawIQTransmitEvent;
import de.tu.darmstadt.seemoo.ansian.control.events.tx.data.rds.RDSTransmitEvent;
import de.tu.darmstadt.seemoo.ansian.model.modulation.Modulation;
import de.tu.darmstadt.seemoo.ansian.model.preferences.MiscPreferences;
import de.tu.darmstadt.seemoo.ansian.model.preferences.Preferences;

public class TransmitView extends LinearLayout {

    private Spinner txModeSpinner;
    private EditText payloadTextEditText;
    private SeekBar vgaGainSeekBar;
    private SeekBar morseWPMSeekBar;
    private TextView morseWPMLabel;
    private TextView morseFrequencyLabel;
    private EditText morseFrequencyEditText;
    private TextView vgaGainLabel;
    private CheckBox amplifierCheckBox;
    private CheckBox antennaPowerCheckBox;
    private EditText sampleRateEditText;
    private EditText frequencyEditText;
    private TextView audioSourceLabel;
    private Spinner audioSource;
    private Button playButton;
    private static TransmitEvent.State txState = TransmitEvent.State.TXOFF;
    private static final String LOGTAG = "TransmitView";

    public TransmitView(Context context) {
        this(context, null);
    }

    public TransmitView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public TransmitView(Context context, AttributeSet attrs, int defaultStyle) {
        super(context, attrs, defaultStyle);
        init();
        setBackgroundColor(Color.BLACK);
    }

    protected void init() {
        LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.transmit_view, this);

        txModeSpinner = (Spinner) findViewById(R.id.sp_txMode);
        payloadTextEditText = (EditText) findViewById(R.id.et_payloadText);
        sampleRateEditText = (EditText) findViewById(R.id.et_sampRate);
        frequencyEditText = (EditText) findViewById(R.id.et_freq);
        morseFrequencyEditText = (EditText) findViewById(R.id.et_morseFreq);
        morseFrequencyLabel = (TextView) findViewById(R.id.tv_morseFreqLabel);
        amplifierCheckBox = (CheckBox) findViewById(R.id.cb_amp);
        antennaPowerCheckBox = (CheckBox) findViewById(R.id.cb_antenna);
        vgaGainLabel = (TextView) findViewById(R.id.vgaGainLabel);
        vgaGainSeekBar = (SeekBar) findViewById(R.id.vgaGainSeekBar);
        morseWPMLabel = (TextView) findViewById(R.id.morseWPMLabel);
        morseWPMSeekBar = (SeekBar) findViewById(R.id.morseWPMSeekBar);
        playButton = (Button) findViewById(R.id.transmitButton);
        audioSourceLabel = (TextView) findViewById(R.id.tv_audioSource);
        audioSource = (Spinner) findViewById(R.id.sp_audioSource);

        txModeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int position, long l) {
                Modulation.TxMode txMode = Modulation.TxMode.values()[txModeSpinner.getSelectedItemPosition()];
                switch (txMode) {
                    case RAWIQ:
                        payloadTextEditText.setEnabled(false);
                        morseWPMSeekBar.setVisibility(View.GONE);
                        morseWPMLabel.setVisibility(View.GONE);
                        morseFrequencyLabel.setVisibility(View.GONE);
                        morseFrequencyEditText.setVisibility(View.GONE);
                        audioSource.setVisibility(View.GONE);
                        audioSourceLabel.setVisibility(View.GONE);
                        sampleRateEditText.setEnabled(true);
                        break;
                    case MORSE:
                        payloadTextEditText.setEnabled(true);
                        morseWPMSeekBar.setVisibility(View.VISIBLE);
                        morseWPMLabel.setVisibility(View.VISIBLE);
                        morseFrequencyLabel.setVisibility(View.VISIBLE);
                        morseFrequencyEditText.setVisibility(View.VISIBLE);
                        audioSource.setVisibility(View.GONE);
                        audioSourceLabel.setVisibility(View.GONE);
                        sampleRateEditText.setEnabled(false);
                        sampleRateEditText.setText("1000000");
                        break;
                    case PSK31:
                        payloadTextEditText.setEnabled(true);
                        morseWPMSeekBar.setVisibility(View.GONE);
                        morseWPMLabel.setVisibility(View.GONE);
                        morseFrequencyLabel.setVisibility(View.GONE);
                        morseFrequencyEditText.setVisibility(View.GONE);
                        audioSource.setVisibility(View.GONE);
                        audioSourceLabel.setVisibility(View.GONE);
                        sampleRateEditText.setEnabled(false);
                        sampleRateEditText.setText("1000000");
                        break;
                    case RDS:
                        payloadTextEditText.setEnabled(true);
                        morseWPMSeekBar.setVisibility(View.GONE);
                        morseWPMLabel.setVisibility(View.GONE);
                        morseFrequencyLabel.setVisibility(View.GONE);
                        morseFrequencyEditText.setVisibility(View.GONE);
                        audioSource.setVisibility(View.VISIBLE);
                        audioSourceLabel.setVisibility(View.VISIBLE);

                    default:
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });

        vgaGainSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                updateVgaGainLabel();
            }
        });

        morseWPMSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
              updateWPMLabel();
            }
        });
        playButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (txState == TransmitEvent.State.TXOFF) {
                    Modulation.TxMode txMode = Modulation.TxMode.values()[txModeSpinner.getSelectedItemPosition()];
                    TransmitEvent event = null;
                    String payload;

                    int sampleRate = Integer.parseInt(sampleRateEditText.getText().toString());
                    long frequency = Long.parseLong(frequencyEditText.getText().toString());
                    boolean isAmp = amplifierCheckBox.isChecked();
                    boolean isAntennaPower = antennaPowerCheckBox.isChecked();
                    int vgaGain = vgaGainSeekBar.getProgress();

                    switch (txMode) {
                        case RAWIQ:
                            String fileName = Preferences.MISC_PREFERENCE.getSend_fileName();
                            event = new RawIQTransmitEvent(TransmitEvent.State.MODULATION,
                                    TransmitEvent.Sender.GUI, sampleRate, frequency, isAmp,
                                    isAntennaPower, vgaGain, fileName);
                            break;
                        case MORSE:
                            int wpm = morseWPMSeekBar.getProgress() + 1;
                            int morseFrequency = Integer.parseInt(morseFrequencyEditText.getText().toString());
                            payload = payloadTextEditText.getText().toString();
                            event = new MorseTransmitEvent(TransmitEvent.State.MODULATION,
                                    TransmitEvent.Sender.GUI, sampleRate, frequency, isAmp,
                                    isAntennaPower, vgaGain,
                                    payload, wpm, morseFrequency);
                            break;
                        case PSK31:
                            payload = payloadTextEditText.getText().toString();
                            event = new PSK31TransmitEvent(TransmitEvent.State.MODULATION,
                                    TransmitEvent.Sender.GUI, sampleRate, frequency, isAmp,
                                    isAntennaPower, vgaGain,
                                    payload);
                            break;
                        case RDS:
                            payload = payloadTextEditText.getText().toString();
                            int audioSourcePosition = audioSource.getSelectedItemPosition();
                            event = new RDSTransmitEvent(TransmitEvent.State.MODULATION,
                                    TransmitEvent.Sender.GUI, sampleRate, frequency, isAmp,
                                    isAntennaPower, vgaGain,
                                    payload, audioSourcePosition == 0);
                            break;
                    }
                    if (event != null) EventBus.getDefault().post(event);
                } else {
                    EventBus.getDefault().post(new TransmitStatusEvent(TransmitEvent.State.TXOFF, TransmitEvent.Sender.GUI));
                }
            }
        });
        updateButtonText();
        update();
    }



    public void update() {
        updateWPMLabel();
        updateVgaGainLabel();
    }

    private void updateVgaGainLabel() {
        vgaGainLabel.setText(String.format(getContext().getString(R.string.vga_gain_label), vgaGainSeekBar.getProgress()));
    }

    private void updateWPMLabel() {
        int wpm = morseWPMSeekBar.getProgress()+1;
        int ditDuration = (int) Math.round(1200d / wpm);
        morseWPMLabel.setText(String.format(getContext().getString(R.string.morseWPMLabel_text),
                wpm, ditDuration));
    }


    private void updateButtonText() {
        switch (txState) {
            case TXOFF:
                playButton.setText(R.string.morse_button_send);
                break;
            case MODULATION:
                playButton.setText(R.string.morse_button_stop_modulation);
                break;
            case TXACTIVE:
                playButton.setText(R.string.morse_button_stop_tx);
                break;
            default:
                break;
        }
    }

    @Subscribe
    public void onEvent(final TransmitEvent event) {
        MainActivity.instance.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                txState = event.getState();
                Log.i(LOGTAG, "onEvent: now in state " + txState);
                updateButtonText();
            }
        });
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        EventBus.getDefault().unregister(this);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        EventBus.getDefault().register(this);
    }

}
