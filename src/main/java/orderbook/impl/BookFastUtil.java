package orderbook.impl;

import orderbook.OrderBook;
import orderbook.Side;
import it.unimi.dsi.fastutil.ints.Int2IntRBTreeMap;
import it.unimi.dsi.fastutil.ints.Int2IntSortedMap;
import it.unimi.dsi.fastutil.ints.IntComparators;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.val;

import java.util.Arrays;

/**
 * An implementation of an Order Book using a FastUtil Int2IntRBTreeMap collection
 * This is an Int2Int collection mapping prices to a quantity at that price
 * fastutil has anothe rcollection that has very similar characteristics
 */
@Builder @AllArgsConstructor
public class BookFastUtil implements OrderBook {
//    public int symbolId;
//    String symbol;

    private @Builder.Default
        Int2IntRBTreeMap bids = new Int2IntRBTreeMap(IntComparators.OPPOSITE_COMPARATOR);
    private @Builder.Default
        Int2IntRBTreeMap offers = new Int2IntRBTreeMap(IntComparators.NATURAL_COMPARATOR);

//    public Histogram histogram;
//    int minDelta = Integer.MAX_VALUE;
//    @Setter
//    private static boolean trackingDelta = true;

    @Override
    public int get(final Side side, final int price) {
        final val levels = getLevels(side);
        final int oldSize = levels.get(price);

        return oldSize > 0 ? oldSize : NO_VALUE;
    }

    @Override
    public OrderBook add(final Side side, final int price, final int quantity) {
        final val levels = getLevels(side);

        final int oldSize = levels.get(price);
        final int newSize = quantity;    // This is a replacement book - some are incremental

        // TODO Ongoing implementation of tracking of price proximity to top distribution
//        if (trackingDelta && levels.size()>0) {
//            final int existTop = levels.firstintKey();
//            final int delta = Math.abs(existTop-price);
//            if (delta>0)
//                minDelta = Math.min(minDelta,  delta);
//            if (histogram!=null)
//                histogram.recordValue(delta);
//            //System.out.printf("delta %,d\n", delta);
//        }
        if (newSize > 0) {
            levels.put(price, newSize);
        } else {
            levels.remove(price);
        }
        return this;
    }

    @Override
    public int depth(final Side side) {
        return side.equals(Side.BID) ?
                bids.size() :
                offers.size();
    }

    @Override
    public void forEach(final Side side, final PriceLevel priceLevel) {
        int price, quantity;
        switch (side) {
            case BID:
                final var bidIter = bids.entrySet().iterator();
                do {
                    final var bid = bidIter.next();
                    price = bid.getKey();
                    quantity = bid.getValue();
                } while (priceLevel.more(price, quantity) && bidIter.hasNext());
                break;
            case OFFER:
                final var offerIter = offers.entrySet().iterator();
                do {
                    final var offer = offerIter.next();
                    price = offer.getKey();
                    quantity = offer.getValue();
                } while (priceLevel.more(price, quantity) && offerIter.hasNext());
                break;
            default:
                throw new IllegalArgumentException("Side not supported: " + side);
        }
    }

    @Override
    public void getLevels(final Side side, int level, final int[] outPrices, final int[] outQty) {
        int outIx = 0;
        final var sideMap = side.equals(Side.BID) ?
            bids :
            offers;
        final var iter = sideMap.entrySet().iterator();
        while (iter.hasNext() && level > 0) {
            final var entry = iter.next();
            outPrices[outIx] = entry.getKey();
            outQty[outIx] = entry.getValue();
            outIx++;
            level--;
        }
        // Fill any entries left unfilled so far
        Arrays.fill(outPrices, outIx, outPrices.length, NO_PRICE);
        Arrays.fill(outQty, outIx, outQty.length, 0);
    }

    @Override
    public int getMidPrice() {
        final var bidTop = getLevels(Side.BID).firstIntKey();
        final var bidOffer = getLevels(Side.OFFER).firstIntKey();
        return (bidTop + bidOffer) / 2;
    }

    private Int2IntSortedMap getLevels(final Side side) {
        return side == Side.BID ? bids : offers;
    }

    @Override
    public OrderBook clear() {
        bids.clear();
        offers.clear();
        return this;
    }
}
