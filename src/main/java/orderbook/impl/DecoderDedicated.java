package orderbook.impl;

import orderbook.Decoder;
import orderbook.Side;
import orderbook.Splitter;
import lombok.Builder;

/**
 * A faster decoder, using converters dedicate to the format
 * Brings better performance than a generalised converter
 */
@Builder
public class DecoderDedicated implements Decoder {

    public static final int STAMP = 1;
    public static final int ENTRY_SIZE = 7;
    public static final int ENTRY_PX = 5;
    public static final int SIDE = 9;
    public static final int SYMBOL = 3;

    @Override
    public void decode(final Splitter split, final Add add) {
        final Side side = Decoders.toSide(split.toChar(SIDE, Decoders::toChar));
        final int entrySize = (int)split.toLong(ENTRY_SIZE, Decoders::toLongChar2);
        final int entryPx = (int)split.toLong(ENTRY_PX, Decoders::toLongChar2);
        final long stamp = split.toLong(STAMP, Decoders::toLongStamp10);
        final long symbolId = split.toLong(SYMBOL, Decoders::toLongChar8);
        if (add != null) add.apply(stamp, symbolId, side, entryPx, entrySize);
    }
}
