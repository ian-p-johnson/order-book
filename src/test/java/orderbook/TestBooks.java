package orderbook;

import orderbook.impl.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class TestBooks {

    @Test
    void testWrapper() {
        final BookDirect direct = BookDirect.builder().symbolId(1).build().initialiseSlabs();
        final BookWrap wrap = BookWrap.wrap(direct)
            .setMaxPrice(999)
            .setMaxQuantity(1000)
            .setMaxLevel(3);

        assertThrows(IllegalArgumentException.class, () -> wrap.add(null, 0, 0));
        assertThrows(IllegalArgumentException.class, () -> wrap.add(Side.BID, -1, 0));
        assertThrows(IllegalArgumentException.class, () -> wrap.add(Side.BID, -0, -1));
        assertThrows(IllegalArgumentException.class, () -> wrap.add(Side.BID, 1_000, 0));
        assertThrows(IllegalArgumentException.class, () -> wrap.add(Side.BID, -0, 1_001));

        assertThrows(IllegalArgumentException.class, () -> wrap.getSizeUpToLevel(null, 9));
        assertThrows(IllegalArgumentException.class, () -> wrap.getSizeUpToLevel(Side.BID, 9));
        assertThrows(IllegalArgumentException.class, () -> wrap.getSizeUpToLevel(Side.BID, -1));

        assertThrows(IllegalArgumentException.class, () -> wrap.depth(null));

        final int[] outBids = new int[10], outOffers = new int[10];
        assertThrows(IllegalArgumentException.class, () -> wrap.getLevels(null, 0, outBids, outOffers));
        assertThrows(IllegalArgumentException.class, () -> wrap.getLevels(Side.BID, -1, outBids, outOffers));
        assertThrows(IllegalArgumentException.class, () -> wrap.getLevels(Side.BID, 9, outBids, outOffers));
        assertThrows(IllegalArgumentException.class, () -> wrap.getLevels(Side.BID, 0, null, outOffers));
        assertThrows(IllegalArgumentException.class, () -> wrap.getLevels(Side.BID, 0, outBids, null));
    }

    private static OrderBook[] bookSource() {
        return new OrderBook[] {
            BookArt.builder().maxIterationLevel(20).build().init(),
            BookDirect.builder().depth(100).symbolId(1).build().initialiseSlabs(),
            BookFastUtil.builder().build(),
        };
    }
    @ParameterizedTest
    @MethodSource("bookSource")
    void testIteration(OrderBook book) {
        final int[] ct = new int[1];

        for (int price = 1; price <= 5; price++) {
            book.add(Side.BID,   50 - price, 10);
            book.add(Side.OFFER, 50 + price, 15);
        }

        assertEquals(5, book.depth(Side.BID));
        assertEquals(5, book.depth(Side.OFFER));

        ct[0] = 0;
        book.forEach(Side.BID, (price, size) -> {
            assertEquals(49 - ct[0], price);
            assertEquals(10, size);
            ct[0]++;
            //System.out.printf("B %d %d\n", price, size);
            return true;
        });

        ct[0] = 0;
        book.forEach(Side.OFFER, (price, size) -> {
            assertEquals(51 + ct[0], price);
            assertEquals(15, size);
            ct[0]++;
            //System.out.printf("O %d %d\n", price, size);
            return true;
        });
        assertEquals(20, BookUtils.getSizeUpToLevel(book, Side.BID, 2));
        assertEquals(30, BookUtils.getSizeUpToLevel(book, Side.OFFER, 2));
        assertEquals(2, BookUtils.getLevelSatisfyingSize(book, Side.BID, 15));
        assertEquals(3, BookUtils.getLevelSatisfyingSize(book, Side.OFFER, 35));
    }

}
