package orderbook;

public enum Side {
    BID, OFFER,
    CLEAR,  // SO we can insert a clear op in the stream, to clear that side of the book
    UNKNOWN
}
