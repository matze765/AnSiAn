package de.tu.darmstadt.seemoo.ansian.control.events.morse;

import de.tu.darmstadt.seemoo.ansian.tools.StringFormatter;

public class MorseSymbolEvent {

	private float symbolSuccessRate;

	public MorseSymbolEvent(float symbolSuccessRate) {
		this.symbolSuccessRate = symbolSuccessRate;
	}

	public String getSuccessRateString() {
		return StringFormatter.formatPercent(symbolSuccessRate);
	}

}
