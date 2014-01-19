package com.marekmaj.hfplatform;


import com.lmax.disruptor.*;
import com.marekmaj.hfplatform.event.incoming.AccountEvent;
import com.marekmaj.hfplatform.event.outcoming.ResultEvent;
import com.marekmaj.hfplatform.event.outcoming.ResultEventPublisher;
import com.marekmaj.hfplatform.processor.AccountEventWorkHandler;
import com.marekmaj.hfplatform.processor.ResultEventHandler;
import com.marekmaj.hfplatform.service.impl.SingleThreadedPublishingAccountService;
import com.marekmaj.hfplatform.utils.Stats;
import com.marekmaj.hfplatform.utils.WithDedicatedCpuRunnable;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static com.lmax.disruptor.RingBuffer.createSingleProducer;

public class SingleThreadPublishingApp extends SingleThreadBaseApp {

    private final RingBuffer<ResultEvent> outputDisruptor =
            createSingleProducer(ResultEvent.RESULT_EVENT_FACTORY, OUTPUT_DISRUPTOR_SIZE, new BusySpinWaitStrategy());

    private final SequenceBarrier sequenceBarrier = outputDisruptor.newBarrier();
    private final ResultEventHandler resultEventHandler = new ResultEventHandler(chronicle);
    private final BatchEventProcessor<ResultEvent> batchEventProcessor = new BatchEventProcessor<>(outputDisruptor, sequenceBarrier, resultEventHandler);
    {
        outputDisruptor.addGatingSequences(batchEventProcessor.getSequence());
    }

    private final ExecutorService GATEWAY_CONSUMERS_EXECUTOR = Executors.newSingleThreadExecutor();

    private final AccountEventWorkHandler accountEventWorkHandler = new AccountEventWorkHandler(
            new SingleThreadedPublishingAccountService(new ResultEventPublisher(outputDisruptor)));
    private final WorkerPool<AccountEvent> workerPool = getAccountEventWorkerPool(accountEventWorkHandler);

    public static void main( String[] args ) throws Exception{
        new SingleThreadPublishingApp().run();
    }

    @Override
    public void start() throws Exception {
        startWork();
    }

    private void startWork() throws Exception{
        final CountDownLatch latch = new CountDownLatch(1);
        resultEventHandler.reset(latch, batchEventProcessor.getSequence().get() + ITERATIONS);

        Future<?> future = GATEWAY_CONSUMERS_EXECUTOR.submit(new WithDedicatedCpuRunnable(batchEventProcessor));

        workerPool.start(WORKER_EXECUTOR);

        Future<?>[] futures = new Future[GATEWAY_PUBLISHERS_COUNT];
        for (int i = 0; i < GATEWAY_PUBLISHERS_COUNT; i++) {
            futures[i] = GATEWAY_PUBLISHERS_EXECUTOR.submit(new WithDedicatedCpuRunnable(accountEventPublishers[i]));
        }

        cyclicBarrier.await();

        for (int i = 0; i < GATEWAY_PUBLISHERS_COUNT; i++) {
            futures[i].get();
        }

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
        System.out.println( "Total ops for handler " + accountEventWorkHandler.getCounter());
        System.out.println();
        System.out.println( "-------------RESULT EVENT PUBLISHERS-------------");
        System.out.println( "Total ready to publish results " + Stats.getReadyToPublishResults());
        System.out.println( "Total published results " + Stats.getPublishedResults());

        System.out.println();
        System.out.println( "-------------RESULT EVENT HANDLERS-------------");
        System.out.println( "Total logged results " + Stats.getLoggedResults());
        System.out.println( "Total ignored results " + Stats.getIgnoredResults());
        System.out.println( "Not consumed results " + (Stats.getIgnoredResults() - Stats.getTransactionRollbacks()));
    }
}
