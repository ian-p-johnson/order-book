package orderbook.impl;

import orderbook.Decoder;
import orderbook.Side;
import orderbook.Splitter;
import lombok.Builder;
import org.decimal4j.api.DecimalArithmetic;
import org.decimal4j.scale.Scales;

/**
 * This currently uses fixed 2 dp converters- but can easily be fitted with a converter map configured
 * with symbol specific converters
 */
@Builder
public class DecoderGeneric implements Decoder {

    public static final int STAMP = 1;
    public static final int ENTRY_SIZE = 7;
    public static final int ENTRY_PX = 5;
    public static final int SIDE = 9;
    public static final int SYMBOL = 3;

    @Builder.Default
    private DecimalArithmetic df0 = Scales.getScaleMetrics(0).getDefaultArithmetic();
    @Builder.Default
    private DecimalArithmetic df2 = Scales.getScaleMetrics(2).getDefaultArithmetic();

    @Override
    public void decode(final Splitter split, final Add add) {
        final Side side = Decoders.toSide(split.toChar(SIDE, Decoders::toChar));
        final int entrySize = (int)split.toLong(ENTRY_SIZE, df2::parse);
        final int entryPx = (int)split.toLong(ENTRY_PX, df2::parse);
        final long stamp = split.toLong(STAMP, df0::parse);
        final long symbolId = split.toLong(SYMBOL, Decoders::toLongChar8);
        if (add != null) add.apply(stamp, symbolId, side, entryPx, entrySize);
    }
}
