package orderbook.impl;

import orderbook.OrderBook;
import orderbook.Side;
import lombok.Builder;

/**
 * An empty Book implementation, intended for performance testing
 * The operations are not semantically meaningful with just a little effort
 * prevent calls being optimized out
 */
@Builder
public class BookNull implements OrderBook {
    private long sumPrice, sumQty, ct;

    @Override
    public int depth(final Side side) {
        return 0;
    }

    @Override
    public void forEach(Side side, PriceLevel priceLevel) {
    }

    @Override
    public void getLevels(final Side side, final int level, final int[] outPrices, final int[] outQty) {
    }

    @Override
    public OrderBook add(final Side side, final int price, final int quantity) {
        sumPrice += price;
        sumQty += quantity;
        ct++;
        return this;
    }

    @Override
    public int getSizeUpToLevel(final Side side, final int level) {
        return 0;
    }

    @Override
    public int getMidPrice() {
        return (int)(sumPrice / ct);
    }

    @Override
    public OrderBook clear() {
        return this;
    }
}
