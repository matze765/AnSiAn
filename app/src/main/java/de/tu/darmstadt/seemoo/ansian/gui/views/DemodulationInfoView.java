package de.tu.darmstadt.seemoo.ansian.gui.views;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.text.TextUtils;
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
        demodInfoText.setHorizontallyScrolling(true);
        demodInfoText.setSingleLine(true);
        demodInfoText.setTypeface(Typeface.MONOSPACE);
        demodInfoText.setTextSize(12);

		demodTextText = (TextView) findViewById(R.id.demod_text_text);
        demodTextText.setHorizontallyScrolling(true);
        demodTextText.setSingleLine(true);
        demodTextText.setTypeface(Typeface.MONOSPACE);
        demodTextText.setSelected(true);
        demodTextText.setTextSize(12);


        DemodInfoEvent infoEvent  = EventBus.getDefault().getStickyEvent(DemodInfoEvent.class);
        if(infoEvent != null && infoEvent.getMode() != DemodInfoEvent.Mode.REPLACE_CHAR) {
            if(infoEvent.getTextPosition() == DemodInfoEvent.Position.TOP) {
                infoBuffer = new StringBuilder(infoEvent.getText());
                textBuffer = new StringBuilder();
                demodInfoText.setText(textBuffer.toString());
                if(infoEvent.isMarquee()) {
                    demodInfoText.setGravity(Gravity.LEFT);
                    demodInfoText.setEllipsize(TextUtils.TruncateAt.MARQUEE);
                    demodInfoText.setSelected(true);
                } else {
                    demodInfoText.setGravity(Gravity.RIGHT);
                    demodInfoText.setEllipsize(TextUtils.TruncateAt.END);
                }
            } else {
                infoBuffer = new StringBuilder();
                textBuffer = new StringBuilder(infoEvent.getText());
                demodTextText.setText(textBuffer.toString());
                if(infoEvent.isMarquee()) {
                    demodTextText.setGravity(Gravity.LEFT);
                    demodTextText.setEllipsize(TextUtils.TruncateAt.MARQUEE);
                    demodTextText.setSelected(true);

                } else {
                    demodTextText.setGravity(Gravity.RIGHT);
                    demodTextText.setEllipsize(TextUtils.TruncateAt.END);
                }
            }
        } else {
            textBuffer = new StringBuilder();
            infoBuffer = new StringBuilder();
        }
	}

    @Subscribe
    public void onEvent(final DemodInfoEvent event) {
        MainActivity.instance.runOnUiThread(new Runnable() {

            @Override
            public void run() {

                TextView textview;
                StringBuilder stringBuilder;
                if(event.getTextPosition()== DemodInfoEvent.Position.TOP) {
                    textview = demodInfoText;
                    stringBuilder = infoBuffer;
                } else {
                    textview = demodTextText;
                    stringBuilder = textBuffer;
                }

                switch(event.getMode()) {
                    case APPEND_STRING:
                        stringBuilder.append(event.getText());
                        break;
                    case WRITE_STRING:
                        stringBuilder.delete(0, stringBuilder.length());
                        stringBuilder.append(event.getText());
                        break;
                    case REPLACE_CHAR:
                        stringBuilder.setCharAt(event.getCharacterPosition(), event.getText().charAt(0));
                }

                textview.setText(stringBuilder.toString());
                if(event.isMarquee()) {
                    textview.setGravity(Gravity.LEFT);
                    textview.setEllipsize(TextUtils.TruncateAt.MARQUEE);
                    textview.setSelected(true);

                } else {
                    textview.setGravity(Gravity.RIGHT);
                    textview.setEllipsize(TextUtils.TruncateAt.END);
                }
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
