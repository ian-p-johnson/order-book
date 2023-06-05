package orderbook;

import it.unimi.dsi.fastutil.longs.Long2ObjectArrayMap;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import orderbook.impl.BookDirect;
import orderbook.impl.DecoderDedicated;
import orderbook.impl.DecoderGeneric;
import orderbook.impl.SplitterIndexed;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

public class TestDecode {
    Splitter splitIndexed = new SplitterIndexed(20);

    public static final String TEST_MSGS_LONGER[] = {
        "8=FIX.4.4|9=385|" +
                "35=W|34=2|49=CSERVER|50=QUOTE|52=20210621-05:28:59.339|56=demo.icmarkets.8088608|" +
                "55=1|" +
                "268=7|" +
                "269=1|" +
                "270=1.18632|" +
                "271=500000|" +
                "278=74520927|" +
                "269=1|270=1.18631|271=100000|278=74520929|269=1|270=1.1863|271=165000|278=74520928|269=1|" +
                    "270=1.18633|271=5500000|278=74520930|269=0|270=1.1863|271=5000|278=74510596|269=0|" +
                    "270=1.18627|271=3000000|278=74517402|269=0|270=1.18629|271=3260000|278=74519180|10=013"
    };
    public static final String TEST_DELIMSS[] = {
        ",,",
        ",="
    };
    public static final String TEST_MSGS[] = {
        "t=1638848595|i=BTC-USD|p=32.99|q=100.12|s=b",
        "t=2638848596|i=ETH-USD|p=30.97|q=100.11|s=b",
        "t=3638848597|i=SOL-USD|p=34.98|q=100.13|s=b",
        "t=1638848595|i=BTC-USD|p=33.05|q=100.12|s=a",
        "t=2638848596|i=ETH-USD|p=31.02|q=100.11|s=a",
        "t=3638848597|i=SOL-USD|p=35.01|q=100.13|s=a",
        "t=3638848597|i=XXX-USD|p=50.11|q=100.13|s=b",
        // Lets add a little depth on the bid
        "t=1638848595|i=BTC-USD|p=33.00|q=100.12|s=b",
        "t=1638848595|i=BTC-USD|p=32.98|q=100.12|s=b",
        "t=2638848596|i=ETH-USD|p=30.98|q=100.11|s=b",
        "t=2638848596|i=ETH-USD|p=30.96|q=100.11|s=b",
        "t=3638848597|i=SOL-USD|p=34.99|q=100.13|s=b",
        "t=3638848597|i=SOL-USD|p=34.97|q=100.13|s=b",
        "t=3638848597|i=XXX-USD|p=50.14|q=100.13|s=a",
        "t=3638848597|i=XXX-USD|p=50.15|q=100.13|s=a",
        // Lets add a little depth on the offer
        "t=1638848595|i=BTC-USD|p=33.06|q=100.12|s=a",
        "t=1638848595|i=BTC-USD|p=33.04|q=100.12|s=a",
        "t=2638848596|i=ETH-USD|p=31.03|q=100.11|s=a",
        "t=2638848596|i=ETH-USD|p=31.01|q=100.11|s=a",
        "t=3638848597|i=SOL-USD|p=35.02|q=100.13|s=a",
        "t=3638848597|i=SOL-USD|p=35.00|q=100.13|s=a",
    };

    final Decoder decoderGeneric = DecoderGeneric.builder().build();
    final Decoder decoderDedicated = DecoderDedicated.builder().build();

    @EqualsAndHashCode @ToString
    private class Level {
        int price, quantity;
        Level(int price, int quantity) {
            this.price = price;
            this.quantity = quantity;
        }
    }
    private static void verify() {

    }
    @Test
    void testDecodeAndApply() {
        final var decoder = decoderDedicated;
        final var bookMap = new Long2ObjectArrayMap<BookDirect>();
        for (final String msg: TEST_MSGS) {
            final Splitter.Result result = splitIndexed.split(msg, "|=");
            assertEquals(Splitter.Result.OK, result);
            assertEquals(10, splitIndexed.fieldCt());
            decoder.decode(splitIndexed, (stamp, symbolId, side, price, qty) -> {
                final var book = bookMap.computeIfAbsent(symbolId, x -> BookDirect.builder()
                    .symbolId(symbolId)
                    .depth(99_00)
                    .build().initialiseSlabs());
                book.add(side, price, qty);
            });
        }
        final int[] ct = new int[1];
        for (final var book: bookMap.values()) {
            //System.out.printf("book %s\n", book);
            //System.out.printf("%d:%d\n", book.depth(Side.BID), book.depth(Side.OFFER));
            if (book.getSymbolId() == 24866933691470660L) { // Cannot switch on a long
                assertEquals(1, book.depth(Side.BID));
                assertEquals(2, book.depth(Side.OFFER));
            } else {
                assertEquals(3, book.depth(Side.BID));
                assertEquals(3, book.depth(Side.OFFER));
            }
            ct[0] = 0;
            ((BookDirect)book).forEach(Side.BID, (price, size) -> {
                //System.out.printf("B %d = %d\n", price, size);
                final String id = "B:" + String.valueOf(ct[0]);

                if (book.getSymbolId() == 18669995963011908L) { // Cannot switch on a long
                    switch (ct[0]) {
                        case 0: assertEquals(new Level(3300 ,10012), new Level(price, size), id); break;
                        case 1: assertEquals(new Level(3299 ,10012), new Level(price, size), id); break;
                        case 2: assertEquals(new Level(3298 ,10012), new Level(price, size), id); break;
                    }
                } else
                if (book.getSymbolId() == 19514442367980356L) {
                    switch (ct[0]) {
                        case 0: assertEquals(new Level(3098 ,10011), new Level(price, size), id); break;
                        case 1: assertEquals(new Level(3097 ,10011), new Level(price, size), id); break;
                        case 2: assertEquals(new Level(3096 ,10011), new Level(price, size), id); break;
                    }
                } else
                if (book.getSymbolId() == 23449611663659844L) {
                    switch (ct[0]) {
                        case 0:assertEquals(new Level(3499 , 10013), new Level(price, size), id); break;
                        case 1:assertEquals(new Level(3498 , 10013), new Level(price, size), id); break;
                        case 2:assertEquals(new Level(3497 , 10013), new Level(price, size), id); break;
                    }
                } else
                if (book.getSymbolId() == 24866933691470660L) {
                } else {
                    fail ("no symbolId mapping " + book.getSymbolId());
                }
                ct[0]++;
                return true; });
            ct[0] = 0;
            ((BookDirect)book).forEach(Side.OFFER, (price, size) -> {
                //System.out.printf("O %d = %d\n", price, size);
                final String id = "O:" + String.valueOf(ct[0]);

                if (book.getSymbolId() == 18669995963011908L) { // Cannot switch on a long
                    switch (ct[0]) {
                        case 0: assertEquals(new Level(3304,10012), new Level(price, size), id); break;
                        case 1: assertEquals(new Level(3305,10012), new Level(price, size), id); break;
                        case 2: assertEquals(new Level(3306,10012), new Level(price, size), id); break;
                    }
                } else
                if (book.getSymbolId() == 19514442367980356L) {
                    switch (ct[0]) {
                        case 0: assertEquals(new Level(3101,10011), new Level(price, size), id); break;
                        case 1: assertEquals(new Level(3102,10011), new Level(price, size), id); break;
                        case 2: assertEquals(new Level(3103,10011), new Level(price, size), id); break;
                    }
                } else
                if (book.getSymbolId() == 23449611663659844L) {
                    switch (ct[0]) {
                        case 0:assertEquals(new Level(3500, 10013), new Level(price, size), id); break;
                        case 1:assertEquals(new Level(3501, 10013), new Level(price, size), id); break;
                        case 2:assertEquals(new Level(3502, 10013), new Level(price, size), id); break;
                    }
                } else
                if (book.getSymbolId() == 24866933691470660L) {
                } else {
                    fail("no symbolId mapping " + book.getSymbolId());
                }

                ct[0]++;
                return true; });
        }
//        assertThat(stream.limit(8).map(p -> p.bid).collect(Collectors.toList()),
//                Matchers.contains(0, 2, 4, 6, 8, 10, 12, 14));

//        assertEquals(1, book.getLevels(Side.BID).size());
//        assertEquals(0, book.getLevels(Side.OFFER).size());
    }

    @Test
    void testDecode() {
        final int[] ct = new int[1];
        for (final String msg: TEST_MSGS) {
            final Splitter.Result result = splitIndexed.split(msg, "|=");

            assertEquals(Splitter.Result.OK, result);
            assertEquals(10, splitIndexed.fieldCt());

            final var decoder = DecoderGeneric.builder().build();
            decoder.decode(splitIndexed, (stamp, symbolId, side, price, qty) -> {
                // Sample the output
                final String id = String.valueOf(ct[0]) + " " + msg;
                //System.out.printf("%d %d %5s %d = %d\n", stamp, symbolId, side, price, qty);
                switch (ct[0]) {
                    case 0:
                        assertEquals(1638848595L, stamp, id);
                        assertEquals(18669995963011908L, symbolId, id);
                        assertEquals(Side.BID, side, id);
                        assertEquals(3299, price, id);
                        assertEquals(10012, qty, id);
                        break;
                    case 1:
                        assertEquals(2638848596L, stamp, id);
                        assertEquals(19514442367980356L, symbolId, id);
                        assertEquals(Side.BID, side, id);
                        assertEquals(3097, price, id);
                        assertEquals(10011, qty, id);
                        break;
                    case 3:
                        assertEquals(1638848595L, stamp, id);
                        assertEquals(18669995963011908L, symbolId, id);
                        assertEquals(Side.OFFER, side, id);
                        assertEquals(3305, price, id);
                        assertEquals(10012, qty, id);
                        break;
                    case 20:
                        assertEquals(3638848597L, stamp, id);
                        assertEquals(23449611663659844L, symbolId, id);
                        assertEquals(Side.OFFER, side, id);
                        assertEquals(3500, price, id);
                        assertEquals(10013, qty, id);
                        break;
                }
            });
            ct[0]++;
        }
    }

}
