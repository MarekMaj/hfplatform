package com.marekmaj.hfplatform;

import com.lmax.disruptor.BusySpinWaitStrategy;
import com.lmax.disruptor.FatalExceptionHandler;
import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.WorkerPool;
import com.marekmaj.hfplatform.event.incoming.AccountEvent;
import com.marekmaj.hfplatform.event.incoming.AccountEventPublisher;
import com.marekmaj.hfplatform.processor.AccountEventWorkHandler;
import com.marekmaj.hfplatform.service.model.Account;
import com.marekmaj.hfplatform.utils.Stats;
import com.marekmaj.hfplatform.utils.time.NormalDistributionGenerator;
import com.marekmaj.hfplatform.utils.time.TimeDelayGenerator;
import com.marekmaj.hfplatform.utils.time.UniformDistributionGenerator;
import net.openhft.chronicle.IndexedChronicle;
import org.HdrHistogram.Histogram;
import org.HdrHistogram.HistogramData;

import java.io.IOException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public abstract class BaseApp {
    protected static final int INPUT_DISRUPTOR_SIZE =  1024 * 64;
    protected static final int OUTPUT_DISRUPTOR_SIZE = 1024 * 64;
    protected static final int NUM_ACCOUNTS = 10000;
    protected static final int ITERATIONS = 1000* 1000 * 100;
    protected static final int WARMUP = 1000 * 1000 * 30;
    protected static final double INITIAL_BALANCE = 100000;

    protected Account[] accounts;
    private static final TimeDelayGenerator TIME_GENERATOR = System.getProperty("time.gen").equalsIgnoreCase("uniform") ?
            new UniformDistributionGenerator() : System.getProperty("time.gen").equalsIgnoreCase("normal") ?
            new NormalDistributionGenerator() : null;

    protected final CyclicBarrier cyclicBarrier = new CyclicBarrier(1 + 1);

    protected final RingBuffer<AccountEvent> inputDisruptor =
            RingBuffer.createSingleProducer(AccountEvent.TRANSFER_EVENT_FACTORY,
                    INPUT_DISRUPTOR_SIZE,
                    new BusySpinWaitStrategy());  // TODO bound threads to cores

    protected final ExecutorService GATEWAY_PUBLISHER_EXECUTOR = Executors.newSingleThreadExecutor();
    protected AccountEventPublisher accountEventPublisher;


    protected IndexedChronicle chronicle;
    {
        try {
            this.chronicle = new IndexedChronicle("/dane/work/mgr/logs/log");
            chronicle.config().useUnsafe(true); // for benchmarks
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    protected WorkerPool<AccountEvent> getAccountEventWorkerPool(AccountEventWorkHandler... accountEventWorkHandlers) {
        WorkerPool<AccountEvent> workerPool = new WorkerPool<>(inputDisruptor,
                inputDisruptor.newBarrier(),
                new FatalExceptionHandler(),
                accountEventWorkHandlers);
        inputDisruptor.addGatingSequences(workerPool.getWorkerSequences());
        return workerPool;
    }

    protected void run() throws Exception{
        if (TIME_GENERATOR != null) {
            TIME_GENERATOR.initTimes(ITERATIONS);
        }
        System.out.println( "Starting transactions..." );
        start();
        chronicle.close();
        showStats();
        showStatsSpecific();
        try {
            showHistogram();
        } catch (Exception e) { }

        System.exit(0);
    }

    private long getDiffInStats(int i) {
        return Stats.delaysBeforeLatenciesAfter[i];
    }

    protected void showStats(){
        System.out.println( "Initial whole balance: " + INITIAL_BALANCE*NUM_ACCOUNTS);
        System.out.println( "Current balance: " + sumBalance());
        System.out.println( "Ops per second: " + ITERATIONS /
                ((Stats.delaysBeforeLatenciesAfter[ITERATIONS] - Stats.delaysBeforeLatenciesAfter[WARMUP]) * 1000000));
        System.out.println( "Rollbacks for transfer operation: " + Stats.getTransactionRollbacks());
        System.out.println( "Rollbacks for account manipulation: " + Stats.getAccountRollbacks());
        System.out.println( "Comitted: " + Stats.getCommits());
        System.out.println( "Canceled (the same account): " + Stats.getCanceled());
        System.out.println( "Canceled (insufficient funds): " + Stats.getInsufficient());
    }

    private void showHistogram() {
        System.out.println();
        System.out.println( "-------------STATS COLLECTED-------------");
        long min = getDiffInStats(1);
        long max = getDiffInStats(1);

        for (int i = WARMUP; i < ITERATIONS; i++) {
            long diff = getDiffInStats(i);
            if (diff < min) {
                min = diff;
            } else if (diff > max) {
                max = diff;
            }
        }
        //System.out.println("Start time not set: " + startTimeNotSet + ", endTimeNotSet " + endTimeNotSet);
        System.out.println("Minimum latency[ns]: " + min);
        System.out.println("Maximum latency[ns]: " + max);

        System.out.println();
        System.out.println( "-------------HISTOGRAM[micro s]-------------");
        Histogram histogram = new Histogram(1000000, 5);
        for (int i = WARMUP; i < ITERATIONS; i++) {
            histogram.recordValue(getDiffInStats(i)/1000);
        }

        HistogramData data = histogram.getHistogramData();
        System.out.println( "Max time " + data.getMaxValue());
        System.out.println( "Min time " + data.getMinValue());
        System.out.println( "Mean time " + data.getMean());
        System.out.println( "50 percentile " + data.getValueAtPercentile(50));
        System.out.println( "75 percentile " + data.getValueAtPercentile(75));
        System.out.println( "90 percentile " + data.getValueAtPercentile(90));
        System.out.println( "95 percentile " + data.getValueAtPercentile(95));
        System.out.println( "99 percentile " + data.getValueAtPercentile(99));
        System.out.println( "99.9 percentile " + data.getValueAtPercentile(99.9));
        System.out.println( "Percentile for less than 1ms " + data.getPercentileAtOrBelowValue(1000));
    }
    private double sumBalance(){
        double balance = 0.0;
        for(Account account: accounts){
            balance += account.getBalance();
        }
        return balance;
    }

    protected abstract void start() throws Exception;

    protected abstract void warmup() throws Exception;

    protected abstract void showStatsSpecific();
}
