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

    public static int getSizeUpToLevel(final OrderBook book, final Side side, final int level) {
        final int[] v = new int[2]; // Anonymously named for a single allocation
        book.forEach(side, (price, size) -> {
            //System.out.printf(" %s %d %d\n", side, price, size);
            v[SIZE] += size; v[CT]++;
            return v[CT] < level;
        });
        return v[SIZE];
    }
    public static int getLevelSatisfyingSize(final OrderBook book, final Side side, final int quantity) {
        final int[] v = new int[2]; // // Anonymously named for a single allocation
        book.forEach(side, (price, size) -> {
            //System.out.printf(" %s %d %d\n", side, price, size);
            v[SIZE] += size; v[CT]++;
            return v[SIZE] < quantity;
        });
        return v[CT];
    }
}

