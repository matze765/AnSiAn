package de.tu.darmstadt.seemoo.ansian.model;

import java.util.concurrent.ArrayBlockingQueue;

import de.tu.darmstadt.seemoo.ansian.control.DataHandler;
import de.tu.darmstadt.seemoo.ansian.tools.ArrayHelper;

/**
 * 
 * @author Steffen Kreis
 * 
 *         This adapter is used by the Waveform to get the desired data. In
 *         later releases (with scrolling WaveformView working) it is supposed
 *         to hold the drawing data, so it is not necessary to recalculate that
 *         data as long as no change in scaling/scrolling/'orientation change'
 *         happened
 *
 */
public class WaveformDrawDataAdapter {

	private float[] drawBuffer;	// Buffer to hold the drawing data (scaled and assembled real samples)

	public float[] getDrawArrayRe(int pixel, float xScale, float yScale) {
		if(drawBuffer == null || drawBuffer.length != pixel)
			drawBuffer = new float[pixel];

		ArrayBlockingQueue<SamplePacket> inputQueue = DataHandler.getInstance().getWfInputQueue();
		ArrayBlockingQueue<SamplePacket> returnQueue = DataHandler.getInstance().getWfReturnQueue();
		SamplePacket sampleBuffer = inputQueue.poll();
		if(sampleBuffer == null)
			return null;	// TODO: We should store old drawing data and return it in this case!
		float[] samples = sampleBuffer.getRe();
		float sampleIndex = 0;
		for(int drawIndex = 0; drawIndex < drawBuffer.length; drawIndex++) {

			// Check whether there are enough samples for the next pixel:
			if(sampleBuffer.size() - sampleIndex < xScale) {
				// Not enough samples. We have to swap  the sample buffer

				// First calculate the avg over the remaining samples:
				float tmpAvg = ArrayHelper.calcAverage(samples, sampleIndex, sampleBuffer.size());
				float fraction1 = sampleBuffer.size() - sampleIndex;
				float fraction2 = xScale - fraction1;

				// Swap buffer
				returnQueue.offer(sampleBuffer);
				sampleBuffer = inputQueue.poll();
				if(sampleBuffer == null) {
					return null;	// TODO: We should store old drawing data and return it in this case!
				}
				samples = sampleBuffer.getRe();
				sampleIndex = 0;

				// Calculate the actual avg:
				drawBuffer[drawIndex] = yScale * (tmpAvg*fraction1 + ArrayHelper.calcAverage(samples, sampleIndex, sampleIndex+fraction2)*fraction2) / xScale;
				sampleIndex += fraction2;
			}
			else {
				// Calc average to get next pixel:
				drawBuffer[drawIndex] = yScale * ArrayHelper.calcAverage(samples, sampleIndex, sampleIndex + xScale);
				sampleIndex += xScale;
			}
		}

		// Return buffer
		returnQueue.offer(sampleBuffer);

		return drawBuffer;
	}

}
