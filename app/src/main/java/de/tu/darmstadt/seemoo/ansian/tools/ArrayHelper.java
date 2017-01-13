package de.tu.darmstadt.seemoo.ansian.tools;

import android.util.Log;

import java.lang.reflect.Array;
import java.util.Arrays;

/**
 * 
 * @author Markus Grau and Steffen Kreis
 * 
 *         provides several array operations
 *
 */
public class ArrayHelper {

	private float[] array;
	private float[] result;
	private int center;

	public ArrayHelper(float[] array) {
		this.array = array;
		center = array.length / 2;
	}

	public float[] getScaledValues(int amount) {
		if (amount != 0) {
			return getScaledValues(amount, 1);
		} else {
			return null;
		}
	}

	private float[] getCenterValues(int range) {
		int lowerPos = center - range;
		int upperPos = center + range;
		// if (lowerPos < 0) {
		// lowerPos = 0;
		// }
		// if (upperPos > array.length) {
		// upperPos = array.length;
		// }
		return Arrays.copyOfRange(array, lowerPos, upperPos);
	}

	public float[] getScaledValues(int amount, float scale) {
		return getScaledValues(amount, scale, 1, true);
	}

	public float[] getScaledValues(int amount, float yScale, boolean cut) {
		return getScaledValues(amount, 1, yScale, cut);
	}

	public float[] getScaledValues(int amount, float scale, float yScale, boolean cut) {
		float[] tempResult = new float[amount]; // new
		if (cut) {
			if (scale != 1) {
				result = getCenterValues((int) ((array.length / scale) / 2));
			} else {
				result = Arrays.copyOf(array, array.length);
				scale *= (float) array.length / amount; // new
			}
		} else {
			result = Arrays.copyOf(array, array.length);
			scale = (float) array.length / amount; // new
			// scale = result.length/amount;
		}
		float max = 0;
		for (int i = 0; i < amount; i++) {
			tempResult[i] = calcAverage(result, Math.round(max), Math.round(max += scale)) * yScale;
		}
		return result = tempResult;
	}

	public float getAverage(int range) {
		float[] temp = Arrays.copyOfRange(array, center - range, center + range);
		return calcAverage(temp);
	}

	public static float calcAverage(float[] temp) {
		return calcAverage(temp, 0, temp.length);
	}

	public static float calcAverage(float[] values, float from, float till) {
		float avg = 0;
		int fromFloor = (int)Math.floor(from);
		int tillFloor = (int)Math.floor(till);

		// Special case: both borders lie in the same value
		if(fromFloor == tillFloor)
			return values[fromFloor];

		avg += (fromFloor+1-from) * values[fromFloor];
		avg += calcAverage(values, fromFloor+1, tillFloor);
		avg += (till-tillFloor) * values[tillFloor];

		return avg / (till-from);
	}

	public static float calcAverage(float[] values, int from, int till) {
		float avg = 0;
		if(from < 0 || till > values.length)
			return 0;
		if (from == till) {
			if(from < values.length)
				return values[from];
			else
				return 0;
		}
		for (int pos = from; pos < till; pos++) {
			avg += values[pos];
		}
		return avg / (till - from);
	}

	public float getAverage() {
		return calcAverage(array);
	}

	/**
	 * @author Steffen Kreis
	 * 
	 *         concatenates two generic arrays of same data type. Does not
	 *         support primitive data types
	 * @param first
	 *            array
	 * @param second
	 * @return concatenated array
	 */
	public static <T> T[] concatenate(T[] first, T[] second) {
		int firstLength = first.length;
		int secondLength = second.length;

		@SuppressWarnings("unchecked")
		T[] res = (T[]) Array.newInstance(first.getClass().getComponentType(), firstLength + secondLength);
		System.arraycopy(first, 0, res, 0, firstLength);
		System.arraycopy(second, 0, res, firstLength, secondLength);

		return res;
	}

	/**
	 * @author Steffen Kreis
	 * 
	 *         concatenates two float arrays. See
	 *         {@link #concatenate(Object[], Object[]) concatenate}. The generic
	 *         method does not support primitive datatypes, for float use this
	 *         one instead.
	 * 
	 * @param first
	 *            array
	 * @param second
	 * @return concatenated array
	 */
	public static float[] concatenate(float[] first, float[] second) {
		int firstLength = first.length;
		int secondLength = second.length;
		float[] res = new float[firstLength + secondLength];
		System.arraycopy(first, 0, res, 0, firstLength);
		System.arraycopy(second, 0, res, firstLength, secondLength);
		return res;
	}


	/**
	 * Multiplies two float arrays. They are expected to have equal length.
	 *
	 * @param firstOperandAndResult the first operand will also be used as result
	 * @param secondOperand second operand of the addition
	 */
	public static void packetMultiply(float[] firstOperandAndResult, float[] secondOperand) {
		for (int i = 0; i < firstOperandAndResult.length; i++) {
			firstOperandAndResult[i] = firstOperandAndResult[i] * secondOperand[i];
		}
	}

	/**
	 * Multiplies each element of a float array with a constant float.
	 *
	 * @param firstOperandAndResult the first operand will also be used as result
	 * @param constant that will be multiplied with each element of the array
	 */
	public static void packetMultiply(float[] firstOperandAndResult, float constant) {
		for (int i = 0; i < firstOperandAndResult.length; i++) {
			firstOperandAndResult[i] = firstOperandAndResult[i] * constant;
		}
	}

	/**
	 * Adds two float arrays. They are expected to have equal length.
	 *
	 * @param firstOperandAndResult the first operand will also be used as result
	 * @param secondOperand second operand of the addition
	 */
	public static void packetAdd(float[] firstOperandAndResult, float[] secondOperand) {
		for (int i = 0; i < firstOperandAndResult.length; i++) {
			firstOperandAndResult[i] = firstOperandAndResult[i] + secondOperand[i];
		}
	}

	/**
	 * Normalizes a packet, such that the highest peak is slightly under 1.0
	 *
	 * @param operandAndResult source and target of the operation
	 */
	public static void packetNormalize(float[] operandAndResult) {

		// get max
		float max = Float.MIN_VALUE;
		for (int i = 0; i < operandAndResult.length; i++) {
			if (operandAndResult[i] > max) {
				max = operandAndResult[i];
			}

		}

		// increase max a bit to prevent 1.0
		max += 0.001;


		// normalize
		for (int i = 0; i < operandAndResult.length; i++) {
			operandAndResult[i] = operandAndResult[i] / max;
		}
	}
	/**
	 * Implements the up sampling by a integer factor
	 * @param buffer the real signal
	 * @param factor integer factor
	 * @return upsampled signal (length = buffer.length*factor)
	 */
	public static float[] upsample(float[] buffer, int factor) {

		float[] result = new float[buffer.length * factor];
		for (int i = 0; i < buffer.length; i++) {
			for (int j = 0; j < factor; j++) {
				result[i * 23 + j] = buffer[i];
			}
		}
		return result;
	}
	/**
	 * Repeats an arbitrary byte array.
	 *
	 * @param array input array that will be repeated
	 * @param repeatCount number of repetitions (0= no repetition)
	 * @return the repeated array (length = array.length * (repeatCount+1)
	 */
	public static byte[] repeat(byte[] array, int repeatCount) {
		repeatCount++;
		byte[] repeated = new byte[array.length * repeatCount];
		for (int i = 0; i < repeatCount; i++) {
			System.arraycopy(array, 0, repeated, i * array.length, array.length);
		}

		return repeated;
	}


}
