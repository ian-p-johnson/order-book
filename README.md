# Not one Order Book to rule them all
```
0: 100 12.34 | 12.44 400
1: 220 12.24 | 12.54 500
2: 150 12.14 | 12.64 300
```
A set of bid and offer prices, in price order, about a gap termed the spread.
The bids are to pay a specific amount at a specific price and an offer is to sell
a specific amount at a specific price. This is a Limit Order Book (LOB)

Some forms of LOB contain multiple independent quantity entries at each price level (one for each
entry at that price) , some contain an aggregate quantity at that price level.
The prices are arranged on 2 sides - BIDS & OFFERS(or ASKS) and are ordered by price.
A matching engine will generally match against the highest BID (when selling) or
lowest OFFER (when buying) according to matching rules. Not all price levels
have a quantity

For some use-cases, knowing the quantity available at a price level is enough (e.g. 
for some market making or liquidity sensitive trading) and for others, additional information 
is needed (e.g. exchanges where trades are matched/managed, order numbers, ..)

Most, but not all LOB support price/time ordering - where price has higher priority 
and time lower priority. You match against better prices first and then orders placed earlier at a given price level before matching against later orders at that price.  

Any structure maintaining an order book has characteristics describing  whether it implements aggregation at a price, time ordering, sparse levels 
(price with no quantity) as well as the range of prices and quantities it can support.

### I will present here a number of Order Book implementations, each with different characteristics. There is no single best implementation, and one should be selected for each use-case according to requirements

These specification asses the capabilities across the following requirements:
* Performance
* Low Latency  - these are usually used in environments where fast AND low latency
is a requirement. These Order Books are implemented in Java which uses garbage collection
and significant garbage generation will likely lead to garbage collection pauses. 
* Resource Usage - often many order books are required - one for each symbol and often one for each connection. 
Total resource usage can be significant so the resource usage should be a factor in selection 

## BookFastUtil (Int2Int)
<br>**Keys**: int32 - +/- 2B - (possible to implement unsigned int32 with some changes)
<br>**Values**: int32
<br>**Resource Usage**: low - dynamically allocate leaves in a tree structure
<Br>**Performance**: moderate - works with a wide range of values and distribution of prices

Uses primitives to avoid excessive Boxing operations. Implements a Map from an int (Price) 
to an int (Quantity) at that price. All quantity at that price are aggregated into 1 entry. If that
price is absent or zero then the level is missing.

**No other information than price/quantity is stored**

## Book ART 
**Keys**: long
<br>**Values**: int32 as used here - options for bucketized orders at a price
<br>**Resource Usage**: moderate
<Br>Performance: high

A wrapper around an Adaptive Radix Tree (credit to https://github.com/exchange-core/collections )
A structure particularly well suited to the task of maintaining an order book. Traditional in-memory data structures 
like balanced binary search trees are not efficient on modern hardware, because they do not optimally utilize 
on-CPU caches. An ART combines multiple entries onto nodes so intra-node searches are efficient. This particularly 
ART implements a long key (ideal for price) and can support multiple entries at a price (using stamp prioritized 
buckets) It also implements a form of object pooling to reduce the pressure due to garbage generation. 

It was, in general , much faster for common insertions/deletions than the **BookFastUtil Int2Int** but much slower that the 
**BookDirect**. By design it is supposed to be quite frugal in allocations but I was unable to verify that - i will be 
revisiting that aspect.   

## Book Direct
**Keys**: int32 - +/- 2B - (possible to implement unsigned int32 with some changes)
<br>**Values**: int32
<br>**Resource Usage**: high memory - a statically allocated array that is sufficient to hold all 
prices can be significant in size - for the requirements stated this is 99999 x sizeof(int32) = 400K bytes 
<Br>**Performance**: very high, variable - works with a wide range of values and distribution of prices

Uses a single allocated int array to implement a direct lookup from price to a quantity at 
that price. The array has to be sufficiently sized to fit all possible prices that 
we expect to represent - which can make it unwieldy if that range is large. The 
implementation is relatively simple, but some operations have a notable costs depending on 
the sparsity of prices near the top of book where most activity tends to take place. 
Although intuitively it would seem it would require excessive work to scan over prices with no quantity, 
the nature of modern CPU/GPU and efficient data locality means that adjacent price access is mechanically 
efficient, taking advantage of cache architecture and speculative fetching from main memory. The 
actual performance of a Direct access book should be considered in the light of the mix 
of operations expected. A typical current day CPU has a cacheline of 64 bytes, meaning we can 
fit the top 16 entries in 1 line. The active areas do shift so a lot of activity overlaps a small number of cache lines 

**No other information than price/quantity is stored**

The Direct implementation will be challenged when:
* The range of representable prices is large, and there is no simple discretization of the price.
* Available performant memory is restricted
* The price "density", especially near the top of book is low. Sparse data leads to increased
scanning through empty price slots. Data locality advantages would then be a trade-off again 
CPU usage.
* Some challenging scenarios when the map is very sparse or almost empty and linear scanning 
becomes more expensive

## Book Direct Sliding
**(A Direct window, backed by a more general book, probably the ART - Implementation out of scope here)**

Addresses the "hot" activity area, taking advantage of locality of access near top of book 

It allocates a "window" over the active area and operates a Direct Book in that range and any updates outside that 
range are handled by a "backing" book with a more flexible key/value holding capability. The window implements an effective read/write 
cache. The window is implemented in a "wrapping" manner allowing the centre price and local
activity to move outside of the original window array space, wrapping around using a price 
mask/offset to identify the physical array offset into the Direct Book. 

In many ways, it resembles a layered CPU cache, but is useful in environments where fast caches are not so abundant, for example a GPU.
A layered book is capable of representing the dense prices near the activity head and efficient handle sparse prices outside of the 
sliding window. It is a context sensitive cache  

If the requirement is to maintain a sufficiently shallow book then the sliding window may be sufficient and no backing 
layer may be required, but if it is not, a more complex scheme to "page" ranges of price/quantities from 
the backing book to the active window can be used to "re-centre" the window around the mid-price
activity.

A larger window will reduce re-balancing activity, perhaps sufficiently that it 
retains most of the performance characteristics of the simple Direct Book. Any re-organisation/paging between layers could take place on the "Hot path" when it is required, or in a background 
thread that can proactively identify the impending requirement for a migration from/to the backing 
book without requiring a physical lock on the sliding window. This operation could be very efficient 
in the sliding window because all locations are likely efficiently accessed in a modern CPU/GPU. 
Very rarely will the majority of the sliding window require relocation, so it should remain hot and available 
most of the time. 

Below is a table taken of updates from Binance over a period of months over a variation of 
symbols. It tabulates the distance of each price update from the top of book and estimates 
how "deep" a sliding window needs to be to service a given % of order book updates. 
Binance is particularly challenging in this respect because it tends to allow 8 digits precision 
with very few restrictions on price allowing prices lying between the regular spacing seen on many 
exchanges (the min tick value). Many exchanges would be less challenging, and some quite easy to deal
with on a direct book. Where intra "soft min-tick" prices are seen, but rarely, a bit can flag a "break out" 
into a separate entry designed to accommodate a number of updates in a quantized slot. As long as 
the prevalence of these outliers is low, performance may not be adversely affected. The window size can be set 
to reduce the prevalence of breakouts.   
```
      Sym        Total   Min    1x   10x   20x   50x  100x  200x  500x 1000x
----------------------------------------------------------------------------
  ZECUSDT    3,559,741  1000  53.1  92.6  96.7  98.9  99.1  99.2  99.6  99.7
SUSHIUSDT    6,540,499    10  38.0  82.0  87.8  91.6  95.0  98.8  99.6  99.9
  BTCUSDT  529,381,266   100   5.5  11.3  14.5  30.3  44.9  59.8  72.1  78.5
  YFIUSDT    6,673,771 10000  23.5  64.6  79.5  86.3  89.3  92.5  99.5  99.5
  FILUSDT   16,750,629    10  25.1  70.3  78.7  83.1  86.2  88.3  96.5  99.7
  UNIUSDT   11,850,633    10  23.6  72.6  82.8  86.3  89.6  93.1  99.4  99.6
  LTCUSDT   20,104,204   100  19.7  55.3  74.4  86.2  87.7  90.5  95.5  99.4
 ALGOUSDT   10,727,798     1  27.7  81.6  86.3  89.4  93.3  99.0  99.6  99.7
  SNXUSDT    9,554,230    10  27.0  75.3  81.6  87.7  92.9  95.6  99.5  99.8
 COMPUSDT    8,721,308   100  24.0  64.3  76.6  85.9  90.4  96.2  99.4  99.5
MATICUSDT   42,843,575     1  17.9  60.9  76.8  83.8  85.8  87.5  89.9  91.9
  SOLUSDT   18,901,692   100  34.9  77.9  83.0  88.8  92.8  98.3  98.7  98.9
 AAVEUSDT    6,273,345  1000  54.1  95.4  98.4  99.0  99.3  99.4  99.7 100.0
 DOGEUSDT   25,033,613     1  68.5  89.3  94.6  99.1  99.4  99.5  99.6 100.0
  DOTUSDT   17,904,224    10  24.2  74.1  83.0  86.3  88.6  91.5  99.6  99.8
  XMRUSDT    5,317,119  1000  34.2  83.3  87.4  92.3  94.9  95.2  99.1  99.8
 LINKUSDT   19,342,794    10  22.8  69.4  81.1  85.1  87.3  90.7  99.1  99.4
  ETHUSDT  101,937,773   100   8.5  17.5  26.2  56.7  68.5  80.0  83.2  84.9
  EOSUSDT    4,021,633    10  44.6  86.7  92.2  95.1  99.0  99.2  99.9 100.0
 ATOMUSDT   24,996,280    10  16.4  55.6  72.6  81.1  85.1  88.1  97.2  99.8
  CRVUSDT    7,782,322    10  30.6  76.7  85.6  92.9  97.6  99.3  99.9 100.0
  MKRUSDT    4,379,369 10000  50.6  93.3  96.6  97.5  99.0  99.2  99.9  99.9
  ZRXUSDT    5,969,245     1  28.6  83.1  87.0  91.0  94.4  99.5  99.6  99.7
 AVAXUSDT   11,645,274   100  39.1  80.3  83.4  90.7  94.2  99.0  99.6  99.9
  ADAUSDT   12,615,296     1  29.1  77.0  82.2  86.1  90.6  96.8  98.6  99.4
  UMAUSDT    1,779,407    10  20.2  73.7  83.8  88.6  92.0  97.5  99.4  99.7
```

Note that in many cases a 10-50 level deep direct window  will accommodate 90% of updates, 
leaving just a few troublesome symbols (e.g. BTCUSDT) where window depth of 1000 or more would be 
needed to capture most of the activity. 
1000 levels is 1000 x sizeof(int) (assuming int32 quantity = 4KB) - not a problem for modern 
CPU architecture, well within L2/L3 cache. The Sliding Window book is more of a challenge for 
a GPU where the availability of multiple regions of faster memory is far more restricted - explict shared memory on 
a typical GTX/RTX GPU is generally 48-128K, spread over a large number of concurrent GPU threads, 
3K-50K depending on SIMT capabilities. Fast L2 is also rather limited

## Requirements
The requirement are Price: 0.01 -> 999.99 and Quantity: 0.00 -> 10737418.23. The primitive type
int (or int32 - 30 bits required) is sufficiently capable of representing the range of prices
and quantities if all values are scaled x 100 (which incidentally reduces complications with 
rounding issues on calculations involving floating point numbers) int32 tends to be a very 
efficient primitive on modern architectures, allowing fast and precise arithmetic when using 
appropriate libraries to deal with the scaled numbers and scale back to the original form when 
desired.

### Low Latency, Low GC
An assumed (but not specified) requirement is that low latency is important, and specifically gc 
pauses are to be avoided as much as possible, suggesting low gc programming techniques such as 
re-using objects, avoiding allocations and typically noisy gc objects such as Strings. This will
not necessarily improve throughput (but it usually does) but it should mitigate some of the 
unpleasant side effects of garbage collection pauses. Where low/zero garbage techniques have 
little prohibitive cost, they will be used.

Following is a list of requirements met by the implementations:
* Instrument universe is BTC-USD, ETH-USD, SOL-USD (all symbols captured in a long as 8 bytes, avoiding variable field handling)
* Price is in the range between “0.01” and “999.99” - scaled to int early in parsing
* Quantity is in the range between “0.00” and “10737418.23” (fits in 30 bit)
* Number of decimal points for price and quantity is always 2 - ideally a general formatter that can be specified per symbol - but currently hardcoded to 2 dp
* Specified API implemented include: 
  * Allow to efficiently iterate over the order book levels in one direction in the ascending (asks) or descending (bids) price order
  * Total bid/ask quantity for up to N levels (the level can only be included if it has quantity > 0)
  * Mid Price
* Some additonal API were added to demonstrate functionaility,such as 
  * Depth per side, book per side, clear etc
  * **Determining the price at which a specific amount of liquidity can,in theory, be accessed**

* **There is no requirement to do anything with the stamp at this time**

## Design Choices

Apart from the Direct Access technique, one approach was to use a conventional 
collection to hold the prices on each side of the book. I used a popular library that has the appropriate characteristics - **fastutil**. 
* Primitive rather than boxed/class keys/values
* Ordered access
* Elements are closely located - so good data access locality  
* Generates minimal garbage for each key/value 
* Only minimal error checking was applied in the core Order Book APi. This is intentional as the library could have uses 
inside a critical performance loop of a simulation as well as live trading.
Regular java will deal with most situations using null pointer, subscript checking and other unchecked exceptions.
Some checks will be made to mitigate difficult to trace conditions, but only 
where the Java standard treatment would likely not pick up a problem and lead to soft failures
    **This is an intentional tradeoff - please wrap this library if you need more. A suitable wrapper has been provided** 

There are a number of collections that support these characteristics but I have considered only 
fastutil here - (Eclipse, HPPC, Koloboke) as they are likely to bring equivalent performance characteristics . I have also wrapped an  "Adaptive Radix Tree", a structure both 
well suited to the role, as well as addressing efficiency concerns to a significant degree - the 
version i am supports a non native value - so it must be checked for garbage generation.   

The decimal4j library was used because it lends itself well to zero/low garbage scaled arithmetic - It is far 
faster that the standard Java BigDecimal, and safer than native FP types. It may, however, not be as fast as 
native fp types, so care must be taken in a more challenging arithmetic environment. An Order Book is usually 
not that environment.

The approach was to implement a generalised version that would operate in a wide range of 
scenarios, then specialise in order to take advantage of the specification (so restricted data ranges, 
price locality) One specific choice was not to propagate **ALL** available data for each order 
book entry. This was intentional as the stripped down book meets the stated requirements and remains useful in 
many other  scenarios where the additional data (e.g. stamp) is not need. Specifically single entries at a price, int32 values precludes having multiple orders at a level and/or storing stamps and other data. The structure could, in theory,
be extended to cover the additional features but not without some notable expense. 

## Verification

The performance of the Order Book implementations will be verified over a number 
scenarios using JMH, both to verify throughput and garbage generation. The specification was to parse 
and process input in a variable length tagged field format (similar to FIX) so a low garbage splitter/parser 
was built in order to maintain low gc through the whole pipeline. 

The book performance was measured 
in isolation as well as alongside parsing and decoding of the stream for more holistic view.

The performance is verified using a data stream creation tool that generates a synthetic mildly 
parameterizable stream of orders. These should generate a realistic load (whilst not being 
based off real data)  Another step might be to capture actual exchange data, convert to the 
format specified and use that to test the implementation in more realistic conditions. 
I have supplied a command line utility that can parse a file and process each entry (in examples) as well as some example data captured from Binance 

I have included some of the performance metrics to give some idea of the capabilities of each
implementation

## Performance
#### Order Book only - add/delete - batches of 1,000 orders - BooKDirect is by far the fastest when tested in isolation, and generates zero garbage for insertion/updates

#### BookFastUtil Int2Int - moderate performance, moderate garbage
```
Benchmark                               (book)            Mode  Cnt      Score   Error   Units
testRawMixed1k                        FASTUTIL            thrpt          13.198          ops/ms
testRawMixed1k:·gc.alloc.rate         FASTUTIL            thrpt          62.833          MB/sec
testRawMixed1k:·gc.time               FASTUTIL            thrpt           6.000              ms
```
#### Book Direct - very fast, zero garbage
```
testRawMixed1k                          DIRECT            thrpt         624.437          ops/ms
testRawMixed1k:·gc.alloc.rate           DIRECT            thrpt          ≈ 10⁻⁴          MB/sec
testRawMixed1k:·gc.count                DIRECT            thrpt             ≈ 0          counts
```
#### BookArt Adaptive Radix Tree - fast, notable garbage (see notes)
```
testRawMixed1k                             ART            thrpt          49.280          ops/ms
testRawMixed1k:·gc.alloc.rate              ART            thrpt         597.044          MB/sec
testRawMixed1k:·gc.count                   ART            thrpt          10.000          counts
testRawMixed1k:·gc.time                    ART            thrpt          21.000              ms
```
#### Parsing 6 messages - fast, no garbage 
```
testSplit6                                 INDEX          thrpt        5574.633          ops/ms
testSplit6:·gc.alloc.rate                  INDEX          thrpt          ≈ 10⁻⁴          MB/sec
testSplit6:·gc.count                       INDEX          thrpt             ≈ 0          counts
```
#### Compared with String.split - slow and gc noisy - provided as a reference only 
```
testSplit6                                 SPLIT          thrpt         133.584          ops/ms
testSplit6:·gc.alloc.rate                  SPLIT          thrpt        2818.941          MB/sec
testSplit6:·gc.count                       SPLIT          thrpt          39.000          counts
testSplit6:·gc.time                        SPLIT          thrpt          31.000              ms
```
#### Decoding 6 messages - generic - moderate performance, no gc
```
testSplitDecode6                           GENERIC        thrpt        1908.105          ops/ms
testSplitDecode6:·gc.alloc.rate            GENERIC        thrpt          ≈ 10⁻⁴          MB/sec
testSplitDecode6:·gc.count                 GENERIC        thrpt             ≈ 0          counts
```
#### Decoding 6 messages - dedicated - nearly 100% faster, no gc 
```
testSplitDecode6                           DEDICATED      thrpt        3679.494          ops/ms
testSplitDecode6:·gc.alloc.rate            DEDICATED      thrpt          ≈ 10⁻⁴          MB/sec
testSplitDecode6:·gc.count                 DEDICATED      thrpt             ≈ 0          counts

```
## Notes
You can use the following build targets - mainly gradle, especially for checkstyle, JMH, but basic build/test in Maven
* ./gradlew jmh - to run performance verification tests
* ./gradlew test - to run unit tests 
* mvn verify
* Run a test using 
  * **java -cp target/orderbook-1.0-SNAPSHOT.jar  orderbook.examples.Example1 -levels 9 -file example1.txt**
  * In testing/ there are  number of SOL-USD test files 1k, 10k, 100k & 1M entries and tests with BookArt, BookFastUtil & BooKDirect. 
  Examining the available liquidity (for 1-99 levels) for each book implementation produces the same result, which at least is an indication of consistency 
      

## Closing observations
* The bottleneck in the pipeline is currently the parsing of fixed to integral type from the variable field input. 
Whilst not directly in the scope of the Order Book implementation itself this would seem a good place to look for improvements, and i have provided
one such version (Dedicated) that has twice the throughput of the more general version. The bottleneck in decoding may be less critical in  some 
applications where the order book performance may be the priority, as parsing/decoding of incoming streams are often performed 
in parallel, whilst matching often takes place in a shared book where the performance requirement may be more critical
* Per thread on a Ryzen 3900X, **without generating any garbage**:
    * BookDirect can reach 600+M updates a second from prepared data - not surprising because it is so targetted (probably more with bounds checking disabled) 
    * Indexed splitter can process around 30M a second from a string (A faster version could run from a memory mapped CharBuffer) 
    * Dedicated decoder + splitter can process around 22M entries a second  
* The library would benefit from a "Market" concept to act as a factory to access and instantiate Order Books, parameterised by symbol, returning the most 
appropriate book configuration for that symbol. AT the moment it merely offers a design pattern in **Example1**  

A highly performant but restricted Direct Book has been implemented but has a restricted applicability. A capable,
lower resource version has been proposed (sliding direct window) but has not yet been implemented. This may maintain much of the performance
of the Direct book together with reduced resource (memory) utilisation , less memory cache architecture pressure etc, at the cost of added complexity. 

<br>Implementation of that version is ongoing ...  






