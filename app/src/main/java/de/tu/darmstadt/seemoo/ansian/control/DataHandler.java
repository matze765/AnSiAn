package de.tu.darmstadt.seemoo.ansian.control;

import java.util.Arrays;
import java.util.Hashtable;
import java.util.concurrent.ArrayBlockingQueue;

import de.greenrobot.event.EventBus;
import de.greenrobot.event.Subscribe;
import de.tu.darmstadt.seemoo.ansian.control.StateHandler.State;
import de.tu.darmstadt.seemoo.ansian.control.events.FFTDataEvent;
import de.tu.darmstadt.seemoo.ansian.control.events.ScanAreaUpdateEvent;
import de.tu.darmstadt.seemoo.ansian.control.events.SourceEvent;
import de.tu.darmstadt.seemoo.ansian.control.events.StateEvent;
import de.tu.darmstadt.seemoo.ansian.model.FFTDrawData;
import de.tu.darmstadt.seemoo.ansian.model.FFTSample;
import de.tu.darmstadt.seemoo.ansian.model.ObjectRingBuffer;
import de.tu.darmstadt.seemoo.ansian.model.SamplePacket;
import de.tu.darmstadt.seemoo.ansian.model.ScannerBuffer;
import de.tu.darmstadt.seemoo.ansian.model.preferences.Preferences;

/**
 * 
 * DataHandler intermediately stores buffers and other data for further operations
 * 
 * @author Markus Grau
 * @author Steffen Kreis
 *
 */
public class DataHandler {

	private static DataHandler instance;
	private static ObjectRingBuffer<FFTSample> fftBuffer;
	private ScannerBuffer scannerBuffer;
	private Hashtable<Long, FFTSample> lastHash;
	private FFTSample last;
	private FFTSample[] ffts;

	// Define the size of the fft output and input Queues. By setting this value
	// to 2 we basically end up
	// with double buffering. Maybe the two queues are overkill, but it works
	// pretty well like this and
	// it handles the synchronization between the scheduler thread and the
	// processing loop for us.
	// Note that setting the size to 1 will not work well and any number higher
	// than 2 will cause
	// higher delays when switching frequencies.
	private static final int WF_QUEUE_SIZE = 2;
	private static final int FFT_QUEUE_SIZE = 2;
	private static final int DEMOD_QUEUE_SIZE = 20;
	private ArrayBlockingQueue<SamplePacket> wfInputQueue;
	private ArrayBlockingQueue<SamplePacket> wfReturnQueue;
	private ArrayBlockingQueue<SamplePacket> fftInputQueue;
	private ArrayBlockingQueue<SamplePacket> fftReturnQueue;
	private ArrayBlockingQueue<SamplePacket> demodInputQueue;
	private ArrayBlockingQueue<SamplePacket> demodReturnQueue;
	private boolean initialized = false;

	public static DataHandler getInstance() {
		if (instance == null)
			instance = new DataHandler();
		return instance;
	}

	private DataHandler() {
		fftBuffer = new ObjectRingBuffer<FFTSample>(FFTSample.class);
		scannerBuffer = new ScannerBuffer();
		wfInputQueue = new ArrayBlockingQueue<>(WF_QUEUE_SIZE);
		wfReturnQueue = new ArrayBlockingQueue<>(WF_QUEUE_SIZE);
		fftInputQueue = new ArrayBlockingQueue<>(FFT_QUEUE_SIZE);
		fftReturnQueue = new ArrayBlockingQueue<>(FFT_QUEUE_SIZE);
		demodInputQueue = new ArrayBlockingQueue<>(DEMOD_QUEUE_SIZE);
		demodReturnQueue = new ArrayBlockingQueue<>(DEMOD_QUEUE_SIZE);
		EventBus.getDefault().register(this);
	}

	public void initQueues(int packetSize) {
		if(initialized) {
			// Not the first call to this function. We have to clear all
			// queues first!
			// TODO
			throw new RuntimeException("NOT IMPLEMENTED YET");
		}

		// TODO: put fft queue in separate function and call when user changes fft size

		for(int i = 0; i < WF_QUEUE_SIZE; i++)
			wfReturnQueue.offer(new SamplePacket(packetSize));
		for(int i = 0; i < FFT_QUEUE_SIZE; i++)
			fftReturnQueue.offer(new SamplePacket(Preferences.MISC_PREFERENCE.getFFTSize()));
		for(int i = 0; i < DEMOD_QUEUE_SIZE; i++)
			demodReturnQueue.offer(new SamplePacket(packetSize));
	}

	public FFTDrawData getScannerDrawData(int pixelWidth) {
		if (lastHash != null)
			return scannerBuffer.getDrawData(lastHash, pixelWidth);
		else
			return scannerBuffer.getDrawData(pixelWidth);
	}

	public FFTSample[] getSamples(int i) {
		if (ffts != null)
			return Arrays.copyOf(ffts, i);
		return fftBuffer.getLast(i);
	}

	public FFTSample getLastFFTSample() {
		return fftBuffer.getLast();
	}

	public FFTSample[] getSamples() {
		return fftBuffer.getSamples();
	}

	@Subscribe
	public void onEvent(ScanAreaUpdateEvent event) {
		if (scannerBuffer != null) {
			scannerBuffer.setSamplerate(event.getSamplerate());
		}
	}

	@Subscribe
	public void onEvent(FFTDataEvent event) {
		FFTSample sample = event.getSample();
		fftBuffer.add(sample);
		scannerBuffer.addSample(sample);
	}

	@SuppressWarnings("unchecked")
	@Subscribe
	public void onEvent(StateEvent event) {
		if (event.getState() == State.PAUSED) {
			lastHash = (Hashtable<Long, FFTSample>) scannerBuffer.getScannerSamples().clone();
			last = getLastFFTSample();
			ffts = getSamples();
		} else {
			lastHash = null;
			last = null;
			ffts = null;
		}
	}

	@Subscribe
	public void onEvent(SourceEvent event) {
		this.initQueues(event.getSource().getPacketSize());
	}

	public FFTDrawData getFrequencyDrawData(int width) {
		if (last != null)
			return last.getDrawData(width);
		else
			return getDrawData(width);
	}

	public FFTDrawData getWaterfallDrawData(int width) {
		if (last != null && Preferences.GUI_PREFERENCE.isWaterfallPaused())
			return last.getDrawData(width);
		else
			return getDrawData(width);
	}

	private FFTDrawData getDrawData(int width) {
		FFTSample sample = getLastFFTSample();
		if (sample != null)
			return getLastFFTSample().getDrawData(width);
		else
			return null;
	}

	public ArrayBlockingQueue<SamplePacket> getWfInputQueue() {
		return wfInputQueue;
	}

	public ArrayBlockingQueue<SamplePacket> getWfReturnQueue() {
		return wfReturnQueue;
	}

	public ArrayBlockingQueue<SamplePacket> getFftInputQueue() {
		return fftInputQueue;
	}

	public ArrayBlockingQueue<SamplePacket> getFftReturnQueue() {
		return fftReturnQueue;
	}

	public ArrayBlockingQueue<SamplePacket> getDemodInputQueue() {
		return demodInputQueue;
	}

	public ArrayBlockingQueue<SamplePacket> getDemodReturnQueue() {
		return demodReturnQueue;
	}
}
