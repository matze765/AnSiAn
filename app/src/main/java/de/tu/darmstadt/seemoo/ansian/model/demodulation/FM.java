package de.tu.darmstadt.seemoo.ansian.model.demodulation;

import android.util.Log;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import de.tu.darmstadt.seemoo.ansian.control.threads.Demodulator;
import de.tu.darmstadt.seemoo.ansian.model.SamplePacket;
import de.tu.darmstadt.seemoo.ansian.model.filter.FirFilter;

public class FM extends Demodulation {

	private static final String LOGTAG = "FM";
	private int deviation;
	private SamplePacket demodulatorHistory;
	private SamplePacket rdsBaseband;
	private SamplePacket rdsFiltered;
	private double rdsTime = 0;
	private DemoType type;
	private RDS rds;
	private FirFilter rdsFilter;

	// DEBUG stuff
	private File debugFile = null;
	private BufferedOutputStream debugOutputStream = null;
	private int debugBytesWritten = 0;
	private int waitCounter = 0;
	// DEBUG


	public FM(DemoType type) {
		this.type = type;
		switch (type) {
		case WFM:
			MIN_USER_FILTER_WIDTH = 100000;
			MAX_USER_FILTER_WIDTH = 100000;
			deviation = 75000;
			quadratureRate = 8 * Demodulator.AUDIO_RATE;
			rds = new RDS();
			rdsFilter = FirFilter.createLowPass(4, 20, quadratureRate, 1000, 3000, 40);
			Log.d(LOGTAG, "FM: created new rdsFilter with " + rdsFilter.getNumberOfTaps()
					+ " taps. Decimation=" + rdsFilter.getDecimation() + " Cut-Off="
					+ rdsFilter.getCutOffFrequency() + " transition=" + rdsFilter.getTransitionWidth());
			break;
		case NFM:
			deviation = 5000;
			MIN_USER_FILTER_WIDTH = 3000;
			MAX_USER_FILTER_WIDTH = 15000;
			break;
		default:
			// throw new WrongDemodulationTypeException();

		}

	}

	/**
	 * Will FM demodulate the samples in input. Use ~75000 deviation for wide
	 * band FM and ~3000 deviation for narrow band FM. Demodulated samples are
	 * stored in the real array of output. Note: All samples in output will
	 * always be overwritten!
	 *
	 * @param input
	 *            incoming (modulated) samples
	 * @param output
	 *            outgoing (demodulated) samples
	 */
	@Override
	public void demodulate(SamplePacket input, SamplePacket output) {
		// Note: This will do the frequency demodulation and the
		// demodulation of the RDS signal at 57kHz

		// Step 1: Frequency demodulation:
		fmDemodulate(input, output);

		// Step 2: Downmixing (shift the RDS signal to baseband)
		if(rdsBaseband == null || rdsBaseband.capacity() < input.size()) {
			rdsBaseband = new SamplePacket(input.size());
		}
		float[] rdsBasebandRe = rdsBaseband.getRe();
		float[] rdsBasebandIm = rdsBaseband.getIm();
		float[] outRe = output.getRe();
		double omega = 2*Math.PI*57000;
		double samplePeriod = 1.0 / output.getSampleRate();
		for(int i = 0; i < output.size(); i++) {
			rdsBasebandRe[i] = outRe[i] * (float) Math.cos(omega * rdsTime);
			rdsTime = (rdsTime + samplePeriod) % (float)(2*Math.PI);
		}
		rdsBaseband.setSize(output.size());
		rdsBaseband.setSampleRate(output.getSampleRate());

		// Step 3: Filter ( RDS signal is ~2400Hz wide )
		if(rdsFiltered == null || rdsFiltered.capacity() < rdsBaseband.size() / rdsFilter.getDecimation()) {
			rdsFiltered = new SamplePacket(rdsBaseband.size() / rdsFilter.getDecimation());
		}
		rdsFiltered.setSize(0);
		if (rdsFilter.filterReal(rdsBaseband, rdsFiltered, 0, rdsBaseband.size()) < rdsBaseband.size()) {
			Log.e(LOGTAG, "doPreFilterWork: [rdsFilter] could not filter all samples from input packet.");
		}

		// Step 4: Pass to RDS demodulator
		rds.demodulate(rdsFiltered, null);

		// DEBUG: write samples to file
		/*
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
		int bytesToWrite=1500000;
		if(debugBytesWritten < bytesToWrite && debugFile != null) {
			float[] re = rdsFiltered.getRe();
			float[] im = rdsFiltered.getIm();
			try {
				for (int i = 0; i < rdsFiltered.size(); i++) {
					int sample = (int) ((re[i] + 1) * 128f);
					debugOutputStream.write(sample);
					sample = (int) ((im[i] + 1) * 128f);
					debugOutputStream.write(sample);
				}
				debugBytesWritten += rdsFiltered.size() * 2;

				if (debugBytesWritten >= bytesToWrite) {
					debugOutputStream.close();
					Log.d(LOGTAG, "doPreFilterWork: File closed!!");
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		*/
	}

	@Override
	public DemoType getType() {
		return type;
	}

	@Override
	public boolean needsUserFilter() {
		/*  we don't want the Demodulator to filter with the default user filter because we
		 *  need to do the quadrature demodulation first (on the entire wFM signal)
		 *  Audio filters are fixed, no need for the user to adjust them.
		 */
		return false;
	}

	public void fmDemodulate(SamplePacket input, SamplePacket output) {
		float[] reIn = input.getRe();
		float[] imIn = input.getIm();
		float[] reOut = output.getRe();
		float[] imOut = output.getIm();
		int inputSize = input.size();
		float quadratureGain = quadratureRate / (2 * (float) Math.PI * deviation);

		if (demodulatorHistory == null) {
			demodulatorHistory = new SamplePacket(1);
			demodulatorHistory.getRe()[0] = reIn[0];
			demodulatorHistory.getIm()[0] = reOut[0];
		}

		// Quadrature demodulation:
		reOut[0] = reIn[0] * demodulatorHistory.re(0) + imIn[0] * demodulatorHistory.im(0);
		imOut[0] = imIn[0] * demodulatorHistory.re(0) - reIn[0] * demodulatorHistory.im(0);
		reOut[0] = quadratureGain * (float) Math.atan2(imOut[0], reOut[0]);
		for (int i = 1; i < inputSize; i++) {
			reOut[i] = reIn[i] * reIn[i - 1] + imIn[i] * imIn[i - 1];
			imOut[i] = imIn[i] * reIn[i - 1] - reIn[i] * imIn[i - 1];
			reOut[i] = quadratureGain * (float) Math.atan2(imOut[i], reOut[i]);
		}
		demodulatorHistory.getRe()[0] = reIn[inputSize - 1];
		demodulatorHistory.getIm()[0] = imIn[inputSize - 1];
		output.setSize(inputSize);
		output.setSampleRate(quadratureRate);
	}

}
