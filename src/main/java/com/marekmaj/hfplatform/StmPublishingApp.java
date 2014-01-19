package com.marekmaj.hfplatform;

import com.lmax.disruptor.*;
import com.marekmaj.hfplatform.event.incoming.AccountEvent;
import com.marekmaj.hfplatform.event.outcoming.ResultEvent;
import com.marekmaj.hfplatform.event.outcoming.ResultEventPublisher;
import com.marekmaj.hfplatform.processor.AccountEventWorkHandler;
import com.marekmaj.hfplatform.processor.ResultEventHandler;
import com.marekmaj.hfplatform.service.impl.AkkaStmPublishingAccountService;
import com.marekmaj.hfplatform.utils.Stats;
import com.marekmaj.hfplatform.utils.WithDedicatedCpuRunnable;
import org.HdrHistogram.Histogram;
import org.HdrHistogram.HistogramData;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static com.lmax.disruptor.RingBuffer.createMultiProducer;


public class StmPublishingApp extends StmBaseApp {

    private final RingBuffer<ResultEvent> outputDisruptor =
            createMultiProducer(ResultEvent.RESULT_EVENT_FACTORY, OUTPUT_DISRUPTOR_SIZE, new BusySpinWaitStrategy());

    private final SequenceBarrier sequenceBarrier = outputDisruptor.newBarrier();
    private final ResultEventHandler resultEventHandler = new ResultEventHandler(chronicle);
    private final BatchEventProcessor<ResultEvent> batchEventProcessor = new BatchEventProcessor<>(outputDisruptor, sequenceBarrier, resultEventHandler);
    {
        outputDisruptor.addGatingSequences(batchEventProcessor.getSequence());
    }

    private static final int NUM_WORKERS = 6;

    private final ExecutorService GATEWAY_PUBLISHERS_EXECUTOR = Executors.newSingleThreadExecutor();
    private final ExecutorService WORKERS_EXECUTOR = Executors.newFixedThreadPool(NUM_WORKERS);
    private final ExecutorService GATEWAY_CONSUMERS_EXECUTOR = Executors.newSingleThreadExecutor();

    private final AccountEventWorkHandler[] accountEventWorkHandlers = new AccountEventWorkHandler[NUM_WORKERS];
    {
        for (int i = 0; i < NUM_WORKERS; i++){
            accountEventWorkHandlers[i] = new AccountEventWorkHandler(
                                new AkkaStmPublishingAccountService(
                                     new ResultEventPublisher(outputDisruptor)));
        }
    }

    private final WorkerPool<AccountEvent> workerPool = getAccountEventWorkerPool(accountEventWorkHandlers);

/*
    private final ResultEventWorkHandler[] resultEventWorkHandlers = new ResultEventWorkHandler[GATEWAY_CONSUMERS_COUNT];
    {
        for (int i = 0; i < GATEWAY_CONSUMERS_COUNT; i++){
            resultEventWorkHandlers[i] = new ResultEventWorkHandler();
        }
    }

    private final WorkerPool<ResultEvent> getPool =
            new WorkerPool<ResultEvent>(outputDisruptor,
                    outputDisruptor.newBarrier(),
                    new FatalExceptionHandler(),
                    resultEventWorkHandlers);
    {
        outputDisruptor.addGatingSequences(getPool.getWorkerSequences());
    }
*/

    public static void main( String[] args ) throws Exception{
        new StmPublishingApp().run();
    }

    @Override
    public void start() throws Exception {
        Stats.startTimes = new long[ITERATIONS];
        Stats.finishTimes = new long[ITERATIONS];
        startWork();
    }

    private void startWork() throws Exception{
        final CountDownLatch latch = new CountDownLatch(1);
        resultEventHandler.reset(latch, batchEventProcessor.getSequence().get() + ITERATIONS);

        Future<?> future = GATEWAY_CONSUMERS_EXECUTOR.submit(new WithDedicatedCpuRunnable(batchEventProcessor));

        RingBuffer<AccountEvent> ringBuffer = workerPool.start(WORKERS_EXECUTOR);
        //RingBuffer<ResultEvent> ringBuffer2 = getPool.start(GATEWAY_CONSUMERS_EXECUTOR);

        Future<?>[] futures = new Future[GATEWAY_PUBLISHERS_COUNT];
        for (int i = 0; i < GATEWAY_PUBLISHERS_COUNT; i++) {
            futures[i] = GATEWAY_PUBLISHERS_EXECUTOR.submit(new WithDedicatedCpuRunnable(accountEventPublishers[i]));
        }

        cyclicBarrier.await();

        for (int i = 0; i < GATEWAY_PUBLISHERS_COUNT; i++) {
            futures[i].get();
        }
        //future.get();

        latch.await();
        workerPool.drainAndHalt();

        //getPool.drainAndHalt();
        batchEventProcessor.halt();
    }

    @Override
    protected void warmup() throws Exception {
        startWork();
    }

    @Override
    protected void showStatsSpecific() {
        System.out.println();
        System.out.println( "-------------ACCOUNT EVENT HANDLERS-------------");
        for (AccountEventWorkHandler handler : accountEventWorkHandlers){
            System.out.println( "Total ops for handler " + handler.getCounter());
        }
        System.out.println();
        System.out.println( "-------------RESULT EVENT PUBLISHERS-------------");
        System.out.println( "Total ready to publish results " + Stats.getReadyToPublishResults());
        System.out.println( "Total published results " + Stats.getPublishedResults());

        System.out.println();
        System.out.println( "-------------RESULT EVENT HANDLERS-------------");
        System.out.println( "Total logged results " + Stats.getLoggedResults());
        System.out.println( "Total ignored results " + Stats.getIgnoredResults());
        System.out.println( "Not consumed results " + (Stats.getIgnoredResults() - Stats.getTransactionRollbacks()));

        System.out.println();
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
                //System.out.println("new minimum: " + min + "from: " + Stats.startTimes[i] + "to: " + Stats.finishTimes[i] + ", i: " + i);
            } else if (diff > max) {
                max = diff;
                //System.out.println("new max: " + max + "from: " + Stats.startTimes[i] + "to: " + Stats.finishTimes[i] + ", i: " + i);
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

    private long getDiffInStats(int i) {
        return Stats.finishTimes[i] - Stats.startTimes[i];
    }

    // TODO how many gc and small gc
}
