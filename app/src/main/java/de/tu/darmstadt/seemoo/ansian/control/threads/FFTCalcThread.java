package de.tu.darmstadt.seemoo.ansian.control.threads;

import android.util.Log;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;

import de.greenrobot.event.EventBus;
import de.tu.darmstadt.seemoo.ansian.control.DataHandler;
import de.tu.darmstadt.seemoo.ansian.control.StateHandler;
import de.tu.darmstadt.seemoo.ansian.control.events.FFTDataEvent;
import de.tu.darmstadt.seemoo.ansian.model.FFT;
import de.tu.darmstadt.seemoo.ansian.model.FFTSample;
import de.tu.darmstadt.seemoo.ansian.model.SamplePacket;
import de.tu.darmstadt.seemoo.ansian.model.preferences.Preferences;

/**
 * FFTCalcThread provides the FFT calculator with new samples
 *
 *
 */
public class FFTCalcThread extends Thread {

	@SuppressWarnings("unused")
	private static final String LOGTAG = "FftCalcThread";

	private FFT fftBlock;
	private int oldFFTSize;

	boolean stopRequested = false;

	public FFTCalcThread() {
		fftBlock = new FFT(oldFFTSize = Preferences.MISC_PREFERENCE.getFFTSize());
	}

	@Override
	public void run() {
		ArrayBlockingQueue<SamplePacket> inputQueue = DataHandler.getInstance().getFftInputQueue();
		ArrayBlockingQueue<SamplePacket> inputReturnQueue = DataHandler.getInstance().getFftReturnQueue();
		LinkedBlockingDeque<FFTSample> outputDeque = DataHandler.getInstance().getFftDrawDeque();
		ArrayBlockingQueue<FFTSample> outputReturnQueue = DataHandler.getInstance().getFftDrawReturnQueue();;
		SamplePacket samples = null;
		FFTSample fftSample = null;
		float[] re, im, mag;

		while (!stopRequested) {
			int fFTSize = Preferences.MISC_PREFERENCE.getFFTSize();
			if (oldFFTSize != fFTSize) {
				fftBlock = new FFT(oldFFTSize = fFTSize);
			}

			boolean scanning = StateHandler.isScanning();

			// Grab a output buffer:
			if(!scanning) {
				try {
					fftSample = outputReturnQueue.poll(100, TimeUnit.MILLISECONDS);
					if(fftSample == null)
						continue;
					if(fftSample.getSize() != fFTSize)
						fftSample = new FFTSample(fFTSize); // When user changes the fft size we toss
															// all old buffers and allocate new..
				} catch (InterruptedException e) {
					Log.e(LOGTAG, "run: Interrupted while polling from output return queue. stop.");
					this.stopFFTCalcThread();
					break;
				}
			}
			else {
				fftSample = new FFTSample(fFTSize);
			}

			// fetch the next samples from the queue:
			try {
				samples = inputQueue.poll(100, TimeUnit.MILLISECONDS);
				if (samples == null) {
					Log.d(LOGTAG, "run: Timeout while waiting on input data. skip.");
					if(!scanning)
						outputReturnQueue.offer(fftSample);
					continue;
				}
				// fft size might have changed (by user)
				// replace all buffers in the input queues that do not fit the current size:
				if(samples.size() != fFTSize) {
					// Drop the old sample buffer (will be collected by the GC) and allocate a new:
					samples = new SamplePacket(fFTSize);
					inputReturnQueue.offer(samples);
					if(!scanning)
						outputReturnQueue.offer(fftSample);
					continue;
				}
			} catch (InterruptedException e) {
				Log.e(LOGTAG, "run: Interrupted while polling from input queue. stop.");
				this.stopFFTCalcThread();
				break;
			}

			mag = fftSample.getMagnitudes();
			re = samples.getRe();
			im = samples.getIm();

			// Multiply the samples with a Window function:
			fftBlock.applyWindow(re, im);

			// Calculate the fft:
			fftBlock.fft(re, im);

			// Calculate the logarithmic magnitude:
			float realPower;
			float imagPower;
			int size = samples.size();
			for (int j = 0; j < size; j++) {
				// We have to flip both sides of the fft to draw it centered on
				// the
				// screen:
				int targetIndex = (j + size / 2) % size;

				// Calc the magnitude = log( re^2 + im^2 )
				// note that we still have to divide re and im by the fft size
				realPower = re[j] / fFTSize;
				realPower = realPower * realPower;
				imagPower = im[j] / fFTSize;
				imagPower = imagPower * imagPower;
				mag[targetIndex] = (float) (10 * Math.log10(Math.sqrt(realPower + imagPower)));
			}

			fftSample.setCenterFrequency(samples.getFrequency());
			fftSample.setTimestamp(samples.getTimestamp());
			fftSample.setSamplerate(samples.getSampleRate());

			if(!scanning)
				outputDeque.offerFirst(fftSample);
			else
				DataHandler.getInstance().getScannerBuffer().addSample(fftSample);
			inputReturnQueue.offer(samples);

			EventBus.getDefault().post(new FFTDataEvent(outputDeque.peek()));
		}
	}

	public void stopFFTCalcThread() {
		stopRequested = true;
	}

}
