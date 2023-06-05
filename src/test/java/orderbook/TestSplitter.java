package orderbook;

import orderbook.impl.SplitterIndexed;
import orderbook.impl.SplitterStringSplit;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class TestSplitter {

    public static final String TEST_MSGS[] = {
        "t=1638848595|i=BTC-USD|p=32.99|q=100.00|s=b",
        "8=FIX.4.4|9=385|" +
                "35=W|34=2|49=CSERVER|50=QUOTE|52=20210621-05:28:59.339|56=demo.icmarkets.8088608|" +
                "55=1|268=7|269=1|270=1.18632|271=500000|278=74520927|" +
                "269=1|270=1.18631|271=100000|278=74520929|269=1|270=1.1863|271=165000|278=74520928|" +
                "269=1|270=1.18633|271=5500000|278=74520930|269=0|270=1.1863|271=5000|278=74510596|269=0|" +
                "270=1.18627|271=3000000|278=74517402|269=0|270=1.18629|271=3260000|278=74519180|10=013"
    };
    public static final String TEST_DELIMSS[] = {
        ",,",
        ",="
    };
    static Splitter better, trivial;

    @BeforeAll
    public static void beforeALl() {
        better = new SplitterIndexed(80);
        trivial = new SplitterStringSplit();
    }

    public static final String REFERENCE_DELIM = "[,=]";

    @Test
    void testTrivial() {
        int n = 0; for (final String testMessage : TEST_MSGS) {
            testSplitter(trivial, n, TEST_MSGS[n], "[,=]");
            n++;
        }
    }

    @Test
    void testBetter() {
        int n = 0; for (final String testMessage : TEST_MSGS) {
            testSplitter(better, n, TEST_MSGS[n], TEST_DELIMSS[n]);
            n++;
        }
    }

    @Test
    void testPrices() {
        // "t=1638848595|i=BTC-USD|p=32.99|q=100.00|s=b",
    }

    private static void testSplitter(final Splitter splitter, final int n,
        final String testMessage, final String delim) {
//        String[] expected = testMessage.split(REFERENCE_DELIM);
//        //System.out.printf("case %d: len %d\n", n, expected.length);
//        Splitter.Result result = splitter.split(testMessage, delim);
//        String id = String.format("test %s", n);
//        assertEquals(Splitter.Result.OK, result, id);
//        assertEquals(expected.length, splitter.fieldCt(), id);
//        int ix = 0;
//        CharBuffer buff = CharBuffer.wrap(testMessage);
//        SplitterBetter better = (splitter instanceof SplitterBetter)?(SplitterBetter)splitter:null;
//        for (; ix < expected.length; ix++) {
//            assertEquals(expected[ix], splitter.sequence(ix), String.format("test %d:1%d", n, ix));
////            if (better!=null) {
////                CharSequence actual =  better.sequence(ix, buff);
////                //System.out.printf("'%s' -> '%s'\n", expected[ix], actual);
////                assertEquals(actual.toString(), expected[ix], "");//String.format("test %d:1%d", n, ix));
////            }
//        }
//        assertEquals(null, splitter.sequence(ix));
//        assertEquals(null, splitter.sequence(-1));
    }
}
