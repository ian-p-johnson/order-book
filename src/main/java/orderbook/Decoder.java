package orderbook;

/**
 * The interface to a feed decode. This is uses teh low level to access fields and convert svalue to a suitable format to apply
 * to the next stage in the pipeline
 */
public interface Decoder {
    /**
     * A consumer for the fields decoded
     */
    @FunctionalInterface
    public interface Add {
        void apply(long stamp, long symbolId, Side side, int entryPx, int entrySize);
    }

    void decode(Splitter split, Add add);
}
