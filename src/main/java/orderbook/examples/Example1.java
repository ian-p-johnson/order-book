package orderbook.examples;

import orderbook.Side;
import orderbook.impl.*;
import orderbook.OrderBook;
import orderbook.Splitter;
import it.unimi.dsi.fastutil.longs.Long2ObjectArrayMap;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.List;

public class Example1 {
    public static final String TEST_MSGS[] = {
        // Level 1
        "t=1638848595|i=BTC-USD|p=32.90|q=100.12|s=b",
        "t=2638848596|i=ETH-USD|p=33.11|q=100.11|s=b",
        "t=3638848597|i=SOL-USD|p=24.98|q=100.13|s=b",

        "t=1638848595|i=BTC-USD|p=32.95|q=99.12|s=a",
        "t=2638848596|i=ETH-USD|p=33.15|q=99.11|s=a",
        "t=3638848597|i=SOL-USD|p=25.03|q=99.13|s=a",

        // Level 2
        "t=1638848595|i=BTC-USD|p=32.89|q=10.12|s=b",
        "t=2638848596|i=ETH-USD|p=33.10|q=10.11|s=b",
        "t=3638848597|i=SOL-USD|p=24.96|q=10.13|s=b",   // -2

        "t=1638848595|i=BTC-USD|p=32.96|q=9.12|s=a",
        "t=2638848596|i=ETH-USD|p=33.16|q=9.11|s=a",
        "t=3638848597|i=SOL-USD|p=25.05|q=9.13|s=a",    // +2

        // Level 3
        "t=1638848595|i=BTC-USD|p=32.88|q=1.12|s=b",
        "t=2638848596|i=ETH-USD|p=33.07|q=1.11|s=b",    // -3
        "t=3638848597|i=SOL-USD|p=24.91|q=1.13|s=b",    // -5

        "t=1638848595|i=BTC-USD|p=32.97|q=0.12|s=a",
        "t=2638848596|i=ETH-USD|p=33.19|q=0.11|s=a",    // +3
        "t=3638848597|i=SOL-USD|p=25.10|q=0.13|s=a",    // +5

        // Level 2 removal for SOL
        "t=3638848597|i=SOL-USD|p=24.96|q=0.00|s=b",    // -5
        "t=3638848597|i=SOL-USD|p=25.05|q=0.00|s=a",    // +5

        "t=3638848597|i=XXX-USD|p=19.98|q=100.13|s=b",
    };

    public static void main(final String[] args) throws IOException {
        List<String> list = Arrays.asList(TEST_MSGS);
        int levels = 3;
        for (int argIx = 0; argIx < args.length; argIx++)
            if ("-file".equals(args[argIx])) {
                list = FileUtils.readLines(new File(args[++argIx]), Charset.defaultCharset());
            } else
                if ("-levels".equals(args[argIx])) {
                    levels = Integer.parseInt(args[++argIx]);
                }
        final Splitter split = new SplitterIndexed(20);
        final var decoder = DecoderDedicated.builder().build();
        final var bookMap = new Long2ObjectArrayMap<OrderBook>();
        final var symbolMap = new Long2ObjectArrayMap();
        for (final String msg : list)
            try {
                final var result = split.split(msg, "|=");
                decoder.decode(split, (stamp, symbolId, side, price, qty) -> {
                    final var book = bookMap.computeIfAbsent(symbolId, x -> {
                        final String symbol = split.sequence(3).toString();
                        symbolMap.put(symbolId, symbol);
                        final OrderBook newBook = BookArt.builder().maxIterationLevel(99).build().init();
//                      final OrderBook newBook = BookFastUtil.builder().build();
//                        final OrderBook newBook = BookDirect.builder().symbolId(symbolId).depth(1_000_00).build().initialiseSlabs();
                        System.out.printf("symbol %s -> %s\n", symbol, newBook);
                        return newBook;
                    });
                    switch (side) {
                        case BID:
                        case OFFER:
                            book.add(side, price, qty);
                            break;
                        case CLEAR:
                            book.clear();
                            break;
                    }
                });
            } catch (Exception e) {
                System.out.printf("%s\n", msg);
                e.printStackTrace();
                throw e;
            }
        final var working = new BookUtils.WorkingSizeUpToLevel();
        for (final var book: bookMap.values()) {
            //System.out.printf("%s: %s\n", symbolMap.get(book.getSymbolId()),  book);
            final int[] bidPrices = new int[levels], bidQty = new int[levels];
            final int[] offerPrices = new int[levels], offerQty = new int[levels];
            book.getLevels(Side.BID, levels, bidPrices, bidQty);
            book.getLevels(Side.OFFER, levels, offerPrices, offerQty);
            System.out.printf(" bids   %s: %s\n offers %s: %s\n",
                Arrays.toString(bidPrices), Arrays.toString(bidQty),
                Arrays.toString(offerPrices), Arrays.toString(offerQty));
            for (int level = 1; level <= levels; level++) {
                System.out.printf("  L%d ", level);
                System.out.printf("  %d %s ", BookUtils.getSizeUpToLevel(book, Side.BID, level), Side.BID, working);
                System.out.printf("  %d %s\n", BookUtils.getSizeUpToLevel(book, Side.OFFER, level), Side.OFFER, working);
            }
        }
    }
}
