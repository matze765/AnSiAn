package de.tu.darmstadt.seemoo.ansian.model.demodulation;

import android.util.Log;

import de.tu.darmstadt.seemoo.ansian.model.SamplePacket;
import de.tu.darmstadt.seemoo.ansian.model.filter.FirFilter;
import de.tu.darmstadt.seemoo.ansian.model.preferences.Preferences;

public class USB extends SSB {
	private static final String LOGTAG = "USB(SSB)";
	private PSK31 psk31;
	private SamplePacket psk31Filtered;
	private FirFilter psk31Filter;

	public USB() {
		MIN_USER_FILTER_WIDTH = 1500;
		MAX_USER_FILTER_WIDTH = 5000;
		userFilterCutOff = MAX_USER_FILTER_WIDTH + MIN_USER_FILTER_WIDTH / 2;
		psk31 = new PSK31();
	}

	@Override
	public void demodulate(SamplePacket input, SamplePacket output) {
		super.demodulateSSB(input, output, true);

		if(Preferences.MORSE_PREFERENCE.isUsbPSK31()) {
			// Filter for PSK31 demodulation:
			if (psk31Filter == null) {
				psk31Filter = FirFilter.createLowPass(4, 1, quadratureRate / 2, 1500, 1000, 40);
				Log.d(LOGTAG, "demodulate: created new psk31Filter with " + psk31Filter.getNumberOfTaps()
						+ " taps. Decimation=" + psk31Filter.getDecimation() + " Cut-Off="
						+ psk31Filter.getCutOffFrequency() + " transition=" + psk31Filter.getTransitionWidth());
			}
			if (psk31Filtered == null || psk31Filtered.capacity() < output.size() / psk31Filter.getDecimation()) {
				psk31Filtered = new SamplePacket(output.size() / psk31Filter.getDecimation());
			}
			psk31Filtered.setSize(0);
			if (psk31Filter.filter(output, psk31Filtered, 0, output.size()) < output.size()) {
				Log.e(LOGTAG, "demodulate: [psk31Filter] could not filter all samples from input packet.");
			}

			psk31.demodulate(psk31Filtered, null);
		}
	}

	@Override
	public DemoType getType() {
		return DemoType.USB;
	}

	@Override
	public boolean isLowerBandShown() {
		return false;
	}
}
