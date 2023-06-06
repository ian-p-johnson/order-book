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

    /**
     * When calling asplitter, it emits entries using this interface
     */
    @FunctionalInterface
    public interface ToCharFunction {
        char apply(CharSequence c, int beginIndex, int endIndex);
    }

    enum Result {
        OK,
        FAILED
    }

    int fieldCt();

    /**
     * Extract the character sequence described by the tag ix
     * @param ix which field is being extracted
     * @return A char sequence with begin/end - usually to render as a string
     */
    CharSequence sequence(int ix);

    /**
     * Extract the integer described by the tag ix
     *
     * @param ix - index to extract field from
     * @param toInt functional interface to call with the char sequence to parse
     * @return int value after parsing
     */
    int toInt(int ix, ToIntFunction toInt);

    /**
     * Extract the long described by the tag ix
     *
     * @param ix - index to extract field from
     * @param toLong functional interface to call with the char sequence to parse
     * @return long value after parsing
     */
    long toLong(int ix, ToLongFunction toLong);

    /**
     * Extract the char described by the tag ix
     *
     * @param ix - index to extract field from
     * @param toChar functional interface to call with the char sequence to parse
     * @return char value after parsing
     */
    char toChar(int ix, ToCharFunction toChar);

    /**
     * Perform the split operation, storing the indexes of parsed fields in the splitter, for extraction
     * The splitter will be reused to avoid allocation
     * @param msg to split
     * @param delim list of delimiters use to identify field boundaries
     * @return a result if it has been split successfully
     */
    Result split(String msg, String delim);
}
