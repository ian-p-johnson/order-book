package orderbook;

public interface Splitter {
    @FunctionalInterface
    public interface ToIntFunction {
        int apply(CharSequence c, int beginIndex, int endIndex);
    }

    @FunctionalInterface
    public interface ToLongFunction {
        long apply(CharSequence c, int beginIndex, int endIndex);
    }

    @FunctionalInterface
    public interface ToCharFunction {
        char apply(CharSequence c, int beginIndex, int endIndex);
    }

    enum Result {
        OK,
        FAILED
    }

    int fieldCt();

    CharSequence sequence(int ix);

    int toInt(int ix, ToIntFunction toInt);

    long toLong(int ix, ToLongFunction toLong);

    char toChar(int ix, ToCharFunction toChar);

    Result split(String msg, String delim);
}
