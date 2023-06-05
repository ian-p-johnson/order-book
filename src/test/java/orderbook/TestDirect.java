package orderbook;

import orderbook.impl.BookDirect;
import orderbook.impl.DecoderGeneric;
import orderbook.impl.SplitterIndexed;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestDirect {

    Splitter splitter = new SplitterIndexed(20);
    DecoderGeneric decoder = DecoderGeneric.builder().build();
    BookDirect book = (BookDirect)BookDirect.builder()
        .depth(10_000)
        .symbolId(1)
        .build()
        .initialiseSlabs();

    // TODO Test deleting last price
    // Or deleting before first price

    public static final String TEST_BASIC[] = {
        "t=1638848595|i=BTC-USD|p=32.99|q=123.00|s=b", // Level 1
        "t=1638848595|i=BTC-USD|p=33.11|q=321.00|s=a", // Level 1
        "t=1638848595|i=BTC-USD|p=32.98|q=102.00|s=b", // Level 2
        "t=1638848595|i=BTC-USD|p=32.97|q=103.00|s=b", // Level 3
        "t=1638848595|i=BTC-USD|p=33.12|q=102.00|s=a", // Level 2
        "t=1638848595|i=BTC-USD|p=33.13|q=103.00|s=a", // Level 3
    };
    @Test
    void testPadding() {
        processMessages(TEST_BASIC);
        final int[] bidPrices = new int[5], bidQty = new int[5];
        final int[] offerPrices = new int[5], offerQty = new int[5];
        Arrays.fill(bidPrices, 0, bidPrices.length, -99);
        Arrays.fill(offerPrices, 0, offerPrices.length, -99);
        book.getLevels(Side.BID, 5, bidPrices, bidQty);
        book.getLevels(Side.OFFER, 5, offerPrices, offerQty);
        for (int level = 3; level < bidPrices.length; level++) {
            assertEquals(BookDirect.NO_PRICE, bidPrices[level], "B:" + String.valueOf(level));
            assertEquals(BookDirect.NO_PRICE, offerPrices[level], "O:" + String.valueOf(level));
        }
//        System.out.printf("bids %s: %s offers %s: %s\n",
//            Arrays.toString(bidPrices), Arrays.toString(bidQty),
//            Arrays.toString(offerPrices), Arrays.toString(offerQty));
    }

    @Test
    void testCleanAdditions() {
        processMessages(TEST_BASIC);
        assertEquals(3, book.depth(Side.BID));
        assertEquals(3, book.depth(Side.OFFER));

        final int[] bidPrices = new int[5], bidQty = new int[5];
        final int[] offerPrices = new int[5], offerQty = new int[5];
        book.getLevels(Side.BID, 5, bidPrices, bidQty);
        book.getLevels(Side.OFFER, 5, offerPrices, offerQty);
//        System.out.printf("bids %s: %s offers %s: %s\n",
//            Arrays.toString(bidPrices), Arrays.toString(bidQty),
//            Arrays.toString(offerPrices), Arrays.toString(offerQty));

        assertEquals(225_00, book.getSizeUpToLevel(Side.BID, 2));
        assertEquals(423_00, book.getSizeUpToLevel(Side.OFFER, 2));

        assertEquals(328_00, book.getSizeUpToLevel(Side.BID, 3));
        assertEquals(526_00, book.getSizeUpToLevel(Side.OFFER, 3));

        assertEquals(3305, book.getMidPrice());
    }

    @Test
    void testTopRemovals() {
        processMessages(TEST_BASIC);

        final int[] bidPrices = new int[5], bidQty = new int[5];
        final int[] offerPrices = new int[5], offerQty = new int[5];
        book.getLevels(Side.BID, 5, bidPrices, bidQty);
        book.getLevels(Side.OFFER, 5, offerPrices, offerQty);

        processMessages(new String[]{
            "t=1638848595|i=BTC-USD|p=32.99|q=0.00|s=b", // Level 1 removed
            "t=1638848595|i=BTC-USD|p=33.11|q=0.00|s=a"  // Level 1 removed
        });

        assertEquals(2, book.depth(Side.BID));
        assertEquals(2, book.depth(Side.OFFER));

        assertEquals(102_00, book.getSizeUpToLevel(Side.BID, 1));
        assertEquals(205_00, book.getSizeUpToLevel(Side.BID, 2)); // Dropped a level
        assertEquals(205_00, book.getSizeUpToLevel(Side.BID, 3)); // Nothing at this level

        assertEquals(102_00, book.getSizeUpToLevel(Side.OFFER, 1));
        assertEquals(205_00, book.getSizeUpToLevel(Side.OFFER, 2)); // Dropped a level
        assertEquals(205_00, book.getSizeUpToLevel(Side.OFFER, 3)); // Nothing at this level
    }

    private void processMessages(final String[] messages) {
        for (final String msg : messages)
            processMessage(msg);
    }

    private void processMessage(final String msg) {
        final Splitter.Result result = splitter.split(msg, "|=");
        decoder.decode(splitter, (stamp, symbolId, side, price, qty) -> book.add(side, price, qty));
    }
}
