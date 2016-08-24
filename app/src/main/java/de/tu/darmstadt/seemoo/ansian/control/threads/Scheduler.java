package de.tu.darmstadt.seemoo.ansian.control.threads;

import java.util.concurrent.ArrayBlockingQueue;

import android.util.Log;
import de.greenrobot.event.EventBus;
import de.greenrobot.event.Subscribe;
import de.tu.darmstadt.seemoo.ansian.control.DataHandler;
import de.tu.darmstadt.seemoo.ansian.control.events.RecordingEvent;
import de.tu.darmstadt.seemoo.ansian.model.Recording;
import de.tu.darmstadt.seemoo.ansian.model.SamplePacket;
import de.tu.darmstadt.seemoo.ansian.model.demodulation.Demodulation.DemoType;
import de.tu.darmstadt.seemoo.ansian.model.preferences.MiscPreferences;
import de.tu.darmstadt.seemoo.ansian.model.preferences.Preferences;
import de.tu.darmstadt.seemoo.ansian.model.sources.IQSourceInterface;

/**
 * <h1>AnSiAn - Scheduler</h1>
 *
 * Module: Scheduler.java Description: This Thread is responsible for forwarding
 * the samples from the input hardware to the Demodulator and to the Processing
 * Loop and at the correct speed and format. Sample packets are passed to other
 * blocks by using blocking queues. The samples passed to the Demodulator will
 * be shifted to base band first. If the Demodulator or the Processing Loop are
 * to slow, the scheduler will automatically drop incoming samples to keep the
 * buffer of the hackrf_android library from beeing filled up.
 *
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
public class Scheduler extends Thread {
	private boolean stopRequested = true;

	private IQSourceInterface source = null; // Reference to the source of the
												// IQ samples
	private MiscPreferences preferences;
	private Recording recording;
	private static final String LOGTAG = "Scheduler";

	public Scheduler(IQSourceInterface source) {
		setPriority(MAX_PRIORITY);
		preferences = Preferences.MISC_PREFERENCE;
		this.source = source;
		EventBus.getDefault().register(this);
	}

	public void stopScheduler() {
		if (recording != null)
			recording.stopRecordingThread();
		this.stopRequested = true;
		this.source.stopSampling();
	}

	public void start() {
		this.stopRequested = false;
		this.source.startSampling();
		super.start();
	}

	/**
	 * @return true if scheduler is running; false if not.
	 */
	public boolean isRunning() {
		return !stopRequested;
	}

	public boolean isDemodulationActivated() {
		return preferences.getDemodulation() != DemoType.OFF;
	}

	@Override
	public void run() {
		Log.i(LOGTAG, "Scheduler started. (Thread: " + this.getName() + ")");

		long timestamp = System.currentTimeMillis();
		long count = 0;

		SamplePacket fftBuffer = null;
		SamplePacket tmpFlushBuffer = null;	// Just a tmp buffer to flush a queue if necessary
		ArrayBlockingQueue<SamplePacket> demodInputQueue = DataHandler.getInstance().getDemodInputQueue();
		ArrayBlockingQueue<SamplePacket> demodReturnQueue = DataHandler.getInstance().getDemodReturnQueue();
		ArrayBlockingQueue<SamplePacket> wfInputQueue = DataHandler.getInstance().getWfInputQueue();
		ArrayBlockingQueue<SamplePacket> wfReturnQueue = DataHandler.getInstance().getWfReturnQueue();
		ArrayBlockingQueue<SamplePacket> fftInputQueue = DataHandler.getInstance().getFftInputQueue();
		ArrayBlockingQueue<SamplePacket> fftReturnQueue = DataHandler.getInstance().getFftReturnQueue();

		// Check if we get packets from the source.
		// The timeout for the first packet is long because it might take a while to set up the source..
		byte[] firstPacket = source.getPacket(10000);
		if(firstPacket == null) {
			Log.e(LOGTAG, "run: Did not get a packet from source after 10 seconds. Must be an error. stop.");
			stopRequested = true;
		} else {
			// We ignore the first packet, this was just to see if there are packets incoming...
			source.returnPacket(firstPacket);
		}

		while (!stopRequested) {
			// Get a new packet from the source:
			byte[] packet = source.getPacket(1000);

			if (packet == null) {
				Log.e(LOGTAG, "run: No more packets from source. Shutting down...");
				this.stopScheduler();
				break;
			}
			count++;
			long length = packet.length;
			long timetemp = System.currentTimeMillis();
			if (timetemp - timestamp > 1000) {

				// Log.d(LOGTAG, "packetsize " + length+ " samplerate:
				// "+source.getSampleRate());
				// Log.d(LOGTAG, "samples per second: " + ((count * length)*1000
				// / (timetemp - timestamp)) );
				count = 0;
				timestamp = timetemp;
			}

			// /// Recording
			// ////////////////////////////////////////////////////////////////////////
			if (recording != null) {
				recording.write(packet);
			}

			// /// Demodulation
			// /////////////////////////////////////////////////////////////////////
			if (isDemodulationActivated()) {
				SamplePacket demodBuffer = demodReturnQueue.poll();
				if(demodBuffer != null) {
					demodBuffer.setSize(0);    // mark buffer as empty
					source.mixPacketIntoSamplePacket(packet, demodBuffer, Preferences.GUI_PREFERENCE.getDemodFrequency());

					// Provide a copy to the waveform view
					SamplePacket wfBuffer = wfReturnQueue.poll();
					if(wfBuffer != null) {
						demodBuffer.copyTo(wfBuffer);
						wfInputQueue.offer(wfBuffer);
					}

					if(Preferences.GUI_PREFERENCE.isSquelchSatisfied())
						demodInputQueue.offer(demodBuffer);
					else
						demodReturnQueue.offer(demodBuffer);	// squelch not satisfied. put buffer back to the pool.
				} else {
					Log.d(LOGTAG, "run: Flush the demod queue because demodulator is too slow!");
					while ((tmpFlushBuffer = demodInputQueue.poll()) != null)
						demodReturnQueue.offer(tmpFlushBuffer);
				}
			}

			// /// FFT
			// //////////////////////////////////////////////////////////////////////////////
			// If buffer is null we request a new buffer from the fft input queue:
			if(fftBuffer == null) {
				fftBuffer = fftReturnQueue.poll();
				if(fftBuffer != null)
					fftBuffer.setSize(0);	// mark buffer as empty
			}

			// If we got a buffer, fill it!
			if(fftBuffer != null)
			{
				// fill the packet into the buffer:
				source.fillPacketIntoSamplePacket(packet,fftBuffer);

				// check if the buffer is now full and if so: deliver it to the output queue
				if(fftBuffer.capacity() == fftBuffer.size()) {
					fftInputQueue.offer(fftBuffer);
					fftBuffer = null;
				}
				// otherwise we would just go for another round...
			}
			// If buffer was null we currently have no buffer available, which
			// means we
			// simply throw the samples away (this will happen most of the
			// time).

			// Waveform Buffer
			if(!isDemodulationActivated()) {
				SamplePacket wfBuffer = wfReturnQueue.poll();
				if (wfBuffer != null) {
					wfBuffer.setSize(0);
					source.fillPacketIntoSamplePacket(packet, wfBuffer);    // TODO this could possibly be made more efficient
					wfInputQueue.offer(wfBuffer);
				}
			}

			// In both cases: Return the packet back to the source buffer pool:
			source.returnPacket(packet);
		}
		this.stopRequested = true;
		if (recording != null)
		{
			recording.stopRecordingThread();
			recording = null;
		}
		Log.i(LOGTAG, "Scheduler stopped. (Thread: " + this.getName() + ")");

	}

	@Subscribe
	public void onEvent(RecordingEvent event) {
		if (event.isRecording()) {
			recording = event.getRecording();
			recording.startRecordingThread();
		} else if(recording != null) {
			recording.stopRecordingThread();
			recording = null;
		}
	}

}
