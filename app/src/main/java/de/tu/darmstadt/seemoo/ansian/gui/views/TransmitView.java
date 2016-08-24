package de.tu.darmstadt.seemoo.ansian.gui.views;

import android.content.Context;
import android.graphics.Color;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import de.greenrobot.event.EventBus;
import de.greenrobot.event.Subscribe;
import de.tu.darmstadt.seemoo.ansian.MainActivity;
import de.tu.darmstadt.seemoo.ansian.R;
import de.tu.darmstadt.seemoo.ansian.control.events.morse.TransmitEvent;
import de.tu.darmstadt.seemoo.ansian.model.preferences.Preferences;

public class TransmitView extends LinearLayout {

    private SeekBar vgaGainSeekBar;
    private TextView vgaGainLabel;
    private CheckBox amplifierCheckBox;
    private CheckBox antennaPowerCheckBox;
    private EditText sampleRateEditText;
    private EditText frequencyEditText;
    private Button playButton;
    private static boolean sending = false;
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

        frequencyEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                Preferences.MISC_PREFERENCE.setSend_frequency(Integer.parseInt(s.toString()));
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

        playButton = (Button) findViewById(R.id.transmitButton);
        playButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(LOGTAG, "Send Button clicked; sending = " + sending);
                EventBus.getDefault().post(new TransmitEvent(!sending, TransmitEvent.Sender.GUI));
            }
        });
        setButtonText(sending);
        update();
    }

    private void update() {
        updateAmplifierCheckbox();
        updateAntennaPowerCheckbox();
        updateVgaGainLabel();
        updateSampleRateEditText();
        updateFrequencyeEditText();
        vgaGainSeekBar.setProgress(Preferences.MISC_PREFERENCE.getSend_vgaGain());
    }

    private void updateVgaGainLabel() {
        vgaGainLabel.setText(String.format(getContext().getString(R.string.vga_gain_label), Preferences.MISC_PREFERENCE.getSend_vgaGain()));
    }

    private void updateAmplifierCheckbox() {
        amplifierCheckBox.setChecked(Preferences.MISC_PREFERENCE.getAmplifier());
    }

    private void updateAntennaPowerCheckbox() {
        antennaPowerCheckBox.setChecked(Preferences.MISC_PREFERENCE.getAntennaPower());
    }

    private void updateSampleRateEditText() {
        sampleRateEditText.setText(Integer.toString(Preferences.MISC_PREFERENCE.getSend_sampleRate()));
    }

    private void updateFrequencyeEditText() {
        frequencyEditText.setText(Integer.toString(Preferences.MISC_PREFERENCE.getSend_frequency()));
    }

    private void setButtonText(boolean b) {
        if (b)
            playButton.setText(R.string.morse_button_stop);
        else
            playButton.setText(R.string.morse_button_send);
    }

    @Subscribe
    public void onEvent(final TransmitEvent event) {
        MainActivity.instance.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                setButtonText(sending = event.isTransmitting());
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
