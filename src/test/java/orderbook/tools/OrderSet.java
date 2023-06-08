package orderbook.tools;

import orderbook.impl.Decoders;
import it.unimi.dsi.fastutil.chars.CharArrayList;
import it.unimi.dsi.fastutil.chars.CharList;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongList;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

import java.util.Map;
import java.util.Random;

/**
 * Generates a set of price delta, quantity and order book operationss, resulting in a number of arrays that
 * can be used to orchestrate an order book test.
 *
 * Current the distribution of operations is defined by proportion, the price delta from top is
 * random and the qty is random - all withing sensible limits.
 *
 * There is no current walk applied to the price - this is unrealistic
 *
 * The intention is to create a realistic order book load, without the need to load a real canned
 * order book stream
 */
@Builder @ToString(onlyExplicitlyIncluded = true)
public class OrderSet {
    public int[] prices, quantities;
    public char[] op;
    public long[] symbolIds;

    @Builder.Default @ToString.Include
    public int n = 10;


    @Builder.Default
    private Random r = new Random(0);

    @Builder.Default @ToString.Include @Getter
    private int priceRange = 10;

    @Builder.Default @ToString.Include @Getter
    private int priceBase = 100;

    @Builder.Default @ToString.Include @Getter
    private int qtyRange = 100;

    @Builder.Default @ToString.Include @Getter
    private int qtyBase = 100;

    @ToString.Include Map<Integer, Integer> typeDistribution;
    @ToString.Include Map<Integer, Integer> priceDistribution;
    @ToString.Include Map<Integer, Integer> symbolDistribution;
    @ToString.Include String[] symbols;

    public static final char ADD = 0;
    public static final char REMOVE = 1;
    public static final char CHANGE = 2;
    public static final char ACCESS = 3;

    public OrderSet generate() {
        final IntList pricesList = new IntArrayList(n);
        final IntList quantitiesList = new IntArrayList(n);
        final LongList symbolIdsList = new LongArrayList(n);
        final CharList opList = new CharArrayList(n);
        final IntDistribution typesD = new IntDistribution(typeDistribution);
        final IntDistribution pricesD = new IntDistribution(priceDistribution);
        final IntDistribution symbolsD = new IntDistribution(symbolDistribution);

        for (int ix = 0; ix < n; ix++) {
            final char op = (char)IntDistribution.getRandomInt(typesD);
            final int p = IntDistribution.getRandomInt(pricesD);
            final int price = r.nextInt(p) - p / 2;
            final int symbolIx = IntDistribution.getRandomInt(symbolsD);
            final String symbol = symbols[symbolIx];
            final long symbolId = Decoders.toLongChar8(symbol, 0, symbol.length());
            symbolIdsList.add(symbolId);
            pricesList.add(priceBase + price); //+ price * priceRange/100);

            quantitiesList.add(op == REMOVE ?
                0 :
                (qtyBase + r.nextInt(qtyRange)));
            opList.add(op);
        }
        prices = pricesList.toIntArray();
        quantities = quantitiesList.toIntArray();
        op = opList.toCharArray();
        symbolIds = symbolIdsList.toLongArray();
        return this;
    }

    public static void main(final String[] args) {

        final String[] symbolUniverse = new String[]{"BTC-USD", "ETH-USD", "SOL-USD"};
        final OrderSet orderSet = OrderSet.builder()
            .typeDistribution(Map.of(
            0, 40,  // 40 / Add
            1, 20,      // 20 / Change
            2, 30))     // 30 / Remove
            .priceDistribution(Map.of(
            10, 40, // 40% / 10
            1_00, 40,   // 40% / 100
            5_00, 15,   // 15% / 500
            10_00, 5))  //  5% / 1000
            .symbolDistribution(Map.of(
            0, 1, // 33% BTC-USD
            1, 1,   // 33% ETH-USD
            2, 1))   // 33% SOL-USD
            .symbols(symbolUniverse)
            .priceBase(5_000)   .priceRange(4_000)
            .qtyBase(10_00)     .qtyRange(1_00)
            .n(1_000)
            .build().generate();
        System.out.printf("set = %s\n", orderSet);

        System.out.printf("%4s: %5s-> %4s\n", "Sz", "Price", "Qty");
        for (int ix = 0; ix < orderSet.n; ix++)
            System.out.printf("%d %4d: %5d-> %4d\n",
                    orderSet.symbolIds[ix],  (int)orderSet.op[ix],
                    orderSet.prices[ix], orderSet.quantities[ix]);
    }
}
