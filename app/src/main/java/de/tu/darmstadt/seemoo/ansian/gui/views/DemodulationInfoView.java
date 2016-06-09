package de.tu.darmstadt.seemoo.ansian.gui.views;

import android.content.Context;
import android.graphics.Color;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.widget.LinearLayout;
import android.widget.TextView;
import de.greenrobot.event.EventBus;
import de.greenrobot.event.Subscribe;
import de.tu.darmstadt.seemoo.ansian.MainActivity;
import de.tu.darmstadt.seemoo.ansian.R;
import de.tu.darmstadt.seemoo.ansian.control.events.DemodInfoEvent;
import de.tu.darmstadt.seemoo.ansian.control.events.DemodTextEvent;
import de.tu.darmstadt.seemoo.ansian.control.events.morse.MorseStateEvent;
import de.tu.darmstadt.seemoo.ansian.model.demodulation.morse.Morse.State;

public class DemodulationInfoView extends LinearLayout {

	private TextView demodInfoText;
    private TextView demodTextText;

	private String LOGTAG = "DemodulationInfoView";
    private StringBuilder infoBuffer;
    private StringBuilder textBuffer;

	public DemodulationInfoView(Context context) {
		this(context, null);
	}

	public DemodulationInfoView(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public DemodulationInfoView(Context context, AttributeSet attrs, int defaultStyle) {
		super(context, attrs, defaultStyle);
        isInEditMode();
		init();
        setBackgroundColor(Color.BLACK);
	}

	private void init() {
		LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		inflater.inflate(R.layout.demodulation_info_view, this);

		demodInfoText = (TextView) findViewById(R.id.demod_info_text);
        demodInfoText.setGravity(Gravity.RIGHT);
        demodInfoText.setHorizontallyScrolling(true);

		demodTextText = (TextView) findViewById(R.id.demod_text_text);
        demodTextText.setGravity(Gravity.RIGHT);
        demodTextText.setHorizontallyScrolling(true);


        DemodTextEvent textEvent = EventBus.getDefault().getStickyEvent(DemodTextEvent.class);
        if(textEvent != null && textEvent.getMode() != DemodTextEvent.Mode.REPLACE_CHAR) {
            textBuffer = new StringBuilder(textEvent.getText());
            demodTextText.setText(textBuffer.toString());
        } else {
            textBuffer = new StringBuilder();
        }

        DemodInfoEvent infoEvent  = EventBus.getDefault().getStickyEvent(DemodInfoEvent.class);
        if(infoEvent != null && infoEvent.getMode() != DemodInfoEvent.Mode.REPLACE_CHAR) {
            infoBuffer = new StringBuilder(textEvent.getText());
            demodInfoText.setText(textBuffer.toString());
        } else {
            infoBuffer = new StringBuilder();
        }

	}

    @Subscribe
    public void onEvent(final DemodTextEvent event) {
        MainActivity.instance.runOnUiThread(new Runnable() {

            @Override
            public void run() {

                switch(event.getMode()) {
                    case APPEND_STRING:
                        textBuffer.append(event.getText());
                        break;
                    case WRITE_STRING:
                        textBuffer = new StringBuilder(event.getText());
                        break;
                    case REPLACE_CHAR:
                        textBuffer.setCharAt(event.getPosition(), event.getText().charAt(0));
                }

                demodTextText.setText(textBuffer.toString());

            }

        });
    }

    @Subscribe
    public void onEvent(final DemodInfoEvent event) {
        MainActivity.instance.runOnUiThread(new Runnable() {

            @Override
            public void run() {

                switch(event.getMode()) {
                    case APPEND_STRING:
                        infoBuffer.append(event.getText());
                        break;
                    case WRITE_STRING:
                        infoBuffer = new StringBuilder(event.getText());
                        break;
                    case REPLACE_CHAR:
                        infoBuffer.setCharAt(event.getPosition(), event.getText().charAt(0));
                }

                demodInfoText.setText(infoBuffer.toString());

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
