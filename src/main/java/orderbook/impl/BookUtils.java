package orderbook.impl;

import orderbook.OrderBook;
import orderbook.Side;

/**
 * General algorithms to traverse the book according to some criteria
 * Unfortunately, because they use lambda, they cannot easily keep state without
 * allocating so they are not garbage free.
 * One way to avoid this might be to implement delegate management to the caller but it
 * leads to a more awkward API
 */
public class BookUtils {
    private static final int CT = 0;
    private static final int SIZE = 1;

    public static class WorkingSizeUpToLevel {
        int ct, size;
    }
    public static int getSizeUpToLevel(final OrderBook book, final Side side, final int level) {
        return getSizeUpToLevel(book, side, level, new WorkingSizeUpToLevel());
    }
    public static int getSizeUpToLevel(final OrderBook book, final Side side, final int level, final WorkingSizeUpToLevel working) {
        working.ct = working.size = 0;
        book.forEach(side, (price, size) -> {
            //System.out.printf(" %s %d %d\n", side, price, size);
            working.size += size; working.ct++;
            return working.ct < level;
        });
        return working.size;
    }

    public static class WorkingLevelSatisfyingSize {
        int ct, size;
    }

    /**
     * Gets the total size available up to the specified level on that side of the book
     * An empty price level count as 0 size
     * An empty price level does not count to the levels consumed
     * If the side is exhausted, no error will lbe emitted and no further size will be accumulated
     * Two version, one with passed in working space to avoid allocation
     *
     * @param side  to access
     * @param level to decend to
     * @return the total size available - 0 if none
     */
    public static int getLevelSatisfyingSize(final OrderBook book, final Side side, final int quantity) {
        return getLevelSatisfyingSize(book, side, quantity, new WorkingLevelSatisfyingSize());
    }
    public static int getLevelSatisfyingSize(final OrderBook book, final Side side, final int quantity, final WorkingLevelSatisfyingSize working) {
        working.ct = working.size = 0;
        book.forEach(side, (price, size) -> {
            //System.out.printf(" %s %d %d\n", side, price, size);
            working.size += size; working.ct++;
            return working.size < quantity;
        });
        return working.ct;
    }
}

