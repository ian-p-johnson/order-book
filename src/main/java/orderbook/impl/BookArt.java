package orderbook.impl;

import exchange.core2.collections.art.LongAdaptiveRadixTreeMap;
import exchange.core2.collections.objpool.ObjectsPool;
import orderbook.OrderBook;
import orderbook.Side;
import orderbook.util.MutableBidOffer;
import orderbook.util.MutableInt;
import lombok.Builder;
import lombok.Getter;
import lombok.val;

import java.util.Arrays;
import java.util.HashMap;

/**
 * A wrapper around the Adaptive Radix Tree at
 * <a href="https://github.com/exchange-core/collections">exchange-core/collections</a>
 * This support long keys and Object Values which can be bucketized order lists offering more
 * than one price at a level
 *
 * The Adaptive Radix Tree is a structure particularly well suited to the task of maintaining an order book.
 * Traditional in-memory data structures like balanced binary search trees are not so efficient on modern
 * hardware, because they do not optimally utilize on-CPU caches. An ART combines multiple entries onto nodes
 * so intra-node searches and additions are efficient. This particularly ART implements a long key
 * (ideal for price) and can support multiple entries at a price (using stamp prioritized buckets) It also
 * implements a form of object pooling to reduce the pressure due to garbage generation.
 *
 * The assumption of an object for the value means it will allocate, and the object pooling should help but
 * have yet to make any substantive improvement using the object pooling
 *
 * This implementation also does not expose a flexible iteration mechanism  - either as an iterator or as a
 * predicate based traveral, so for now we limit the traversal and track in the wrapper call
 *
 * Rather that wrapper mitigation, a better approach would be to change the ART to support predicate behaviour
 * for the iteration
 *
 */
@Builder
public class BookArt implements OrderBook {

    /**
     * The ART does not support inverse ordering so all BID keys should be inverted
     */
    @Getter
    private LongAdaptiveRadixTreeMap<Integer> bids;
    @Getter
    private LongAdaptiveRadixTreeMap<Integer> offers;
    @Builder.Default
    private int maxIterationLevel = 100;
    @Builder.Default
    private boolean usePooling = true;

    public BookArt init() {
        if (usePooling) {
            final HashMap<Integer, Integer> poolConfig = new HashMap<>();
            poolConfig.put(ObjectsPool.DIRECT_ORDER, 1024 * 1024);
            poolConfig.put(ObjectsPool.DIRECT_BUCKET, 1024 * 64);
            poolConfig.put(ObjectsPool.ART_NODE_4, 1024 * 32);
            poolConfig.put(ObjectsPool.ART_NODE_16, 1024 * 16);
            poolConfig.put(ObjectsPool.ART_NODE_48, 1024 * 8);
            poolConfig.put(ObjectsPool.ART_NODE_256, 1024 * 4);
            final ObjectsPool objectPool = new ObjectsPool(poolConfig);

            bids = new LongAdaptiveRadixTreeMap<>(objectPool);
            offers = new LongAdaptiveRadixTreeMap<>(objectPool);
        } else {
            bids = new LongAdaptiveRadixTreeMap<>();
            offers = new LongAdaptiveRadixTreeMap<>();
        }

        return this;
    }

    @Override
    public void forEach(final Side side, final PriceLevel priceLevel) {
        forEach(side, priceLevel, maxIterationLevel);
    }
    public void forEach(final Side side, final PriceLevel priceLevel, final int maxLevel) {
        // ART Does not support a Predicate to cease iteration or an interator interface - just forEach bounded
        // by a fixed depth

        // An exception could be thrown as a sanity check, otherwise changes need to be made to the ART
        // to use a predicate rather than a LongConsumer

        // For now we use an allocated completion boolean as a gate + a maxIterationLevel
        // This does define an operation constraint
        final boolean[] more = new boolean[]  {true};    // Ouch!
        switch (side) {
            case BID:
                bids.forEach((price, value) -> {
                    if (more[0])
                        more[0] = priceLevel.more((int)invert(price), value);
                }, maxLevel);
                break;
            case OFFER:
                offers.forEach((price, value) -> {
                    if (more[0])
                        more[0] = priceLevel.more((int)price, value);
                }, maxLevel);
                break;
            default:
                throw new IllegalArgumentException("Side not supported: " + side);
        }
    }

    /*
        Raw keys are inverted in the BIDS ART
     */
    private static long invert(final long key) {
        return -key;
    }

    @Override
    public int get(final Side side, final int price) {
        switch (side) {
            case BID:
                final val retBid = bids.get(invert(price)); // Invert the bid
                return retBid != null ? retBid : NO_VALUE;
            case OFFER:
                final val retOffer = offers.get(price);
                return retOffer != null ? retOffer : NO_VALUE;
            default:
                throw new IllegalArgumentException("Side not supported: " + side);
        }
    }

    @Override
    public OrderBook add(final Side side, int price, final int quantity) {
        final long adjustedPrice = side.equals(Side.BID) ?
            invert(price) :
            price;
        final var map = side.equals(Side.BID) ?
            bids :
            offers;

        if (quantity == 0)
            map.remove(adjustedPrice);
        else
            map.put(adjustedPrice, quantity);
        return this;
    }

    @Override
    public int getMidPrice() {
        if (bids.size(Integer.MAX_VALUE) == 0 || offers.size(Integer.MAX_VALUE) == 0)
            return NO_PRICE;

        final val bidOffer = new MutableBidOffer(); // Ouch
        bids.forEach((price, value) -> {
            price = invert(price);
            bidOffer.bid = price;
        }, 1);
        offers.forEach((price, value) -> {
            bidOffer.offer = price;
        }, 1);
        return (int)(bidOffer.bid + bidOffer.offer) / 2;
    }

    @Override
    public int depth(final Side side) {
        return Side.BID.equals(side) ?
                bids.size(Integer.MAX_VALUE) :
                offers.size(Integer.MAX_VALUE);
    }

    @Override
    public void getLevels(final Side side, final int level, final int[] outPrices, final int[] outQty) {

        final MutableInt ct = new MutableInt(); // Ouch
        forEach(side, (entryPx, entrySize) -> {
            outPrices[ct.value] = (int)entryPx;
            outQty[ct.value++] = entrySize;
            return true;
        }, level);
        Arrays.fill(outPrices, ct.value, outPrices.length, NO_PRICE);
        Arrays.fill(outQty, ct.value, outQty.length, 0);
    }

    @Override
    public OrderBook clear() {
        bids.clear();
        offers.clear();
        return this;
    }
}
