package orderbook;

public interface OrderBook {

    /**
     * Part of the iteration mechanism - used to pass price levels to the client after a forEach- when they
     * have sufficient they should return false.
     *
     * Any available wil lbe emitted whilst true is returned
     */
    @FunctionalInterface
    interface PriceLevel {
        boolean more(int entryPx, int entrySize);
    }

    /**
     * Anything that emits a price/qty will emit this to mean no price/quantity
     */
    int NO_PRICE = -1;  // Invalid or no Price
    int NO_VALUE = -1;  // Invalid or no Quantity

    /**
     * Add, Update or Remove a price level from one side of an order book
     * Adds a new entry if it does not already exist on that side
     * Updates the quantity for a level that exists on that side
     * If quantity = 0, then remove that price level from that side
     * There are 0 or 1 quantity associated with a given price level for a given side
     *
     * @param side     to operate on
     * @param price    to add, modify, remove
     * @param quantity to add, modify - to remove, specify 0
     * @return the order book
     */
    OrderBook add(Side side, int price, int quantity);

    /**
     * Derives the mid-price from top of book bid/ask - applying FLOOR rounding
     *
     * @return Could return NO_PRICE if either a bid or offer is not present
     */
    int getMidPrice();

    /**
     * Returns the number of non zero quantity prices on the specified side
     *
     * @param side to access
     * @return the total number of none 0 levels available - 0 if none
     */
    int depth(Side side);

    /**
     * Iterates through the specified side of the book, stopping
     * when it runs out of levels or the consumer sates that he does not require any more
     * The order of iteration will be the preferred ordering of the book
     * i.e Highest Bid first, Lowest Offer first
     *
     * @param side       to access
     * @param priceLevel to call with each level
     */
    void forEach(Side side, PriceLevel priceLevel);

    /**
     * Extracts the top levels of an orderbook,copying them into the supplied bid/offer arrays
     * It performs no explicit bounds checking, leaving that to defaultjava behaviour
     *
     * @param side      Bid or Offer side
     * @param level     How many levels deep into the book to extract
     * @param outPrices A 0 indexed array to extract price into - 0 is "best" (so highest Bid and lowest Offer)
     * @param outQty    A 0 indexed array to extract quantity into- each element corresponds to the same position on outPrices
     */
    void getLevels(Side side, int level, int[] outPrices, int[] outQty);

    /**
     *
     * @param side of book
     * @param price being accessed
     * @return quantity (or NO
     */
    int get(final Side side, final int price);

    /**
     * Clears all price./quantities from the book
     *
     * @return this order book
     */
    OrderBook clear();
}
