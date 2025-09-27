import java.io.*;
import java.util.*;

public class MonteCarloMPI {

    private static long simulate(long samples, long seed){
        Random rng = new Random(seed);
        long hits = 0;
        for(long i=0;i<samples;i++){
            double x=rng.nextDouble(), y=rng.nextDouble();
            if(x*x + y*y <= 1.0) hits++;
        }
        return hits;
    }

    public static void main(String[] args) throws Exception {
        
        if (args.length >= 1 && "--worker".equals(args[0])) {
            long samples = Long.parseLong(args[1]);
            long seed    = Long.parseLong(args[2]);
            System.out.println(simulate(samples, seed));
            return;
        }

        
        long totalSamples = (args.length >= 1) ? Long.parseLong(args[0]) : 10_000_000L;
        int nproc         = (args.length >= 2) ? Integer.parseInt(args[1])
                                              : Math.max(2, Runtime.getRuntime().availableProcessors());

        long base = totalSamples / nproc, rem = totalSamples % nproc;
        long t0 = System.nanoTime();

        List<Process> procs = new ArrayList<>();
        List<BufferedReader> outs = new ArrayList<>();

        String javaBin = System.getProperty("java.home") + File.separator + "bin" + File.separator + "java";
        String cp = System.getProperty("java.class.path"); // reutiliza el classpath actual

        for (int r=0; r<nproc; r++){
            long mySamples = base + (r < rem ? 1 : 0);
            long seed = 12345L ^ (0x9E3779B97F4A7C15L * (r + 1));

            ProcessBuilder pb = new ProcessBuilder(
                javaBin, "-cp", cp,
                "MonteCarloMPI", "--worker",
                Long.toString(mySamples), Long.toString(seed)
            );
            pb.redirectErrorStream(true);
            Process p = pb.start();
            procs.add(p);
            outs.add(new BufferedReader(new InputStreamReader(p.getInputStream())));
        }

        long totalHits = 0;
        for (int r=0; r<nproc; r++){
            String line = outs.get(r).readLine(); // cada worker imprime solo los hits
            if (line != null) totalHits += Long.parseLong(line.trim());
            procs.get(r).waitFor();
        }

        double pi = 4.0 * ((double) totalHits / (double) totalSamples);
        double err = Math.abs(pi - Math.PI);
        double sec = (System.nanoTime() - t0) / 1e9;

        System.out.printf("Procesos: %d | Samples: %d%n", nproc, totalSamples);
        System.out.printf("π ≈ %.12f | error=%.12g | tiempo=%.3fs%n", pi, err, sec);
    }
}

