package de.tu.darmstadt.seemoo.ansian.gui.views;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import de.greenrobot.event.EventBus;
import de.greenrobot.event.Subscribe;
import de.tu.darmstadt.seemoo.ansian.R;
import de.tu.darmstadt.seemoo.ansian.control.StateHandler;
import de.tu.darmstadt.seemoo.ansian.control.events.RequestFrequencyEvent;
import de.tu.darmstadt.seemoo.ansian.control.events.RequestStateEvent;
import de.tu.darmstadt.seemoo.ansian.control.events.tx.TransmitEvent;
import de.tu.darmstadt.seemoo.ansian.control.events.tx.TransmitStatusEvent;
import de.tu.darmstadt.seemoo.ansian.control.events.tx.image.sstv.ImagePickIntentResultEvent;
import de.tu.darmstadt.seemoo.ansian.control.events.tx.image.sstv.SSTVTransmitEvent;
import de.tu.darmstadt.seemoo.ansian.model.demodulation.Demodulation;
import de.tu.darmstadt.seemoo.ansian.model.modulation.SSTV;
import de.tu.darmstadt.seemoo.ansian.model.preferences.Preferences;


/**
 * Created by MATZE on 22.02.2017.
 */

public class SSTVView extends LinearLayout {
    public static final int IMAGE_PICKER_INTENT_RESULT_CODE = 42;

    private static final String LOGTAG = "SSTVView";
    private boolean isTransmitting = false;
    private boolean isReceiving = false;
    private Uri imageUri;

    public SSTVView(Context context) {
        this(context, null);
    }

    public SSTVView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SSTVView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
        setBackgroundColor(Color.BLACK);

    }

    public void init(final Context context){
        LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.sstv_view, this);
        EventBus.getDefault().register(this);

        ImageButton ib_pickImage = (ImageButton) this.findViewById(R.id.btn_pickImage);
        SeekBar vgaGainSeekBar = (SeekBar) this.findViewById(R.id.vgaGainSeekBar);

        Button startTxButton = (Button) this.findViewById(R.id.transmitButton);
        Button startRxButton = (Button) this.findViewById(R.id.receiveButton);


        ib_pickImage.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent photoPickerIntent = new Intent(Intent.ACTION_PICK);
                photoPickerIntent.setType("image/*");

                ((Activity) context).startActivityForResult(photoPickerIntent, IMAGE_PICKER_INTENT_RESULT_CODE);
            }
        });

        startTxButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                if(!isTransmitting){
                    startTX();
                } else {
                    stopTX();
                }
            }
        });
        startRxButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                if(!isReceiving){
                    startRx();
                } else {
                    stopRx();
                }
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
                updateVgaGainLabel();
            }


        });

        updateVgaGainLabel();
    }

    private void updateVgaGainLabel() {
        TextView vgaGainLabel = (TextView) this.findViewById(R.id.vgaGainLabel);
        SeekBar vgaSeekBar = (SeekBar) this.findViewById(R.id.vgaGainSeekBar);
        vgaGainLabel.setText(String.format(getContext().getString(R.string.vga_gain_label), vgaSeekBar.getProgress()));
    }

    private void stopRx(){
        Button txBtn = (Button) this.findViewById(R.id.transmitButton);
        Button rxBtn = (Button) this.findViewById(R.id.receiveButton);
        if(isReceiving) {
            isReceiving = false;
            txBtn.setEnabled(true);
            rxBtn.setText(R.string.start_rx);
            EventBus.getDefault().post(new RequestStateEvent(StateHandler.State.STOPPED));
        }
    }
    private void startRx(){

        Button txBtn = (Button) this.findViewById(R.id.transmitButton);
        Button rxBtn = (Button) this.findViewById(R.id.receiveButton);
        EditText frequencyEditText = (EditText) this.findViewById(R.id.et_frequency);
        if(!isTransmitting && !isReceiving){
            int frequency = Integer.parseInt(frequencyEditText.getText().toString());
            Preferences.GUI_PREFERENCE.setDemodFrequency(frequency);
            Preferences.MISC_PREFERENCE.setDemodulation(Demodulation.DemoType.SSTV);
            StateHandler.setDemodulationMode(Demodulation.DemoType.SSTV);
            EventBus.getDefault().post(new RequestFrequencyEvent(frequency - 100000));
            EventBus.getDefault().post(new RequestStateEvent(StateHandler.State.MONITORING));


            isReceiving = true;
            txBtn.setEnabled(false);
            rxBtn.setText(R.string.stop_rx);
        }

    }

    private void startTX(){

        if(this.imageUri == null){
            Toast.makeText(this.getContext(), "No Image selected", Toast.LENGTH_LONG).show();
            return;
        }

        Button txBtn = (Button) this.findViewById(R.id.transmitButton);
        Button rxBtn = (Button) this.findViewById(R.id.receiveButton);

        EditText etFrequency = (EditText) this.findViewById(R.id.et_frequency);
        CheckBox cbAmp = (CheckBox) this.findViewById(R.id.cb_amp);
        CheckBox cbAntennaPower = (CheckBox) this.findViewById(R.id.cb_antenna);
        SeekBar sbVgaGain = (SeekBar) this.findViewById(R.id.vgaGainSeekBar);

        if(!isTransmitting && !isReceiving) {

            boolean crop = true;
            boolean repeat = false;
            int sampleRate = 1_000_000;
            long frequency =  Long.parseLong(etFrequency.getText().toString());
            boolean isAmplifier = cbAmp.isChecked();
            boolean isAntennaPower = cbAntennaPower.isChecked();
            int vgaGain = sbVgaGain.getProgress();



            SSTV.SSTV_TYPE type = SSTV.SSTV_TYPE.ROBOT_SSTV_BW_120;
            try {
                final InputStream imageStream = getContext().getContentResolver().openInputStream(imageUri);
                final Bitmap selectedImage = BitmapFactory.decodeStream(imageStream);
                imageStream.close();
                TransmitEvent transmitEvent = new SSTVTransmitEvent(TransmitEvent.State.MODULATION, TransmitEvent.Sender.GUI,
                        sampleRate, frequency, isAmplifier, isAntennaPower,vgaGain, selectedImage, crop, repeat, type);
                EventBus.getDefault().post(transmitEvent);
                isTransmitting = true;
                txBtn.setText(R.string.stop_tx);
                rxBtn.setEnabled(false);

            }  catch (FileNotFoundException e) {
                Toast.makeText(this.getContext(), "Image not found", Toast.LENGTH_LONG).show();
            } catch (IOException e) {
                Toast.makeText(this.getContext(), "IO Exception", Toast.LENGTH_LONG).show();
            }
        }

    }

    private void stopTX(){
        Button txBtn = (Button) this.findViewById(R.id.transmitButton);
        txBtn.setText(R.string.start_tx);

        Button rxBtn = (Button) this.findViewById(R.id.receiveButton);
        rxBtn.setEnabled(true);
        if(isTransmitting){
            isTransmitting = false;
            EventBus.getDefault().post(new TransmitStatusEvent(TransmitEvent.State.TXOFF, TransmitEvent.Sender.GUI));
        }
    }

    @Subscribe
    public void onEvent(final ImagePickIntentResultEvent event){
        EditText et = (EditText) this.findViewById(R.id.et_imageToTransmit);
        this.imageUri = event.getFile();
        Log.d(LOGTAG, this.imageUri.toString());

        String[] parts = this.imageUri.toString().split("/");
        et.setText(parts[parts.length-1]);

    }

    @Subscribe
    public void onEvent(final TransmitStatusEvent event){

        if(event.getSender() != TransmitEvent.Sender.GUI){
            switch(event.getState()){
                case TXOFF:
                    Activity activity = (Activity) getContext();
                    activity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            stopTX();
                        }
                    });
                    break;
                case TXACTIVE:
                    break;
                case MODULATION:
                    break;
            }

        }
    }
}
