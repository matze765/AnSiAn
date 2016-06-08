package de.tu.darmstadt.seemoo.ansian.model.demodulation;

import android.provider.Settings;
import android.util.Log;

import de.tu.darmstadt.seemoo.ansian.model.SamplePacket;

public class RDS extends Demodulation {
    private static final String LOGTAG = "RDS";

    private static final float BAUDRATE = 1187.5f;
    private BPSK bpsk;

    private byte[] bitBuffer;
    private int bitBufferSize = 0;
    private int bitBufferStart = 0;

    private static final int INVALIDBLOCK = -1;
    private static final int BLOCKA = 0;
    private static final int BLOCKB = 1;
    private static final int BLOCKC = 2;
    private static final int BLOCKD = 3;
    private static final String[] BLOCK_TYPE_NAME = {"A", "B", "C", "D"};
    private static final int BLOCKSIZE = 16;
    private static final int CHECKWORDSIZE = 10;
    private static final int BLOCKSIZE_W_CHECKWORD = BLOCKSIZE + CHECKWORDSIZE;

    public RDS() {
        bitBuffer = new byte[2000];     // Just a large buffer, no need for optimization here..
        bpsk = new BPSK(BAUDRATE);
    }

    @Override
    public void demodulate(SamplePacket input, SamplePacket output) {
        // Demodulate the new samples
        bitBufferSize += bpsk.demodulate(input, bitBuffer, bitBufferSize);

        while(bitBufferSize-bitBufferStart >= BLOCKSIZE_W_CHECKWORD) {
            // Try to decode the bits (check the checksum)
            int blocktype = checkBits(bitBuffer, bitBufferStart);

            // DEBUG:
            if (blocktype < 0) {
                bitBufferStart++;
                continue;
            }

            System.out.printf("demodulate: FOUND BLOCK %s! : ", BLOCK_TYPE_NAME[blocktype]);
            for(int i = 0; i < BLOCKSIZE; i++)
                System.out.printf("%d", bitBuffer[bitBufferStart+i]);
            System.out.printf("\n");


            // Do decoding and process the information... (TODO)


            // Next packet:
            bitBufferStart += BLOCKSIZE_W_CHECKWORD;
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
        for(int blocktype = BLOCKA; blocktype <= BLOCKD; blocktype++) {
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

    public void decodeBlockA(byte[] bits) {

    }

    public byte decodeBlockB(byte[] bits) {
        return 0x00;
    }

    public void decodeBlockC(byte[] bits) {

    }

    public void decodeBlockD(byte[] bits) {

    }
}
