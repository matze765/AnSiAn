package de.tu.darmstadt.seemoo.ansian.control.threads;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;

import android.util.Log;
import de.tu.darmstadt.seemoo.ansian.model.SamplePacket;
import de.tu.darmstadt.seemoo.ansian.model.filter.FirFilter;
import de.tu.darmstadt.seemoo.ansian.model.preferences.Preferences;

/**
 * <h1>AnSiAn - Decimator</h1>
 *
 * Module: Decimator.java Description: This class implements a decimation block
 * used to downsample the incoming signal to the sample rate used by the
 * demodulation routines. It will run in a separate thread.
 *
 * @author Dennis Mantz
 * @author Markus Grau
 * @author Steffen Kreis
 *
 *         Copyright (C) 2014 Dennis Mantz License:
 *         http://www.gnu.org/licenses/gpl.html GPL version 2 or higher
 *
 *         This library is free software; you can redistribute it and/or modify
 *         it under the terms of the GNU General Public License as published by
 *         the Free Software Foundation; either version 2 of the License, or (at
 *         your option) any later version.
 *
 *         This library is distributed in the hope that it will be useful, but
 *         WITHOUT ANY WARRANTY; without even the implied warranty of
 *         MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 *         General Public License for more details.
 *
 *         You should have received a copy of the GNU General Public License
 *         along with this library; if not, write to the Free Software
 *         Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA
 *         02110-1301 USA
 */
public class Decimator extends Thread {
	private int outputSampleRate; // sample rate at the output of the decimator
									// block

	private boolean stopRequested = true;
	private static final String LOGTAG = "Decimator";
	private int performanceSelector = -1;

	private static final int OUTPUT_QUEUE_SIZE = 2; // Double Buffer
	private ArrayBlockingQueue<SamplePacket> inputQueue; // queue that holds the incoming sample packets
	private ArrayBlockingQueue<SamplePacket> inputReturnQueue; // queue to return used buffers from the input queue
	private ArrayBlockingQueue<SamplePacket> outputQueue; // queue that will hold the decimated sample packets
	private ArrayBlockingQueue<SamplePacket> outputReturnQueue; // queue to return used buffers from the output queue

	// DOWNSAMPLING:
	private static final int INPUT_RATE = 1000000; // For now, this decimator
													// only works with a fixed
													// input rate of 1Msps
	private FirFilter inputFilter1 = null;
	private FirFilter inputFilter2 = null;
	private SamplePacket tmpDownsampledSamples;

	/**
	 * Constructor. Will create a new Decimator block.
	 *
	 * @param outputSampleRate
	 *            // sample rate to which the incoming samples should be
	 *            decimated
	 * @param packetSize
	 *            // packet size of the incoming sample packets
	 * @param inputQueue
	 *            // queue that delivers incoming sample packets
	 * @param inputReturnQueue
	 *            // queue to return used input sample packets
	 */
	public Decimator(int outputSampleRate, int packetSize, ArrayBlockingQueue<SamplePacket> inputQueue,
					 ArrayBlockingQueue<SamplePacket> inputReturnQueue) {
		this.outputSampleRate = outputSampleRate;
		this.inputQueue = inputQueue;
		this.inputReturnQueue = inputReturnQueue;

		// Create output queues:
		this.outputQueue = new ArrayBlockingQueue<>(OUTPUT_QUEUE_SIZE);
		this.outputReturnQueue = new ArrayBlockingQueue<>(OUTPUT_QUEUE_SIZE);
		for (int i = 0; i < OUTPUT_QUEUE_SIZE; i++)
			outputReturnQueue.offer(new SamplePacket(packetSize));

		// Create local buffers:
		this.tmpDownsampledSamples = new SamplePacket(packetSize);
	}

	public int getOutputSampleRate() {
		return outputSampleRate;
	}

	public void setOutputSampleRate(int outputSampleRate) {
		this.outputSampleRate = outputSampleRate;
	}

	public SamplePacket getDecimatedPacket(int timeout) {
		try {
			return outputQueue.poll(timeout, TimeUnit.MILLISECONDS);
		} catch (InterruptedException e) {
			Log.e(LOGTAG, "getPacket: Interrupted while waiting on queue");
			return null;
		}
	}

	public void returnDecimatedPacket(SamplePacket packet) {
		outputReturnQueue.offer(packet);
	}

	@Override
	public synchronized void start() {
		this.stopRequested = false;
		super.start();
	}

	public void stopDecimator() {
		this.stopRequested = true;
	}

	@Override
	public void run() {

		SamplePacket inputSamples;
		SamplePacket outputSamples;

		Log.i(LOGTAG, "Decimator started. (Thread: " + this.getName() + ")");

		while (!stopRequested) {
			// Check whether the Filters are still set up correctly
			if(performanceSelector != Preferences.MORSE_PREFERENCE.getPerformanceSelector()) {
				performanceSelector = Preferences.MORSE_PREFERENCE.getPerformanceSelector();

				// Create FirFilters with tabs according to performance level
				float cutoff = 75000 + 2500*performanceSelector;
				float transWidth = 75000 - 5000*performanceSelector;
				float attenuation = 20 + 4 * performanceSelector;
				inputFilter1 = FirFilter.createLowPass(4, 1, 1000000, cutoff, transWidth, attenuation);
				Log.d(LOGTAG, "run: created new inputFilter1 with " + inputFilter1.getNumberOfTaps()
								+ " taps. Decimation=" + inputFilter1.getDecimation() + " Cut-Off="
								+ inputFilter1.getCutOffFrequency() + " transition=" + inputFilter1.getTransitionWidth());

				cutoff = 18750 + 625*performanceSelector;
				transWidth = 18750 - 1250*performanceSelector;
				inputFilter2 = FirFilter.createLowPass(4, 1, 250000, cutoff, transWidth, attenuation);
				Log.d(LOGTAG, "run: created new inputFilter2 with " + inputFilter2.getNumberOfTaps()
						+ " taps. Decimation=" + inputFilter2.getDecimation() + " Cut-Off="
						+ inputFilter2.getCutOffFrequency() + " transition=" + inputFilter2.getTransitionWidth());
			}

			// Get a packet from the input queue:
			try {
				inputSamples = inputQueue.poll(1000, TimeUnit.MILLISECONDS);
			} catch (InterruptedException e) {
				Log.e(LOGTAG, "run: Interrupted while waiting on input queue! stop.");
				this.stopRequested = true;
				break;
			}

			// Verify the input sample packet is not null:
			if (inputSamples == null) {
				// Log.d(LOGTAG,
				// "run: Input sample is null. skip this round...");
				continue;
			}

			// Verify the input sample rate: (For now, this decimator only works
			// with a fixed input rate of 1Msps)
			if (inputSamples.getSampleRate() != INPUT_RATE) {
				Log.d(LOGTAG, "run: Input sample rate is " + inputSamples.getSampleRate() + " but should be"
						+ INPUT_RATE + ". skip.");
				continue;
			}

			// Get a packet from the output queue:
			try {
				outputSamples = outputReturnQueue.poll(1000, TimeUnit.MILLISECONDS);
			} catch (InterruptedException e) {
				Log.e(LOGTAG, "run: Interrupted while waiting on output return queue! stop.");
				this.stopRequested = true;
				break;
			}

			// Verify the output sample packet is not null:
			if (outputSamples == null) {
				Log.d(LOGTAG, "run: Output sample is null. skip this round...");
				// return inputSamples back to the input queue:
				inputReturnQueue.offer(inputSamples);
				continue;
			}

			// downsampling
			downsampling(inputSamples, outputSamples);

			// return inputSamples back to the input queue:
			inputReturnQueue.offer(inputSamples);

			// deliver the outputSamples to the output queue
			outputQueue.offer(outputSamples);
		}

		this.stopRequested = true;
		Log.i(LOGTAG, "Decimator stopped. (Thread: " + this.getName() + ")");
	}

	/**
	 * Will decimate the input samples to the outputSampleRate and store them in
	 * output
	 *
	 * @param input
	 *            incoming samples at the incoming rate (input rate)
	 * @param output
	 *            outgoing (decimated) samples at output rate (quadrature rate)
	 * @return
	 */
	private SamplePacket downsampling(SamplePacket input, SamplePacket output) {
		if(outputSampleRate == input.getSampleRate()/4) {
			// apply only the first filter (decimate to INPUT_RATE/4)
			output.setSize(0); // mark buffer as empty
			if (inputFilter1.filter(input, output, 0, input.size()) < input.size()) {
				Log.e(LOGTAG, "downsampling: [inputFilter1] could not filter all samples from input packet.");
			}
		} else {
			// apply the first and the second filter (decimate to INPUT_RATE/16)
			tmpDownsampledSamples.setSize(0); // mark buffer as empty
			if (inputFilter1.filter(input, tmpDownsampledSamples, 0, input.size()) < input.size()) {
				Log.e(LOGTAG, "downsampling: [inputFilter1] could not filter all samples from input packet.");
			}
			output.setSize(0); // mark buffer as empty
			if (inputFilter2.filter(tmpDownsampledSamples, output, 0, tmpDownsampledSamples.size()) < tmpDownsampledSamples.size()) {
				Log.e(LOGTAG, "downsampling: [inputFilter2] could not filter all samples from input packet.");
			}
		}
		return output;
	}
}
