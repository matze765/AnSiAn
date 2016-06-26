package de.tu.darmstadt.seemoo.ansian.tools;

import de.tu.darmstadt.seemoo.ansian.model.SamplePacket;

/**
 * <h1>AnSiAn - signed 12-bit IQ Converter</h1>
 *
 * Module: Signed12BitIQConverter.java Description: This class implements methods
 * to convert the raw input data of IQ sources (12 bit signed) to SamplePackets.
 * It has also methods to do converting and down-mixing at the same time.
 *
 * The converter actually expects 16-bit signed values (short) which have the
 * least significant 4 bits set to 0.
 *
 * @author Dennis Mantz
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
public class Signed12BitIQConverter extends IQConverter {

	public Signed12BitIQConverter() {
		super();
	}

	@Override
	protected void generateLookupTable() {
		/**
		 * The source delivers samples in the following format: The bytes are
		 * interleaved, 16-bit, signed IQ samples (in-phase component first,
		 * followed by the quadrature component):
		 *
		 *  [---------- first sample -----------]   [-------- second sample --------]
		 *         I                  Q                  I                Q ...
		 *  receivedBytes[0,1] receivedBytes[2,3]   receivedBytes[4,5]      ...
		 */

		lookupTable = new float[4096];
		for (int i = 0; i < 4096; i++)
			lookupTable[i] = (i - 2048) / 2048.0f;
	}

	@Override
	protected void generateMixerLookupTable(int mixFrequency) {
		// If mix frequency is too low, just add the sample rate (sampled
		// spectrum is periodic):
		if (mixFrequency == 0 || (sampleRate / Math.abs(mixFrequency) > MAX_COSINE_LENGTH))
			mixFrequency += sampleRate;

		// Only generate lookupTable if null or invalid:
		if (cosineRealLookupTable == null || mixFrequency != cosineFrequency) {
			cosineFrequency = mixFrequency;
			int bestLength = calcOptimalCosineLength();
			cosineRealLookupTable = new float[bestLength][4096];
			cosineImagLookupTable = new float[bestLength][4096];
			float cosineAtT;
			float sineAtT;
			for (int t = 0; t < bestLength; t++) {
				cosineAtT = (float) Math.cos(2 * Math.PI * cosineFrequency * t / (float) sampleRate);
				sineAtT = (float) Math.sin(2 * Math.PI * cosineFrequency * t / (float) sampleRate);
				for (int i = 0; i < 4096; i++) {
					cosineRealLookupTable[t][i] = (i - 2048) / 2048.0f * cosineAtT;
					cosineImagLookupTable[t][i] = (i - 2048) / 2048.0f * sineAtT;
				}
			}
			cosineIndex = 0;
		}
	}

	@Override
	public int fillPacketIntoSamplePacket(byte[] packet, SamplePacket samplePacket) {
		int capacity = samplePacket.capacity();
		int count = 0;
		int startIndex = samplePacket.size();
		float[] re = samplePacket.getRe();
		float[] im = samplePacket.getIm();
		int reLookupIndex;
		int imLookupIndex;
		for (int i = 0; i < packet.length; i += 4) {
			reLookupIndex = ((packet[i] << 4) | ((packet[i+1] >> 4) & 0x0F)) + 2048;
			imLookupIndex = ((packet[i+2] << 4) | ((packet[i+3] >> 4) & 0x0F)) + 2048;
			re[startIndex + count] = lookupTable[reLookupIndex];
			im[startIndex + count] = lookupTable[imLookupIndex];
			count++;
			if (startIndex + count >= capacity)
				break;
		}
		samplePacket.setSize(samplePacket.size() + count); // update the size of
															// the sample packet
		samplePacket.setSampleRate(sampleRate); // update the sample rate
		samplePacket.setFrequency(frequency); // update the frequency
		return count;
	}

	@Override
	public int mixPacketIntoSamplePacket(byte[] packet, SamplePacket samplePacket, long channelFrequency) {
		int mixFrequency = (int) (frequency - channelFrequency);

		generateMixerLookupTable(mixFrequency); // will only generate table if
												// really necessary

		// Mix the samples from packet and store the results in the samplePacket
		int capacity = samplePacket.capacity();
		int count = 0;
		int startIndex = samplePacket.size();
		float[] re = samplePacket.getRe();
		float[] im = samplePacket.getIm();
		int reLookupIndex;
		int imLookupIndex;
		for (int i = 0; i < packet.length; i += 4) {
			reLookupIndex = ((packet[i] << 4) | ((packet[i+1] >> 4) & 0x0F)) + 2048;
			imLookupIndex = ((packet[i+2] << 4) | ((packet[i+3] >> 4) & 0x0F)) + 2048;
			re[startIndex + count] = cosineRealLookupTable[cosineIndex][reLookupIndex]
					- cosineImagLookupTable[cosineIndex][imLookupIndex];
			im[startIndex + count] = cosineRealLookupTable[cosineIndex][imLookupIndex]
					+ cosineImagLookupTable[cosineIndex][reLookupIndex];
			cosineIndex = (cosineIndex + 1) % cosineRealLookupTable.length;
			count++;
			if (startIndex + count >= capacity)
				break;
		}
		samplePacket.setSize(samplePacket.size() + count); // update the size of
															// the sample packet
		samplePacket.setSampleRate(sampleRate); // update the sample rate
		samplePacket.setFrequency(channelFrequency); // update the frequency
		return count;
	}
}
