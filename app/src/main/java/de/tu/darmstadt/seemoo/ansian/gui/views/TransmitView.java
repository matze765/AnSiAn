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
                Preferences.MISC_PREFERENCE.setSend_txMode(txMode);
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

        audioSource.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {

            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                Preferences.MISC_PREFERENCE.setRds_audio_source(audioSource.getSelectedItemPosition());
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

        morseFrequencyEditText.addTextChangedListener(new TextWatcher() {
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
        updateMorseWPMSeekBar();
        updateMorseFrequencyEditText();
        vgaGainSeekBar.setProgress(Preferences.MISC_PREFERENCE.getSend_vgaGain());
        audioSource.setSelection(Preferences.MISC_PREFERENCE.getRds_audio_source());
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
        int selected = Preferences.MISC_PREFERENCE.getSend_txMode().ordinal();
        if(selected < 4) {
            txModeSpinner.setSelection(selected);
        } else{
            txModeSpinner.setSelection(0);
        }
    }

    private void updateMorseWPMSeekBar() {
        morseWPMSeekBar.setProgress(Preferences.MISC_PREFERENCE.getMorse_wpm() - 1);
    }

    private void updateMorseFrequencyEditText() {
        morseFrequencyEditText.setText(Integer.toString(Preferences.MISC_PREFERENCE.getMorse_frequency()));
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
