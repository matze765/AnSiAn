package de.tu.darmstadt.seemoo.ansian.model.demodulation;

import android.util.Log;

import de.tu.darmstadt.seemoo.ansian.model.SamplePacket;
import de.tu.darmstadt.seemoo.ansian.model.filter.FirFilter;

public class USB extends SSB {
	private static final String LOGTAG = "USB(SSB)";
	private PSK31 psk31;
	private SamplePacket psk31Filtered;
	private FirFilter psk31Filter;

	/*// DEBUG stuff
	private File debugFile = null;
	private BufferedOutputStream debugOutputStream = null;
	private int debugBytesWritten = 0;
	private int waitCounter = 0;
	// DEBUG */

	public USB() {
		MIN_USER_FILTER_WIDTH = 1500;
		MAX_USER_FILTER_WIDTH = 5000;
		userFilterCutOff = MAX_USER_FILTER_WIDTH + MIN_USER_FILTER_WIDTH / 2;
		psk31 = new PSK31();
	}

	@Override
	public void demodulate(SamplePacket input, SamplePacket output) {
		super.demodulateSSB(input, output, true);


		// Filter for PSK31 demodulation:
        if (psk31Filter == null) {
            psk31Filter = FirFilter.createLowPass(4, 1, quadratureRate/2, 1500, 1000, 40);
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

		/*
		// DEBUG: write samples to file
		waitCounter++;
		if(debugFile == null && waitCounter > 1000) {
			debugFile = new File("/storage/sdcard0/Download/rdsDebug.iq");
			try {
				debugOutputStream = new BufferedOutputStream(new FileOutputStream(debugFile));
				Log.d(LOGTAG, "doPreFilterWork: writing samples to " + debugFile.getAbsolutePath());
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			}
		}
		int bytesToWrite=500000;
		SamplePacket debugOut = psk31Filtered;
		if(debugBytesWritten < bytesToWrite && debugFile != null) {
			float[] re = debugOut.getRe();
			float[] im = debugOut.getIm();
			try {
				for (int i = 0; i < debugOut.size(); i++) {
					int sample = (int) ((re[i] + 1) * 128f);
					debugOutputStream.write(sample);
					sample = (int) ((im[i] + 1) * 128f);
					debugOutputStream.write(sample);
				}
				debugBytesWritten += debugOut.size() * 2;
				if (debugBytesWritten >= bytesToWrite) {
					debugOutputStream.close();
					Log.d(LOGTAG, "demodulate: File closed!!");
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		*/

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
