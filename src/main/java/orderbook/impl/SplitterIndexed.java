package orderbook.impl;

import orderbook.Splitter;

import java.nio.CharBuffer;

/**
 * A reasonably efficient garbage free string splitter
 *
 * Uses a pair of delimiters, expecting the message to alternate between them
 *
 * An example of what is being parsed is :
 *      t=1638848595|i=BTC-USD|p=32.99|q=100.00|s=b
 *
 * which is a tagged value representation similar to a simple subset of FIX
 *
 * The indexes stored will be the start/end of each field, so an entry for each
 *   t, 1638848595, i, BTC-USD, p, 32.99, q, 100.00, s, b
 * (10 fields in all)
 *
 */
public class SplitterIndexed implements Splitter {

    int[] split;
    int splitIx;
    String msg;

    public SplitterIndexed(final int maxElements) {
        split = new int[maxElements];
    }

    @Override
    public int fieldCt() {
        return splitIx - 1;
    }

    @Override
    public CharSequence sequence(final int ix) {
        if (ix < 0 || ix >= (splitIx - 1)) return null;
        return msg.subSequence(split[ix], split[ix + 1] - 1);
    }

    public int toInt(final int ix, final ToIntFunction toInt) {
        return toInt.apply(msg, split[ix], split[ix + 1] - 1);
    }
    public long toLong(final int ix, final ToLongFunction toLong) {
        return toLong.apply(msg, split[ix], split[ix + 1] - 1);
    }
//    public long toLongFromChar8(final int ix, ToLongFunction toLong) {
//        return toLong.apply(msg, split[ix], split[ix + 1] - 1);
//    }
    public char toChar(final int ix, final ToCharFunction toChar) {
        return toChar.apply(msg, split[ix], split[ix + 1] - 1);
    }

    public CharSequence sequence(final int ix, final CharBuffer charBuffer) {
        if (ix < 0 || ix >= (splitIx - 1)) return null;
        charBuffer.limit(split[ix + 1] - 1).position(split[ix]);
        return charBuffer;
    }

    @Override
    public Result split(final String msg, final String delim) {
        this.msg = msg;
        int from = splitIx = 0;
        final int len = msg.length();
        split[splitIx++] = 0;
        while (true) {
            final int delimc = delim.charAt(splitIx & 1);
            final int offset = msg.indexOf(delimc, from);
            if (offset == -1) {
                if (from != len) split[splitIx++] = len + 1;
                return Result.OK;
            }
            split[splitIx++] = offset + 1;
            from = offset + 1;
        }
    }
}
