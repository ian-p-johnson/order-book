package orderbook.impl;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;
import orderbook.OrderBook;
import orderbook.Side;

import java.util.Arrays;

/**
 * This library EXPLICITLY does not check input arguments because it is intended for
 * use in performance critical environment. Regular java will deal with most situations
 * using null pointer, subscript unchecked exceptions and similar.
 * <p>
 * Some common, easy to check and difficult to trace conditions will be checked but only
 * where the Java standard treatment would likely not pick it up and lead to
 * "soft"/semantic failure
 * <p>
 * This is an intentional tradeoff - please wrap this library if you need more
 *
 * There is also some duplication between iteration and functions in BookUtils
 *
 * The implementation in BookUtils is not allocation free and will require an API
 * change to allow it to be so. For now, we leave allocation free versions here and
 * consolidate them later
 *
 */
@Builder @ToString(onlyExplicitlyIncluded = true)
public class BookDirect implements OrderBook {
    private static class Slab {
        int n;

        Slab(final int n) {
            this.n = n;
            bids = new int[n];
            offers = new int[n];
        }

        int[] bids;
        int[] offers;
    }

    public static final int NO_OFFER = Integer.MAX_VALUE, NO_BID = Integer.MIN_VALUE;

    @Builder.Default @ToString.Include
    private int depth = 10;

    @Builder.Default @ToString.Include
    private int topBidIx = NO_BID, topOfferIx = NO_OFFER;

    private Slab slab;

    @Getter @ToString.Include
    private long symbolId;

    public BookDirect initialiseSlabs() {
        if (symbolId == 0) throw new IllegalArgumentException("symbolId must be specified");
        slab = new Slab(depth);
        return this;
    }

    @Override
    public OrderBook add(final Side side, final int price, final int quantity) {
       //System.out.printf("add %s %d = %d top %d -> %d\n", side, price, quantity, topBidIx, topOfferIx);

        final int maskedPrice = (int)price;

        switch (side) {
            case BID:
                if (topBidIx != NO_BID || quantity != 0) { // Delete on an empty book not a good idea
                    final int[] qty = slab.bids;
                    if (maskedPrice > topBidIx)
                        topBidIx = maskedPrice;
                    qty[maskedPrice] = quantity;
                    if (quantity == 0 && topBidIx == maskedPrice) { // removed head
                        int bidIx = (int)topBidIx;
                        while (qty[bidIx] == 0 && bidIx > 0) bidIx--;
                        topBidIx = qty[bidIx] == 0 ? NO_BID : bidIx;
                    }
                }
                break;
            case OFFER:
                if (topOfferIx != NO_OFFER || quantity != 0) { // Delete on an empty book not a good idea
                    final int[] qty = slab.offers;
                    if (maskedPrice < topOfferIx)
                        topOfferIx = maskedPrice;
                    qty[maskedPrice] = quantity;
                    if (quantity == 0 && topOfferIx == maskedPrice) { // removed head
                        int offerIx = (int)topOfferIx;
                        while (qty[offerIx] == 0 && offerIx < depth) offerIx++;
                        topOfferIx = qty[offerIx] == 0 ? NO_OFFER : offerIx;
                    }
                }
                break;
            default:
                throw new IllegalArgumentException("Side not supported: " + side);
        }
        return this;
    }

    /**
     * This duplicates the iteration loop,but it is, at least, garbage free
     * Consider changing the api to support passed in state
     * An iterater is not garbage free
     * @param side
     * @return depth of book on that side
     */
    @Override
    public int depth(final Side side) { // TODO Consolidate with BookUtils/forEach
        int level = 0;
        switch (side) {
            case BID:
                final int[] bids = slab.bids;
                int bidIx = topBidIx;
                while (bidIx >= 0) {
                    if (bids[bidIx--] != 0)
                        level++;
                }
                break;
            case OFFER:
                final int[] offers = slab.offers;
                int offerIx = topOfferIx;
                while (offerIx < depth) {
                    if (offers[offerIx++] != 0)
                        level++;
                }
                break;
            default:
                throw new IllegalArgumentException("Side not supported: " + side);
        }
        return level;
    }

    @Override
    public void forEach(final Side side, final PriceLevel priceLevel) {
        switch (side) {
            case BID:
                final int[] bids = slab.bids;
                int bidIx = (int)topBidIx;
                while (bidIx >= 0) {
                    final int qty = bids[bidIx];
                    if (qty != 0 && !priceLevel.more(bidIx, qty)) return;
                    bidIx--;
                }
                break;
            case OFFER:
                final int[] offers = slab.offers;
                int offerIx = (int)topOfferIx;
                while (offerIx < depth) {
                    final int qty = offers[offerIx];
                    if (qty != 0 && !priceLevel.more(offerIx, qty)) return;
                    offerIx++;
                }
                break;
            default:
                throw new IllegalArgumentException("Side not supported: " + side);
        }
    }

    /**
     * This duplicates the iteration loop,but it is, at least, garbage free
     * Consider changing the api to support passed in state
     * An iterater is not garbage free
     * @param side
     * @param level - to decend to
     * @param outPrices - 0 based output prices
     * @param outQty - 0 based output quantities
     */
    public void getLevels(final Side side, int level, final int[] outPrices, final int[] outQty) { // TODO Consolidate with BookUtils/forEach
        if (level < 0)
            throw new IllegalArgumentException("level not supported: " + level);

        switch (side) {
            case BID:
                final int[] bids = slab.bids;
                int bidIx = (int)topBidIx;
                int outIx = 0;
                while (level > 0 && bidIx >= 0) {
                    final int qty = bids[bidIx];
                    if (qty != 0) {
                        outPrices[outIx] = bidIx;
                        outQty[outIx] = qty;
                        outIx++;
                        level--;
                    }
                    bidIx--;
                }
                Arrays.fill(outPrices, outIx, outPrices.length, NO_PRICE);
                Arrays.fill(outQty, outIx, outQty.length, 0);
                break;
            case OFFER:
                final int[] offers = slab.offers;
                int offerIx = (int)topOfferIx;
                outIx = 0;
                while (level > 0 && offerIx < depth) {
                    final int qty = offers[offerIx];
                    if (qty != 0) {
                        outPrices[outIx] = offerIx;
                        outQty[outIx] = qty;
                        outIx++;
                        level--;
                    }
                    offerIx++;
                }
                Arrays.fill(outPrices, outIx, outPrices.length, NO_PRICE);
                Arrays.fill(outQty, outIx, outQty.length, 0);
                break;
            default:
                throw new IllegalArgumentException("Side not supported: " + side);
        }
    }

    @Override
    public int getMidPrice() {
        if (topBidIx == NO_BID || topOfferIx == NO_OFFER)
            return NO_PRICE;

        return (topBidIx + topOfferIx) / 2;
    }
    @Override
    public int get(final Side side, final int price) {
        final int maskedPrice = (int)price;
        switch (side) {
            case BID:
                int qty = slab.bids[maskedPrice];
                return qty == 0 ? NO_VALUE : qty;
            case OFFER:
                qty = slab.offers[maskedPrice];
                return qty == 0 ? NO_VALUE : qty;
            default:
                throw new IllegalArgumentException("Side not supported: " + side);
        }
    }

    @Override
    public OrderBook clear() {
        Arrays.fill(slab.bids, 0);
        Arrays.fill(slab.offers, 0);
        topBidIx = NO_BID; topOfferIx = NO_OFFER;
        return this;
    }

}
