package de.tu.darmstadt.seemoo.ansian.control.events.morse;

public class TransmitEvent {

	private boolean transmitting;

	public TransmitEvent(boolean b) {
		transmitting = b;
	}

	public boolean isTransmitting() {
		return transmitting;
	}
}
