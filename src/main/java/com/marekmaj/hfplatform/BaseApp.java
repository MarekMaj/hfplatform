package com.marekmaj.hfplatform;

import com.lmax.disruptor.BusySpinWaitStrategy;
import com.lmax.disruptor.FatalExceptionHandler;
import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.WorkerPool;
import com.marekmaj.hfplatform.event.incoming.AccountEvent;
import com.marekmaj.hfplatform.event.incoming.AccountEventPublisher;
import com.marekmaj.hfplatform.processor.AccountEventWorkHandler;
import com.marekmaj.hfplatform.service.model.Account;
import com.marekmaj.hfplatform.utils.BasicStatsPrinter;
import com.marekmaj.hfplatform.utils.HistogramPrinter;
import com.marekmaj.hfplatform.utils.Stats;
import com.marekmaj.hfplatform.utils.time.NormalDistributionGenerator;
import com.marekmaj.hfplatform.utils.time.TimeDelayGenerator;
import com.marekmaj.hfplatform.utils.time.UniformDistributionGenerator;
import net.openhft.affinity.AffinityThreadFactory;
import net.openhft.chronicle.IndexedChronicle;

import java.io.IOException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public abstract class BaseApp {
    protected static final int INPUT_DISRUPTOR_SIZE = Integer.getInteger("input.buffer.size", 256);
    protected static final int OUTPUT_DISRUPTOR_SIZE = 1024 * 64;
    protected static final int NUM_ACCOUNTS = Integer.getInteger("accounts.size", 10000);
    protected static final int ITERATIONS = 1000* 1000 * 550;
    protected static final int WARMUP = 1000 * 1000 * 50;
    protected static final double INITIAL_BALANCE = 100000;

    protected Account[] accounts;
    protected static final boolean AFFINITY = System.getProperties().containsKey("affinity") &&
            System.getProperty("affinity").equalsIgnoreCase("true");
    private static final TimeDelayGenerator TIME_GENERATOR = System.getProperties().containsKey("time.gen") ?
            (System.getProperty("time.gen").equalsIgnoreCase("uniform") ?
            new UniformDistributionGenerator() : System.getProperty("time.gen").equalsIgnoreCase("normal") ?
            new NormalDistributionGenerator() : null) : null;
    static {
        System.out.println(BaseApp.class.getSimpleName() +": INPUT_DISRUPTOR_SIZE: " + INPUT_DISRUPTOR_SIZE);
        System.out.println(BaseApp.class.getSimpleName() +": NUM_ACCOUNTS: " + NUM_ACCOUNTS);
        System.out.println(BaseApp.class.getSimpleName() +": TIME_GENERATOR: " + TIME_GENERATOR);
        System.out.println(BaseApp.class.getSimpleName() +": AFFINITY: " + AFFINITY);
    }

    protected final CyclicBarrier cyclicBarrier = new CyclicBarrier(1 + 1);

    protected final RingBuffer<AccountEvent> inputDisruptor =
            RingBuffer.createSingleProducer(AccountEvent.TRANSFER_EVENT_FACTORY,
                    INPUT_DISRUPTOR_SIZE,
                    new BusySpinWaitStrategy());

    protected final ExecutorService GATEWAY_PUBLISHER_EXECUTOR = AFFINITY ?
            Executors.newSingleThreadExecutor(new AffinityThreadFactory("GATEWAY_PUBLISHER_EXECUTOR")) :
            Executors.newSingleThreadExecutor();
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
        Stats.delaysBeforeLatenciesAfter = new long[ITERATIONS];
        if (TIME_GENERATOR != null) {
            TIME_GENERATOR.initTimes(ITERATIONS);
        }

        System.out.println( "Starting transactions..." );
        long start = System.currentTimeMillis();
        start();
        long opsPerSecond = (ITERATIONS * 1000L) / (System.currentTimeMillis() - start);
        chronicle.close();
        BasicStatsPrinter.showStats(opsPerSecond, INITIAL_BALANCE*NUM_ACCOUNTS, sumBalance());
        showStatsSpecific();
        try {
            HistogramPrinter.showHistogram(WARMUP, ITERATIONS);
        } catch (Exception e) { }

        // TODO how many gc and small gc
        System.exit(0);
    }

    private double sumBalance(){
        double balance = 0.0;
        for(Account account: accounts){
            balance += account.getBalance();
        }
        return balance;
    }

    protected abstract void start() throws Exception;

    protected abstract void showStatsSpecific();
}
