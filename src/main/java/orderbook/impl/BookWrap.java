package orderbook.impl;

import lombok.*;
import lombok.experimental.Accessors;
import orderbook.OrderBook;
import orderbook.Side;

@Builder @NoArgsConstructor @AllArgsConstructor
@Accessors(chain = true)
public class BookWrap implements OrderBook {

    @Setter @Getter
    private OrderBook orderBook;

    @Builder.Default @Setter
    private int maxPrice = Integer.MAX_VALUE;

    @Builder.Default @Setter
    private int maxQuantity = Integer.MAX_VALUE;

    @Builder.Default @Setter
    private int maxLevel = Integer.MAX_VALUE;


    /**
     * Wraps an order book with an API checking layer
     * The raw OrderBook performs minimal checking intentionally
     *
     * @param orderBook to wrap
     * @return BookWrap - intended to use chained accessors to parameterise the checks
     */
    public static BookWrap wrap(OrderBook orderBook) {
        return BookWrap.builder()
                .orderBook(orderBook)
                .build();
    }

    @Override
    public void forEach(Side side, PriceLevel priceLevel) {
        if (priceLevel == null)
            throw new IllegalArgumentException("Null priceLevel");
        checkSide(side);
        orderBook.forEach(side, priceLevel);
    }

    @Override
    public OrderBook add(final Side side, final int price, final int quantity) {
        if (price < 0 || price > maxPrice)
            throw new IllegalArgumentException("Invalid price: " + price);
        if (quantity < 0 || quantity > maxQuantity)
            throw new IllegalArgumentException("Invalid quantity: " + quantity);
        checkSide(side);
        return orderBook.add(side, price, quantity);
    }


    @Override
    public int getSizeUpToLevel(final Side side, final int level) {
        if (level < 1 || level > maxLevel)
            throw new IllegalArgumentException("Invalid level: " + level);
        checkSide(side);
        return orderBook.getSizeUpToLevel(side, level);
    }

    @Override
    public int getMidPrice() {
        return orderBook.getMidPrice();
    }

    @Override
    public int depth(final Side side) {
        checkSide(side);
        return orderBook.depth(side);
    }

    @Override
    public void getLevels(final Side side, final int level, final int[] outPrices, final int[] outQty) {
        if (level < 1 || level > maxLevel)
            throw new IllegalArgumentException("Invalid level: " + level);
        if (outPrices == null)
            throw new IllegalArgumentException("Invalid outputPrices ");
        if (outQty == null)
            throw new IllegalArgumentException("Invalid outputQty ");
        if (level > outPrices.length || level > outQty.length)
            throw new IllegalArgumentException("Level exceeds output: " + level + " " +
                outPrices.length + ":" + outQty.length);
        checkSide(side);
        orderBook.getLevels(side, level, outPrices, outQty);
    }

    @Override
    public int get(Side side, int price) {
        return orderBook.get(side, price);
    }

    @Override
    public OrderBook clear() {
        return orderBook.clear();
    }

    private static void checkSide(final Side side) {
        if (side == null)
            throw new IllegalArgumentException("Null side");
        switch (side) {
            case BID:
            case OFFER:
                break;
            default:
                throw new IllegalArgumentException("Invalid side: " + side);
        }
    }

}
