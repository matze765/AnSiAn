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
import de.tu.darmstadt.seemoo.ansian.control.events.morse.TransmitEvent;
import de.tu.darmstadt.seemoo.ansian.model.modulation.Modulation;
import de.tu.darmstadt.seemoo.ansian.model.preferences.Preferences;

public class TransmitView extends LinearLayout {

    private Spinner txModeSpinner;
    private EditText payloadTextEditText;
    private SeekBar vgaGainSeekBar;
    private SeekBar morseWPMSeekBar;
    private TextView morseWPMLabel;
    private EditText morseFrequency;
    private TextView vgaGainLabel;
    private CheckBox amplifierCheckBox;
    private CheckBox antennaPowerCheckBox;
    private EditText sampleRateEditText;
    private EditText frequencyEditText;
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

        txModeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int position, long l) {
                Modulation.TxMode txMode = Modulation.TxMode.values()[txModeSpinner.getSelectedItemPosition()];
                Preferences.MISC_PREFERENCE.setSend_txMode(txMode);
                if (txMode == Modulation.TxMode.RAWIQ) {
                    payloadTextEditText.setEnabled(false);
                    morseWPMSeekBar.setEnabled(false);
                    morseWPMLabel.setEnabled(false);
                    morseFrequency.setEnabled(false);
                    sampleRateEditText.setEnabled(true);
                } else {
                    payloadTextEditText.setEnabled(true);
                    morseWPMSeekBar.setEnabled(true);
                    morseWPMLabel.setEnabled(true);
                    morseFrequency.setEnabled(true);
                    sampleRateEditText.setEnabled(false);
                    sampleRateEditText.setText("1000000");
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });

        payloadTextEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                Preferences.MISC_PREFERENCE.setSend_payloadText(s.toString());
            }
        });

        sampleRateEditText = (EditText) findViewById(R.id.et_sampRate);
        frequencyEditText = (EditText) findViewById(R.id.et_freq);

        sampleRateEditText.addTextChangedListener(new TextWatcher() {
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
                    Preferences.MISC_PREFERENCE.setSend_sampleRate(i);
                } catch (NumberFormatException e) {
                    // not an integer; ignore
                }
            }
        });

        morseFrequency = (EditText) findViewById(R.id.et_morseFreq);
        morseFrequency.addTextChangedListener(new TextWatcher() {
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
                    Preferences.MISC_PREFERENCE.setMorse_frequency(i);
                } catch (NumberFormatException e) {
                    // not an integer; ignore
                }
            }
        });

        frequencyEditText.addTextChangedListener(new TextWatcher() {
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
                    Preferences.MISC_PREFERENCE.setSend_frequency(i);
                } catch (NumberFormatException e) {
                    // not an integer; ignore
                }
            }
        });


        amplifierCheckBox = (CheckBox) findViewById(R.id.cb_amp);
        antennaPowerCheckBox = (CheckBox) findViewById(R.id.cb_antenna);

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

        vgaGainLabel = (TextView) findViewById(R.id.vgaGainLabel);
        vgaGainSeekBar = (SeekBar) findViewById(R.id.vgaGainSeekBar);
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

        morseWPMLabel = (TextView) findViewById(R.id.morseWPMLabel);
        morseWPMSeekBar = (SeekBar) findViewById(R.id.morseWPMSeekBar);
        morseWPMSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    int wpm = progress + 1;
                    Preferences.MISC_PREFERENCE.setMorse_wpm(wpm);
                }
                updateWPMLabel();
            }
        });

        playButton = (Button) findViewById(R.id.transmitButton);
        playButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (txState == TransmitEvent.State.TXOFF)
                    EventBus.getDefault().post(new TransmitEvent(TransmitEvent.State.MODULATION, TransmitEvent.Sender.GUI));
                else
                    EventBus.getDefault().post(new TransmitEvent(TransmitEvent.State.TXOFF, TransmitEvent.Sender.GUI));
            }
        });
        updateButtonText();
        update();
    }

    private void updateWPMLabel() {
        morseWPMLabel.setText(String.format(getContext().getString(R.string.morseWPMLabel_text),
                Preferences.MISC_PREFERENCE.getMorse_wpm(), Preferences.MISC_PREFERENCE.getMorse_DitDuration()));
    }

    public void update() {
        updateWPMLabel();
        updateAmplifierCheckbox();
        updateAntennaPowerCheckbox();
        updateVgaGainLabel();
        updateSampleRateEditText();
        updateFrequencyEditText();
        updatePayloadTextEditText();
        updateTxModeSpinner();
        vgaGainSeekBar.setProgress(Preferences.MISC_PREFERENCE.getSend_vgaGain());
    }

    private void updateVgaGainLabel() {
        vgaGainLabel.setText(String.format(getContext().getString(R.string.vga_gain_label), Preferences.MISC_PREFERENCE.getSend_vgaGain()));
    }

    private void updateAmplifierCheckbox() {
        amplifierCheckBox.setChecked(Preferences.MISC_PREFERENCE.isSend_amplifier());
    }

    private void updateAntennaPowerCheckbox() {
        antennaPowerCheckBox.setChecked(Preferences.MISC_PREFERENCE.isSend_antennaPower());
    }

    private void updateSampleRateEditText() {
        sampleRateEditText.setText(Integer.toString(Preferences.MISC_PREFERENCE.getSend_sampleRate()));
    }

    private void updateFrequencyEditText() {
        frequencyEditText.setText(Integer.toString(Preferences.MISC_PREFERENCE.getSend_frequency()));
    }

    private void updatePayloadTextEditText() {
        payloadTextEditText.setText(Preferences.MISC_PREFERENCE.getSend_payloadText());
    }

    private void updateTxModeSpinner() {
        txModeSpinner.setSelection(Preferences.MISC_PREFERENCE.getSend_txMode().ordinal());
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
