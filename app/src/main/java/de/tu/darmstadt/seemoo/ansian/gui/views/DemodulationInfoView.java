package de.tu.darmstadt.seemoo.ansian.gui.views;

import android.content.Context;
import android.graphics.Color;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.widget.LinearLayout;
import android.widget.TextView;
import de.greenrobot.event.EventBus;
import de.greenrobot.event.Subscribe;
import de.tu.darmstadt.seemoo.ansian.MainActivity;
import de.tu.darmstadt.seemoo.ansian.R;
import de.tu.darmstadt.seemoo.ansian.control.events.morse.DemodInfoEvent;
import de.tu.darmstadt.seemoo.ansian.control.events.morse.DemodTextEvent;
import de.tu.darmstadt.seemoo.ansian.control.events.morse.MorseCodeEvent;
import de.tu.darmstadt.seemoo.ansian.control.events.morse.MorseStateEvent;
import de.tu.darmstadt.seemoo.ansian.control.events.morse.MorseSymbolEvent;
import de.tu.darmstadt.seemoo.ansian.model.demodulation.morse.Morse.State;
import de.tu.darmstadt.seemoo.ansian.tools.morse.Decoder;

public class DemodulationInfoView extends LinearLayout {

	private TextView demodInfoText;
    private TextView demodTextText;

	private String LOGTAG = "DemodulationInfoView";
    private StringBuffer infoBuffer;
    private StringBuffer textBuffer;
    private Decoder decoder;

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
		demodTextText = (TextView) findViewById(R.id.demod_text_text);
		decoder = new Decoder();
		demodInfoText.setGravity(Gravity.RIGHT);
		demodInfoText.setHorizontallyScrolling(true);
		demodTextText.setGravity(Gravity.RIGHT);
		demodTextText.setHorizontallyScrolling(true);
		MorseCodeEvent event = EventBus.getDefault().getStickyEvent(MorseCodeEvent.class);
		if (event != null) {
			infoBuffer = new StringBuffer(event.getCompleteCodeString());
			demodInfoText.setText(infoBuffer);
			demodTextText.setText(new StringBuilder().append(decoder.decode(cutString(infoBuffer))));
		} else {
            infoBuffer = new StringBuffer();
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
                        textBuffer = new StringBuffer(event.getText());
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
                        infoBuffer = new StringBuffer(event.getText());
                        break;
                    case REPLACE_CHAR:
                        infoBuffer.setCharAt(event.getPosition(), event.getText().charAt(0));
                }

                demodInfoText.setText(infoBuffer.toString());

            }

        });
    }



//	@Subscribe
//	public void onEvent(final MorseCodeEvent event) {
//		if (event.isInRange()) {
//			MainActivity.instance.runOnUiThread(new Runnable() {
//
//				@Override
//				public void run() {
//					infoBuffer = new StringBuffer(event.getCompleteCodeString());
//                    Log.d(LOGTAG, "completeCodeString: "+infoBuffer.toString());
//
//
//
//					demodTextText.setText(new StringBuilder().append(decoder.decode(cutString(infoBuffer))));
//					demodInfoText.setText(infoBuffer);
//				}
//
//			});
//
//		}
//	}

	@Subscribe
	public void onEvent(final MorseStateEvent event) {
		if (event.getState() == State.STOPPED) {
			MainActivity.instance.runOnUiThread(new Runnable() {
				@Override
				public void run() {
					infoBuffer = new StringBuffer();
                    textBuffer = new StringBuffer();
				}
			});
		}
	}

	private static String cutString(StringBuffer buffer) {
        // return buffer.toString().trim();
		int start = buffer.indexOf(" ");
		int end = buffer.lastIndexOf(" ");
		if (start == end || start == -1 || end == -1)
			return "";
		else
			return buffer.substring(start, end);
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
