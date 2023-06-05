package orderbook.tools;

import it.unimi.dsi.fastutil.ints.Int2IntArrayMap;
import it.unimi.dsi.fastutil.ints.Int2IntMap;

import java.util.Map;
import java.util.Random;

/**
 * Generate a series of int who distribution follows that defines
 * It is used to generate a pseudo-ramdom series of values that are often use to parameterise
 * a test set that has a specific shape/distribution
 * The proportions are weights of a total weight, not % - (unless the total weight is 100)
 */
public class IntDistribution {
    public static Random random = new Random();
    private int totalWeight;
    private Int2IntMap map = new Int2IntArrayMap();

    public static int getRandomInt(final IntDistribution distribution) {
        int weight = random.nextInt(distribution.totalWeight);

        for (final var entry : distribution.map.entrySet()) {
            weight -= entry.getValue();
            if (weight < 0)
                return entry.getKey();
        }
        throw new IllegalStateException("Invalid class distribution");
    }

    public IntDistribution(final Map<Integer, Integer> typeMap) {
        totalWeight = typeMap.values().stream().mapToInt(Integer::intValue).sum();
        this.map.putAll(typeMap);
    }

    public static void main(final String[] args) {
        final IntDistribution distribution = new IntDistribution(Map.of(
            0, 50,  // 50 class 0
            1, 30,      // 30 class 1
            2, 20       // 20 of class 2
        ));

        for (int i = 0; i < 64; i++) {
            final int random = getRandomInt(distribution);
            System.out.printf("%d ", random);
        }
    }
}
