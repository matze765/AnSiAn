package de.tu.darmstadt.seemoo.ansian.gui.views;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;

import de.greenrobot.event.EventBus;
import de.greenrobot.event.Subscribe;
import de.tu.darmstadt.seemoo.ansian.R;
import de.tu.darmstadt.seemoo.ansian.control.events.ImagePickIntentResultEvent;



/**
 * Created by MATZE on 22.02.2017.
 */

public class SSTVView extends LinearLayout {
    public static final int IMAGE_PICKER_INTENT_RESULT_CODE = 42;

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

        ib_pickImage.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent photoPickerIntent = new Intent(Intent.ACTION_PICK);
                photoPickerIntent.setType("image/*");

                ((Activity) context).startActivityForResult(photoPickerIntent, IMAGE_PICKER_INTENT_RESULT_CODE);
            }
        });
    }

    @Subscribe
    public void onEvent(final ImagePickIntentResultEvent event){
        EditText et = (EditText) this.findViewById(R.id.et_imageToTransmit);
        et.setText(event.getFile().toString());

    }
}
