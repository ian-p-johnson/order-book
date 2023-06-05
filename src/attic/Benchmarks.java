package orderbook;

import org.openjdk.jmh.profile.GCProfiler;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.util.concurrent.TimeUnit;


public class Benchmarks {
    public static void main(String[] args) throws RunnerException {
        Options options = new OptionsBuilder()
                .addProfiler(GCProfiler.class)
                .forks(1)
                .threads(1)
                .warmupIterations(1)
                .measurementIterations(1)
                .timeUnit(TimeUnit.MILLISECONDS)

//                .jvm("/home/ianj/dev/java/zulu8.70.0.23-ca-jdk8.0.372-linux_x64/bin/java")
//                .jvm("/home/ianj/dev/java/zulu11.64.19-ca-jdk11.0.19-linux_x64/bin/java")
//                .jvm("/home/ianj/dev/java/zulu17.42.19-ca-jdk17.0.7-linux_x64/bin/java")
//                .jvm("/home/ianj/dev/java/zulu19.32.13-ca-jdk19.0.2-linux_x64/bin/java")
//                .jvm("/home/ianj/dev/java/zulu20.30.11-ca-jdk20.0.1-linux_x64/bin/java")
                .build();
        new Runner(options).run();
    }
}
