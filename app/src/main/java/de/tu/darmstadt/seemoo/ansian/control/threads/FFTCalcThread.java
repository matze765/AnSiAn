package de.tu.darmstadt.seemoo.ansian.control.threads;

import java.util.concurrent.ArrayBlockingQueue;

import de.greenrobot.event.EventBus;
import de.tu.darmstadt.seemoo.ansian.control.DataHandler;
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
	private int FFT_QUEUE_SIZE = 5;

	boolean stopRequested = false;
	private ArrayBlockingQueue<FFTSample> outputQueue;

	public FFTCalcThread() {
		fftBlock = new FFT(oldFFTSize = Preferences.MISC_PREFERENCE.getFFTSize());
		outputQueue = new ArrayBlockingQueue<FFTSample>(FFT_QUEUE_SIZE);

	}

	@Override
	public void run() {
		ArrayBlockingQueue<SamplePacket> inputQueue = DataHandler.getInstance().getFftInputQueue();
		ArrayBlockingQueue<SamplePacket> inputReturnQueue = DataHandler.getInstance().getFftReturnQueue();

		while (!stopRequested) {
			int fFTSize = Preferences.MISC_PREFERENCE.getFFTSize();
			if (oldFFTSize != fFTSize) {
				fftBlock = new FFT(oldFFTSize = fFTSize);
			}
			SamplePacket samples = null;

			while (inputQueue.isEmpty()) {
				try {
					sleep(10);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}

			float[] mag = new float[fFTSize];
			float[] re, im;

			samples = inputQueue.poll();
			re = samples.getRe().clone();
			im = samples.getIm().clone();

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
			outputQueue.offer(new FFTSample(samples, mag));
			inputReturnQueue.offer(samples);

			EventBus.getDefault().post(new FFTDataEvent(outputQueue.poll()));
		}
	}

	public void stopFFTCalcThread() {
		stopRequested = true;
	}

}
