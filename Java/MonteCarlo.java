import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.LongAdder;

public class MonteCarlo {

    static class Worker implements Runnable {
        private final long samples;
        private final LongAdder hitCounter;

        Worker(long samples, LongAdder hitCounter) {
            this.samples = samples;
            this.hitCounter = hitCounter;
        }

        @Override
        public void run() {
            long localHits = 0L;
            ThreadLocalRandom rng = ThreadLocalRandom.current();
            for (long i = 0; i < samples; i++) {
                double x = rng.nextDouble();
                double y = rng.nextDouble();
                if (x * x + y * y <= 1.0) localHits++;
            }
            hitCounter.add(localHits);
        }
    }

    public static void main(String[] args) throws InterruptedException {
        long totalSamples = (args.length >= 1) ? Long.parseLong(args[0]) : 10_000_000L;
        int numThreads    = (args.length >= 2) ? Integer.parseInt(args[1]) : 4;

        if (totalSamples <= 0 || numThreads <= 0) {
            System.err.println("Uso: java MonteCarlo [totalSamples>0] [numThreads>0]");
            System.exit(1);
        }

        long start = System.nanoTime();

        long base = totalSamples / numThreads;
        long rem  = totalSamples % numThreads;

        Thread[] threads = new Thread[numThreads];
        LongAdder globalHits = new LongAdder();

        for (int t = 0; t < numThreads; t++) {
            long mySamples = base + (t < rem ? 1 : 0);
            threads[t] = new Thread(new Worker(mySamples, globalHits), "worker-" + t);
            threads[t].start();
        }
        for (Thread th : threads) th.join();

        long hits = globalHits.sum();
        double pi = 4.0 * ((double) hits / (double) totalSamples);

        double seconds = (System.nanoTime() - start) / 1e9;
        double error = Math.abs(pi - Math.PI);

        System.out.printf("Threads: %d | Samples: %d%n", numThreads, totalSamples);
        System.out.printf("π ≈ %.12f | error=%.12g | tiempo=%.3fs%n", pi, error, seconds);
    }
}
