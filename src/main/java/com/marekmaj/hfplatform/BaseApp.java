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
import net.openhft.chronicle.IndexedChronicle;
import org.HdrHistogram.Histogram;
import org.HdrHistogram.HistogramData;

import java.io.IOException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public abstract class BaseApp {
    protected static final int INPUT_DISRUPTOR_SIZE = 256;                      // to determinuje przepustowość
    protected static final int OUTPUT_DISRUPTOR_SIZE = 1024 * 1024 * 32;        // TODO cos nie tak - brak konsumera ??
    protected static final int NUM_ACCOUNTS = 10000;
    protected static final int ITERATIONS = 1000* 1000 * 30;
    protected static final long WARMUP = 1000L * 1000L * 10L;
    protected static final double INITIAL_BALANCE = 100000;

    protected static final int GATEWAY_PUBLISHERS_COUNT = 1;
    protected static final int GATEWAY_CONSUMERS_COUNT = 1;
    {
        Stats.startTimes = new long[ITERATIONS];
        Stats.finishTimes = new long[ITERATIONS];
    }
    protected Account[] accounts;

    protected final CyclicBarrier cyclicBarrier = new CyclicBarrier(GATEWAY_PUBLISHERS_COUNT + 1);

    protected final RingBuffer<AccountEvent> inputDisruptor =
            RingBuffer.createSingleProducer(AccountEvent.TRANSFER_EVENT_FACTORY,
                    INPUT_DISRUPTOR_SIZE,
                    new BusySpinWaitStrategy());  // TODO bound threads to cores

    // TODO GATEWAY_PUBLISHERS_COUNT will always be one ?
    protected final ExecutorService GATEWAY_PUBLISHERS_EXECUTOR = Executors.newSingleThreadExecutor();
    protected final AccountEventPublisher[] accountEventPublishers = new AccountEventPublisher[GATEWAY_PUBLISHERS_COUNT];


    // TODO wybierac ktora transakcje transfery - balancy to w factory inputDisruptor


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
        System.out.println( "Starting transactions..." );
        //warmup();  // TODO
        //System.gc();
        long start = System.currentTimeMillis();
        start();
        long opsPerSecond = (ITERATIONS * GATEWAY_PUBLISHERS_COUNT * 1000L) / (System.currentTimeMillis() - start);
        chronicle.close();
        showStats(opsPerSecond);
        showStatsSpecific();
        showHistogram();

        System.exit(0);
    }

    private long getDiffInStats(int i) {
        return Stats.finishTimes[i] - Stats.startTimes[i];
    }

    protected void showStats(long opsPerSecond){
        System.out.println( "Initial whole balance: " + INITIAL_BALANCE*NUM_ACCOUNTS);
        System.out.println( "Current balance: " + sumBalance());
        System.out.println( "Ops per second: " + opsPerSecond);
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

        int startTimeNotSet = 0;
        int endTimeNotSet = 0;
        for (int i = ITERATIONS/3; i < ITERATIONS; i++) {
            long diff = getDiffInStats(i);
            if (Stats.finishTimes[i] == 0 ) {
                endTimeNotSet++;
            }
            if (Stats.startTimes[i] == 0 ) {
                startTimeNotSet++;
            }
            if (diff < min) {
                min = diff;
            } else if (diff > max) {
                max = diff;
            }
        }
        System.out.println("Start time not set: " + startTimeNotSet + ", endTimeNotSet " + endTimeNotSet);
        System.out.println("Minimum latency[ns]: " + min);
        System.out.println("Maximum latency[ns]: " + max);

        System.out.println();
        System.out.println( "-------------HISTOGRAM[micro s]-------------");
        Histogram histogram = new Histogram(1000000, 5);
        for (int i = ITERATIONS/3; i < ITERATIONS; i++) {
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
