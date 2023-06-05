package orderbook;

public interface Decoder {
    @FunctionalInterface
    public interface Add {
        void apply(long stamp, long symbolId, Side side, int entryPx, int entrySize);
    }

    void decode(Splitter split, Add add);
}
