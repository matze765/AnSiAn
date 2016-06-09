package de.tu.darmstadt.seemoo.ansian.control.events;

import de.tu.darmstadt.seemoo.ansian.model.sources.IQSourceInterface;

public class SourceEvent {
	private IQSourceInterface source;
	private boolean opened;	// Indicates whether the source was already opened (i.e. source.open() was called)

	public SourceEvent(IQSourceInterface source, boolean opened) {
		this.source = source;
		this.opened = opened;
	}

	public IQSourceInterface getSource() {
		return source;
	}

	public boolean isOpened() {
		return opened;
	}
}
