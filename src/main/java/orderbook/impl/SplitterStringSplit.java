package orderbook.impl;

import orderbook.Splitter;

/**
 * A naive String split - slow and allocation noisy
 * Used a reference in testing
 */
public class SplitterStringSplit implements Splitter {
    String[] split;

    @Override
    public int fieldCt() {
        return split.length;
    }
    @Override
    public CharSequence sequence(final int ix) {
        if (ix < 0 || ix >= split.length) return null;
        return split[ix];
    }
    @Override
    public Result split(final String msg, final String delim) {
        split = msg.split(delim);
        return Result.OK;
    }
    public char toChar(final int ix, final ToCharFunction toChar) {
        throw new UnsupportedOperationException("toChar not implemented");
    }
    public long toLong(final int ix, final ToLongFunction toLong) {
        throw new UnsupportedOperationException("toLong not implemented");
    }
    public int toInt(final int ix, final ToIntFunction toInt) {
        throw new UnsupportedOperationException("toInt not implemented");
    }

}
