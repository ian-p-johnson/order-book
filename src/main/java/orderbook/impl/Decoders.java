package orderbook.impl;

import orderbook.Side;

/**
 * Some common decoder primitives used in the message specific decoders
 */
public class Decoders {
    public static Side toSide(final char ch) {
        switch (ch) {
            case 'b':   return Side.BID;
            case 'a':   return Side.OFFER;
            case 'c':   return Side.CLEAR;
            default:    return Side.UNKNOWN;
        }
    }

    /**
     * Just extracts a character
     *
     * @param c to extract from
     * @param beginIndex within c
     * @param endIndex within c
     * @return extracted char
     */
    public static char toChar(final CharSequence c, final int beginIndex, final int endIndex) {
        final char ch = c.charAt(beginIndex);
        return ch;
    }

    /**
     * Coverts a string of UP TO 8 char into a long
     * Only for use with characters fitting in 1 byte (ASCII)
     * @param c
     * @param beginIndex
     * @param endIndex
     * @return a long that represents the 8 x char
     */
    public static long toLongChar8(final CharSequence c, final int beginIndex, final int endIndex) {
        long result = 0;
        for (int ix = beginIndex; ix < endIndex; ix++) {
            final char ch = c.charAt(ix);
            result = (result << 8) | ch;
            //System.out.printf("ch %d = %c\n", ix, ch);
        }
        return result;
    }
    /**
     * Some string assumptions made
     * There is ALWAYS a "." followed bt 2 digits
     * The string contains[0-9] and ONE decimal point
     * @param c
     * @param beginIndex
     * @param endIndex
     * @return a long that represent the x 100 scale of the fp.2 number
     */
    public static long toLongChar2(final CharSequence c, final int beginIndex, final int endIndex) {
        long result = 0;
        int ix = beginIndex;
        while (true) {
            final char ch = c.charAt(ix++);
            if (ch == '.') break;
            result = (result * 10) + (ch - '0');
        }
        // Skip the dot and unroll the cents
        result = (result * 10) + (c.charAt(ix++) - '0');
        result = (result * 10) + (c.charAt(ix++) - '0');

        return result;
    }

    /**
     *
     * @param c
     * @param beginIndex
     * @param endIndex
     * @return a long that represents a 10 digit stamp
     */
    public static long toLongStamp10(final CharSequence c, final int beginIndex, final int endIndex) {
        long result = 0;
        int ix = beginIndex;
        // for (int _ix=0; _ix<10; _ix++) This was not unrolled, so we do it ourself
        result = (result * 10) + (c.charAt(ix++) - '0');
        result = (result * 10) + (c.charAt(ix++) - '0');
        result = (result * 10) + (c.charAt(ix++) - '0');
        result = (result * 10) + (c.charAt(ix++) - '0');
        result = (result * 10) + (c.charAt(ix++) - '0');
        result = (result * 10) + (c.charAt(ix++) - '0');
        result = (result * 10) + (c.charAt(ix++) - '0');
        result = (result * 10) + (c.charAt(ix++) - '0');
        result = (result * 10) + (c.charAt(ix++) - '0');
        result = (result * 10) + (c.charAt(ix) - '0');

        return result;
    }
}
