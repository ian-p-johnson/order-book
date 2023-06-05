package orderbook;

import it.unimi.dsi.fastutil.longs.Long2ObjectArrayMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import orderbook.impl.*;
import orderbook.tools.OrderSet;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.infra.Blackhole;

import java.util.Map;

public class DecodeAndBookBenchmark {
//    @OutputTimeUnit(TimeUnit.MILLISECONDS)
//    @Threads(value = THREAD_COUNT)
//    @Warmup(iterations = WARMUP_COUNT)
//    @Fork(value = FORK_COUNT)
//    @Measurement(iterations = ITERATION_COUNT)

    public static final String TEST_BASIC[] = {
        "t=1638848595|i=BTC-USD|p=32.99|q=123.00|s=b", // Level 1
        "t=1638848595|i=BTC-USD|p=33.11|q=321.00|s=a", // Level 1
        "t=1638848595|i=BTC-USD|p=32.98|q=102.00|s=b", // Level 2
        "t=1638848595|i=BTC-USD|p=32.97|q=103.00|s=b", // Level 3
        "t=1638848595|i=BTC-USD|p=33.12|q=102.00|s=a", // Level 2
        "t=1638848595|i=BTC-USD|p=33.13|q=103.00|s=a", // Level 3
    };
    public static final String TEST_REMOVE1[] = {
        "t=1638848595|i=BTC-USD|p=32.98|q=0.00|s=b", // Level 2 removed
        "t=1638848595|i=BTC-USD|p=33.12|q=0.00|s=a"  // Level 2 removed
    };
    public static final String TEST_UPDATE1[] = {
        "t=1638848595|i=BTC-USD|p=32.97|q=109.00|s=b", // Level 3 mod
        "t=1638848595|i=BTC-USD|p=33.13|q=109.00|s=a", // Level 3 mod
    };

    public static final String TEST_DELIMSS[] = {
        ",,",
        ",="
    };

    public enum Book {
        FASTUTIL,
        DIRECT,
        ART, ARTXPOOL,
        NULL;
    }

    public enum Split {
        INDEX,
        SPLIT;
    }

    public enum Decode {
        GENERIC,
        DEDICATED;
    }

    static String[] symbolUniverse = new String[]{"BTC-USD", "ETH-USD", "SOL-USD"};

    @State(Scope.Benchmark)
    public static class MyStateBook {
        @Param({"FASTUTIL", "DIRECT", "ART", "NULL"})
        public Book book;

        Splitter splitter = new SplitterIndexed(20);
        DecoderGeneric decoder = DecoderGeneric.builder().build();
        Long2ObjectMap<OrderBook> bookMap = new Long2ObjectArrayMap<>();
        OrderBook bookLong2Long = BookFastUtil.builder().build();
        OrderBook bookNull = BookNull.builder().build();
        OrderBook bookART = BookArt.builder().usePooling(true).build().init();
        OrderBook bookARTNoPool = BookArt.builder().usePooling(false).build().init();
        BookDirect bookDirect = (BookDirect)BookDirect.builder()
            .depth(10_000)
            .symbolId(1)
            .build().initialiseSlabs();
        Map<Integer, Integer> type40_20_30 = Map.of(
            0, 40,  // 40 / Add
            1, 20,      // 20 / Change
            2, 30);
        Map<Integer, Integer> symbols = Map.of(
            0, 1,  // 30% BTC-USD
            1, 1,      // 30% ETH-USD
            2, 1);     // 30% SOL-USD
        OrderSet price_density_2_1k = OrderSet.builder()
            .typeDistribution(type40_20_30)     // 30 / Remove
            .priceDistribution(
            Map.of(
            10, 40, // 40% / 10
            1_00, 40,   // 40% / 100
            5_00, 15,   // 15% / 500
            40_00, 5))  //  5% / 1000
            .symbolDistribution(symbols)
            .symbols(symbolUniverse)
            .priceBase(5_000).priceRange(4_000)
            .qtyBase(10_00).qtyRange(1_00)
            .n(1_000)
            .build().generate();
        OrderSet getPrice_density_1_1k_1k = OrderSet.builder()
            .typeDistribution(type40_20_30)     // 30 / Remove
            .priceDistribution(Map.of(
            16, 80, // 80 / 16
            1_00, 30,   // 30 / 100
            5_00, 15,   // 15 / 500
            40_00, 5))  //  5 / 1000
            .symbolDistribution(symbols)
            .symbols(symbolUniverse)
            .priceBase(5_000).priceRange(4_000)
            .qtyBase(10_00).qtyRange(1_00)
            .n(1_000)
            .build().generate();
    }

    @State(Scope.Benchmark)
    public static class MyStateSplit {
        @Param({"INDEX", "SPLIT"})
        public Split split;

        Splitter splitIndexed = new SplitterIndexed(20);
        Splitter splitString = new SplitterStringSplit();
        DecoderGeneric decoder = DecoderGeneric.builder().build();
    }

    @State(Scope.Benchmark)
    public static class MyStateDecode {
        @Param({"GENERIC", "DEDICATED"})
        public Decode decode;

        Splitter splitter = new SplitterIndexed(20);
        Decoder decoderGeneric = DecoderGeneric.builder().build();
        Decoder decoderDedicated = DecoderDedicated.builder().build();
    }

//    @Benchmark
//    public void testSet(MyStateBook state, Blackhole blackhole) {
//        OrderSet prices = state.price_density_2_1k;
//        selectBook(state);
//        long sum = 0;
//        for (int ix=0; ix<prices.n; ix++) {
//            sum += prices.prices[ix];
//            sum += prices.quantities[ix]
//        }
//        blackhole.consume(sum);
//    }

    private static OrderBook selectBook(MyStateBook state) {
        switch (state.book) {
            case NULL:
                return state.bookNull;
            case ART:
                return state.bookART;
            case ARTXPOOL:
                return state.bookARTNoPool;
            case DIRECT:
                return state.bookDirect;
            case FASTUTIL:
                return state.bookLong2Long;
            default:
                throw new IllegalStateException("Invalid book:" + state.book);
        }
    }

    private static Splitter selectSplitter(MyStateSplit state) {
        switch (state.split) {
            case INDEX:
                return state.splitIndexed;
            case SPLIT:
                return state.splitString;
            default:
                throw new IllegalStateException("Invalid split:" + state.split);
        }
    }

    private static Decoder selectDecode(MyStateDecode state) {
        switch (state.decode) {
            case GENERIC:
                return state.decoderGeneric;
            case DEDICATED:
                return state.decoderDedicated;
            default:
                throw new IllegalStateException("Invalid decode:" + state.decode);
        }
    }

    @Benchmark
    public void testRawMixed1k(MyStateBook state, Blackhole blackhole) {
        final OrderSet prices = state.price_density_2_1k;
        OrderBook book = selectBook(state);
//        OrderBook book = state.bookSlab;
        for (int ix = 0; ix < prices.n; ix++) {
            //book = state.bookMap.computeIfAbsent(prices.symbolIds[ix], x -> BookFastUtil.builder().build());
            book.add(prices.prices[ix] > 5000 ? Side.BID : Side.OFFER,
                prices.prices[ix], prices.quantities[ix]);
        }
//        System.out.printf("%3s:%3s\n", "B", "O");
//        for (var _book: state.bookMap.values())
//            System.out.printf("%3d:%3d\n", _book.depth(Side.BID), _book.depth(Side.BID));
        final int result = book.getMidPrice();
        blackhole.consume(result);

//        System.out.printf("depth %,d:%,d top %d:%d\n",
//                book.depth(Side.BID), book.depth(Side.OFFER),
//                book.getSizeUpToLevel(Side.BID, 1), book.getSizeUpToLevel(Side.OFFER, 1)
//        );
//
//        int levels = 5;
//        long[] bidPrices = new long[levels], bidQty = new long[levels];
//        long[] offerPrices = new long[levels], offerQty = new long[levels];
//        book.getLevels(Side.BID, levels, bidPrices, bidQty);
//        book.getLevels(Side.OFFER, levels, offerPrices, offerQty);
//        System.out.printf("bids %s: %s offers %s: %s\n",
//                Arrays.toString(bidPrices), Arrays.toString(bidQty),
//                Arrays.toString(offerPrices), Arrays.toString(offerQty));


    }

    @Benchmark
    public void testSplit6(final MyStateSplit state, final Blackhole blackhole) {
        final Splitter splitter = selectSplitter(state);
        for (int ix = 0; ix < TEST_BASIC.length; ix++) {
            final String msg = TEST_BASIC[ix];
            splitter.split(msg, "|=");
        }
    }

    @Benchmark
    public void testSplitDecode6(final MyStateDecode state, final Blackhole blackhole) {
        final Decoder decoder = selectDecode(state);
        final Splitter splitter = state.splitter;
        for (int ix = 0; ix < TEST_BASIC.length; ix++) {
            final String msg = TEST_BASIC[ix];
            final Splitter.Result result = splitter.split(msg, "|=");
            decoder.decode(splitter, null);
        }
    }

    //@Benchmark
    public void testAdd3(final MyStateBook state, final Blackhole blackhole) {
        processMessages(state, TEST_BASIC);
    }

    //@Benchmark
    public void testAdd3Remove1(final MyStateBook state, final Blackhole blackhole) {
        processMessages(state, TEST_BASIC);
        processMessages(state, TEST_REMOVE1);
    }

    //@Benchmark
    public void testAdd3Update1(final MyStateBook state, final Blackhole blackhole) {
        processMessages(state, TEST_BASIC);
        processMessages(state, TEST_UPDATE1);
    }

    //@Benchmark
    public void testAdd3Update3x3(final MyStateBook state, final Blackhole blackhole) {
        processMessages(state, TEST_BASIC);
        processMessages(state, TEST_BASIC);
        processMessages(state, TEST_BASIC);
        processMessages(state, TEST_BASIC);
    }

    //@Benchmark
    public void testAdd3Update1Remove1(final MyStateBook state, final Blackhole blackhole) {
        processMessages(state, TEST_BASIC);
        processMessages(state, TEST_UPDATE1);
        processMessages(state, TEST_REMOVE1);
    }

    private void processMessages(final MyStateBook state, final String[] messages) {
        final Decoder decoder = state.decoder;
        final Splitter splitter = state.splitter;
        final OrderBook book = selectBook(state);

        for (int ix = 0; ix < messages.length; ix++) {
            final String msg = messages[ix];
            final Splitter.Result result = splitter.split(msg, "|=");
            decoder.decode(splitter, (stamp, symbolId, side, price, qty) -> book.add(side, price, qty));
        }
    }

}
