package de.tu.darmstadt.seemoo.ansian.control.events.morse;

import de.tu.darmstadt.seemoo.ansian.tools.StringFormatter;

public class MorseCodeEvent {

	private float successRate;
	private float threshold;

	public MorseCodeEvent(float successRate, float threshold) {
		this.threshold = threshold;
		this.successRate = successRate;
	}

	public String getSuccessRateString() {
		return StringFormatter.formatPercent(successRate);
	}

	public String getThresholdString() {
		return StringFormatter.formatThreshold(threshold);
	}
}
