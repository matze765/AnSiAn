package de.tu.darmstadt.seemoo.ansian.model.demodulation;

import android.util.Log;
import de.greenrobot.event.EventBus;
import de.tu.darmstadt.seemoo.ansian.control.events.DemodInfoEvent;
import de.tu.darmstadt.seemoo.ansian.model.SamplePacket;

public class RDS extends Demodulation {
    private static final String LOGTAG = "RDS";

    private static final float BAUDRATE = 1187.5f;
    private BPSK bpsk;

    private byte[] bitBuffer;
    private int bitBufferSize = 0;
    private int bitBufferStart = 0;
    private long currentFrequency = -1;

    private static final int INVALIDBLOCK = -1;
    private static final int BLOCKA = 0;
    private static final int BLOCKB = 1;
    private static final int BLOCKC = 2;
    private static final int BLOCKD = 3;
    private static final int BLOCKC2= 4;
    private static final String[] BLOCK_TYPE_NAME = {"A", "B", "C", "D", "C'"};
    private static final int BLOCKSIZE = 16;
    private static final int CHECKWORDSIZE = 10;
    private static final int BLOCKSIZE_W_CHECKWORD = BLOCKSIZE + CHECKWORDSIZE;

    private static final String[] programTypeString = {     "L", "I", "N", "S", "R1", "R2", "R3", "R4", "R5",
                                                            "R6", "R7", "R8", "R9", "R10", "R11", "R12"};
    private static final String[] programTypeCodeString = { "None", "News", "Current Affairs", "Information",
                                                            "Sport", "Education", "Drama", "Cultures", "Science",
                                                            "Varied Speech", "Pop Music", "Rock Music", "Easy Listening",
                                                            "Light Classics M", "Serious Classics", "Other Music"};

    // Current state
    private int groupCounter;
    private int countryCode;
    private int programType;
    private int programReferenceNumber;
    private int TP;
    private int programTypeCode;
    private int TA;
    private int MS;
    private int stereo;
    private int artificialHead;
    private int compressed;
    private int dynPTY;
    private float altFreq1;
    private float altFreq2;
    private char[] programName;
    private int textAB;
    private char[] radioText;

    public RDS() {
        bitBuffer = new byte[2000];     // Just a large buffer, no need for optimization here..
        bpsk = new BPSK(BAUDRATE);
        resetState();
    }

    private void resetState() {
        groupCounter = 0;
        countryCode = -1;
        programType = -1;
        programReferenceNumber = -1;
        TP = -1;
        programTypeCode = -1;
        TA = -1;
        MS = -1;
        stereo = -1;
        artificialHead = -1;
        compressed = -1;
        dynPTY = -1;
        altFreq1 = Float.NaN;
        altFreq2 = Float.NaN;
        textAB = -1;
        programName = new char[8];
        radioText = new char[64];
        for(int i = 0; i < programName.length; i++)
            programName[i] = ' ';
        for(int i = 0; i < radioText.length; i++)
            radioText[i] = ' ';
    }

    private char int2bit(int i) {
        if(i < 0)
            return '-';
        else
            return i == 0 ? '0' : '1';
    }

    public String getStateString() {
        String af = "AF={";
        if(!Float.isNaN(altFreq1))
            af += String.format("%5.1f",altFreq1);
        else
            af += "     ";
        if(!Float.isNaN(altFreq2))
            af += String.format(" %5.1f",altFreq2);
        else
            af += "      ";
        af += "}";

        String str = String.format("#%04d '%s' 0x%02X [TP=%c TA=%c MS=%c stereo=%c artHead=%c comp.=%c dynPTY=%c] %s",
                groupCounter, new String(programName), programReferenceNumber, int2bit(TP), int2bit(TA), int2bit(MS),
                int2bit(stereo), int2bit(artificialHead), int2bit(compressed), int2bit(dynPTY), af
                );
        return str;
    }

    @Override
    public void demodulate(SamplePacket input, SamplePacket output) {
        // if the frequency has changed, we forget our state:
        if(currentFrequency != input.getFrequency()) {
            resetState();
            currentFrequency = input.getFrequency();

            // Clear screen:
            EventBus.getDefault().postSticky(DemodInfoEvent.newReplaceStringEvent(DemodInfoEvent.Position.TOP, "", true));
            EventBus.getDefault().postSticky(DemodInfoEvent.newReplaceStringEvent(DemodInfoEvent.Position.BOTTOM, "", true));
        }

        // Demodulate the new samples
        bitBufferSize += bpsk.demodulate(input, bitBuffer, bitBufferSize);

        while(bitBufferSize-bitBufferStart >= BLOCKSIZE_W_CHECKWORD*4) {
            // Try to decode the bits (check the checksum)
            int blocktype = checkBits(bitBuffer, bitBufferStart);

            // Continue with the next bit if checkword does not match
            if (blocktype < 0) {
                bitBufferStart++;
                continue;
            }

            // If we found a BLOCK A, we can decode it on its own (it is independent of the rest of the group:
            if(blocktype == BLOCKA) {
                String logtext = decodePI(bitBuffer, bitBufferStart);
                bitBufferStart += BLOCKSIZE_W_CHECKWORD;
                //Log.i(LOGTAG, "demodulate: Found Block A " + logtext);
                continue;
            }

            // check if we found a decodeable group (Block B followed by C and D):
            if(blocktype == BLOCKB
                    &&  (  checkBits(bitBuffer, bitBufferStart+BLOCKSIZE_W_CHECKWORD) == BLOCKC
                        || checkBits(bitBuffer, bitBufferStart+BLOCKSIZE_W_CHECKWORD) == BLOCKC2)
                    &&  checkBits(bitBuffer, bitBufferStart+BLOCKSIZE_W_CHECKWORD*2)  == BLOCKD) {
                // Found a decodeable group!
                String logtext = decodeGroup(bitBuffer, bitBufferStart);
                Log.i(LOGTAG, "demodulate: Recv group #" + groupCounter + ": " + logtext);
                Log.d(LOGTAG, "domodulate: ProgName="+new String(programName)+" RadioText="+new String(radioText));

                EventBus.getDefault().postSticky(DemodInfoEvent.newReplaceStringEvent(
                        DemodInfoEvent.Position.TOP, getStateString(), true));
                EventBus.getDefault().postSticky(DemodInfoEvent.newReplaceStringEvent(
                        DemodInfoEvent.Position.BOTTOM, "RadioText: ["+new String(radioText)+"]", true));

                // Next group:
                bitBufferStart += BLOCKSIZE_W_CHECKWORD*3;
            } else {
                // Found a block which is not decodeable on its own:
                /*
                System.out.printf("demodulate: Found BLOCK %s! outside of group : ", BLOCK_TYPE_NAME[blocktype]);
                for(int i = 0; i < BLOCKSIZE; i++)
                    System.out.printf("%d", bitBuffer[bitBufferStart+i]);
                System.out.printf("\n");
                */

                // Next bit
                bitBufferStart++;
            }
        }

        // Check if we have to rearrange the bitbuffer:
        if(bitBufferStart > bitBuffer.length / 2) {
            System.arraycopy(bitBuffer, bitBufferStart, bitBuffer, 0, bitBufferSize - bitBufferStart);
            bitBufferSize -= bitBufferStart;
            bitBufferStart = 0;
        }
    }

    @Override
    public DemoType getType() {
        return null;
    }

    /**
     * Will check if the 26 bits in the array starting at the given offset
     * are a correct block (checksum ok)
     * @param bits  array of bits (one bit per byte)
     * @param idx   index to start
     * @return BlockType indicating the block format or INVALID if no checksum matches
     */
    public int checkBits(byte[] bits, int idx) {
        byte[][] G = {  {0, 0, 0, 1, 1, 1, 0, 1, 1, 1},
                        {1, 0, 1, 1, 1, 0, 0, 1, 1, 1},
                        {1, 1, 1, 0, 1, 0, 1, 1, 1, 1},
                        {1, 1, 0, 0, 0, 0, 1, 0, 1, 1},
                        {1, 1, 0, 1, 0, 1, 1, 0, 0, 1},
                        {1, 1, 0, 1, 1, 1, 0, 0, 0, 0},
                        {0, 1, 1, 0, 1, 1, 1, 0, 0, 0},
                        {0, 0, 1, 1, 0, 1, 1, 1, 0, 0},
                        {0, 0, 0, 1, 1, 0, 1, 1, 1, 0},
                        {0, 0, 0, 0, 1, 1, 0, 1, 1, 1},
                        {1, 0, 1, 1, 0, 0, 0, 1, 1, 1},
                        {1, 1, 1, 0, 1, 1, 1, 1, 1, 1},
                        {1, 1, 0, 0, 0, 0, 0, 0, 1, 1},
                        {1, 1, 0, 1, 0, 1, 1, 1, 0, 1},
                        {1, 1, 0, 1, 1, 1, 0, 0, 1, 0},
                        {0, 1, 1, 0, 1, 1, 1, 0, 0, 1}
                      };
        byte[][] offsetWord = { {0, 0, 1, 1, 1, 1, 1, 1, 0, 0}, // A
                                {0, 1, 1, 0, 0, 1, 1, 0, 0, 0}, // B
                                {0, 1, 0, 1, 1, 0, 1, 0, 0, 0}, // C
                                {0, 1, 1, 0, 1, 1, 0, 1, 0, 0}, // D
                                {1, 1, 0, 1, 0, 1, 0, 0, 0, 0}  // C'
                              };

        // First calculate the raw checksum (matrix multiplication), without the offset word
        byte[] checksum = {0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
        for(int i = 0; i < checksum.length; i++) {
            for(int j = 0; j < G.length; j++)
                checksum[i] += (byte) (bits[idx + j] * G[j][i]);
        }

        // Now add each offset word and see if the checksum matches:
        for(int blocktype = BLOCKA; blocktype <= BLOCKC2; blocktype++) {
            boolean match = true;
            for (int i = 0; i < checksum.length; i++) {
                if( (checksum[i] + offsetWord[blocktype][i])%2 != bits[idx+BLOCKSIZE+i] ) {
                    match = false;
                    break;
                }
            }
            if(match)
                return blocktype;
        }
        return INVALIDBLOCK;
    }

    private int bits2Int(byte[] bits, int start, int len) {
        int value = 0;
        for(int i = 0; i < len; i++)
            if(bits[start+i] > 0)
                value += Math.pow(2,len-i-1);
        return value;
    }

    public String decodePI(byte[] bits, int idx) {
        countryCode = bits2Int(bits, idx, 4);
        programType = bits2Int(bits, idx+4, 4);
        programReferenceNumber = bits2Int(bits, idx+8, 8);

        return String.format("PI=[CC=0x%X PT=%s PRN=0x%02X]",
                countryCode, programTypeString[programType], programReferenceNumber);
    }

    public String decodeGroup(byte[] bits, int idx) {
        int groupTypeCode = bits2Int(bits, idx, 4);
        char groupVersion = bits[idx+4] == 0 ? 'A' : 'B';
        TP = bits[idx+5];
        programTypeCode = bits2Int(bits, idx+6, 4);

        String logtext = String.format("Group %d%c TP=%b PTY=0x%X(%s) ", groupTypeCode,
                groupVersion, TP, programTypeCode, programTypeCodeString[programTypeCode]);

        switch (groupTypeCode) {
            case 0:
                logtext += decodeType0(bits, idx, groupVersion);
                break;

            case 2:
                logtext += decodeType2(bits, idx, groupVersion);
                break;

            default:
                break;
        }

        groupCounter++;
        return logtext;
    }

    public String decodeType0(byte[] bits, int idx, char groupVersion) {
        int idxC = idx+BLOCKSIZE_W_CHECKWORD;
        int idxD = idx+BLOCKSIZE_W_CHECKWORD*2;

        TA = bits[idx+11];
        MS = bits[idx+12];

        String logtext = "TA="+TA+" MS="+MS;

        int segmentAddressCode = bits2Int(bits, idx+14, 2);
        switch (segmentAddressCode) {
            case 3:
                stereo = bits[idx+13];
                logtext += " stereo="+stereo;
                break;
            case 2:
                artificialHead = bits[idx+13];
                logtext += " artificialHead="+artificialHead;
                break;
            case 1:
                compressed = bits[idx+13];
                logtext += " compressed="+compressed;
                break;
            case 0:
                dynPTY = bits[idx+13];
                logtext += " dynPTY="+dynPTY;
                break;
        }

        if(groupVersion == 'B') {
            logtext += " " + decodePI(bits, idxC); // C is PI
        } else {
            // C is alt. frequencies
            int rawFreq1 = bits2Int(bits, idxC, 8);
            int rawFreq2 = bits2Int(bits, idxC+8, 8);
            if(rawFreq1<205) {
                altFreq1 = 87.5f + 0.1f * rawFreq1;
                logtext += " altFreq1="+altFreq1;
            }
            if(rawFreq2<205) {
                altFreq2 = 87.5f + 0.1f *rawFreq2;
                logtext += " altFreq2="+altFreq2;
            }
        }

        char character1 = (char) bits2Int(bits, idxD, 8);
        char character2 = (char) bits2Int(bits, idxD+8, 8);
        logtext += " ProgName["+segmentAddressCode*2+","+(segmentAddressCode*2+1)+"]="+character1+character2;
        programName[segmentAddressCode*2] = character1;
        programName[segmentAddressCode*2+1] = character2;

        return logtext;
    }

    public String decodeType2(byte[] bits, int idx, char groupVersion) {
        int idxC = idx+BLOCKSIZE_W_CHECKWORD;
        int idxD = idx+BLOCKSIZE_W_CHECKWORD*2;

        textAB = bits[idx+11];
        String logtext = "txtAB="+textAB;

        int segmentAddressCode = bits2Int(bits, idx+12, 4);
        char characterD1 = (char) bits2Int(bits, idxD, 8);
        char characterD2 = (char) bits2Int(bits, idxD+8, 8);

        if(groupVersion == 'B') {
            logtext += " " + decodePI(bits, idxC); // C is PI

            logtext += " RadioText["+segmentAddressCode*2+","+(segmentAddressCode*2+1)+"]="+characterD1+characterD2;
            radioText[segmentAddressCode*2] = characterD1;
            radioText[segmentAddressCode*2+1] = characterD2;
        } else {
            // C is radio text
            char characterC1 = (char) bits2Int(bits, idxC, 8);
            char characterC2 = (char) bits2Int(bits, idxC+8, 8);
            logtext += " RadioText["+segmentAddressCode*4+","+(segmentAddressCode*4+1)+","+(segmentAddressCode*4+2)+
                    ","+(segmentAddressCode*4+3)+"]="+characterC1+characterC2+characterD1+characterD2;
            radioText[segmentAddressCode*4]   = characterC1;
            radioText[segmentAddressCode*4+1] = characterC2;
            radioText[segmentAddressCode*4+2] = characterD1;
            radioText[segmentAddressCode*4+3] = characterD2;
        }

        return logtext;
    }
}
