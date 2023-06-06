package orderbook;

import orderbook.impl.BookFastUtil;
import orderbook.impl.BookUtils;
import orderbook.impl.DecoderGeneric;
import orderbook.impl.SplitterIndexed;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestDecodeAndBook {

    Splitter splitter = new SplitterIndexed(20);
    DecoderGeneric decoder = DecoderGeneric.builder().build();
    OrderBook book = BookFastUtil.builder().build();

    public static final String TEST_BASIC[] = {
        "t=1638848595|i=BTC-USD|p=32.99|q=123.00|s=b", // Level 1
        "t=1638848595|i=BTC-USD|p=33.11|q=321.00|s=a", // Level 1
        "t=1638848595|i=BTC-USD|p=32.98|q=102.00|s=b", // Level 2
        "t=1638848595|i=BTC-USD|p=32.97|q=103.00|s=b", // Level 3
        "t=1638848595|i=BTC-USD|p=33.12|q=102.00|s=a", // Level 2
        "t=1638848595|i=BTC-USD|p=33.13|q=103.00|s=a", // Level 3
    };

    @Test
    void testTopRemovals() {
        processMessages(TEST_BASIC);

        processMessages(new String[]{
            "t=1638848595|i=BTC-USD|p=32.99|q=0.00|s=b", // Level 1 removed
            "t=1638848595|i=BTC-USD|p=33.11|q=0.00|s=a"  // Level 1 removed
        });

        assertEquals(2, book.depth(Side.BID));
        assertEquals(2, book.depth(Side.OFFER));

        assertEquals(102_00, BookUtils.getSizeUpToLevel(book, Side.BID, 1));
        assertEquals(205_00, BookUtils.getSizeUpToLevel(book, Side.BID, 2)); // Dropped a level
        assertEquals(205_00, BookUtils.getSizeUpToLevel(book, Side.BID, 3)); // Nothing at this level

        assertEquals(102_00, BookUtils.getSizeUpToLevel(book, Side.OFFER, 1));
        assertEquals(205_00, BookUtils.getSizeUpToLevel(book, Side.OFFER, 2)); // Dropped a level
        assertEquals(205_00, BookUtils.getSizeUpToLevel(book, Side.OFFER, 3)); // Nothing at this level
    }

    @Test
    void testMidRemovals() {
        processMessages(TEST_BASIC);

        processMessages(new String[]{
            "t=1638848595|i=BTC-USD|p=32.98|q=0.00|s=b", // Level 2 removed
            "t=1638848595|i=BTC-USD|p=33.12|q=0.00|s=a"  // Level 2 removed
        });

        assertEquals(2, book.depth(Side.BID));
        assertEquals(2, book.depth(Side.OFFER));

        assertEquals(123_00, BookUtils.getSizeUpToLevel(book, Side.BID, 1));
        assertEquals(226_00, BookUtils.getSizeUpToLevel(book, Side.BID, 2)); // Dropped a level
        assertEquals(226_00, BookUtils.getSizeUpToLevel(book, Side.BID, 3)); // Nothing at this level

        assertEquals(321_00, BookUtils.getSizeUpToLevel(book, Side.OFFER, 1));
        assertEquals(424_00, BookUtils.getSizeUpToLevel(book, Side.OFFER, 2)); // Dropped a level
        assertEquals(424_00, BookUtils.getSizeUpToLevel(book, Side.OFFER, 3)); // Nothing at this level
    }

    @Test
    void testOverwrite() {
        processMessages(TEST_BASIC);
        processMessages(new String[]{
            "t=1638848595|i=BTC-USD|p=32.97|q=109.00|s=b", // Level 3 mod
            "t=1638848595|i=BTC-USD|p=33.13|q=109.00|s=a", // Level 3 mod
        });

        assertEquals(123_00, BookUtils.getSizeUpToLevel(book, Side.BID, 1));
        assertEquals(321_00, BookUtils.getSizeUpToLevel(book, Side.OFFER, 1));

        assertEquals(225_00, BookUtils.getSizeUpToLevel(book, Side.BID, 2));
        assertEquals(423_00, BookUtils.getSizeUpToLevel(book, Side.OFFER, 2));

        assertEquals(334_00, BookUtils.getSizeUpToLevel(book, Side.BID, 3)); // modified
        assertEquals(532_00, BookUtils.getSizeUpToLevel(book, Side.OFFER, 3)); // modified

        assertEquals(3305, book.getMidPrice());
    }

    @Test
    void testCleanAdditions() {
        processMessages(TEST_BASIC);
        assertEquals(3, book.depth(Side.BID));
        assertEquals(3, book.depth(Side.OFFER));

        assertEquals(12300, BookUtils.getSizeUpToLevel(book, Side.BID, 1));
        assertEquals(32100, BookUtils.getSizeUpToLevel(book, Side.OFFER, 1));

        assertEquals(225_00, BookUtils.getSizeUpToLevel(book, Side.BID, 2));
        assertEquals(423_00, BookUtils.getSizeUpToLevel(book, Side.OFFER, 2));

        assertEquals(328_00, BookUtils.getSizeUpToLevel(book, Side.BID, 3));
        assertEquals(526_00, BookUtils.getSizeUpToLevel(book, Side.OFFER, 3));

        assertEquals(3305, book.getMidPrice());
    }

    @Test
    void testRetrievals() {
        //processMessages(TEST_MSGS);
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
