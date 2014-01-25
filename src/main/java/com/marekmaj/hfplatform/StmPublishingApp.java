package com.marekmaj.hfplatform;

import com.lmax.disruptor.*;
import com.marekmaj.hfplatform.event.incoming.AccountEvent;
import com.marekmaj.hfplatform.event.outcoming.ResultEvent;
import com.marekmaj.hfplatform.event.outcoming.ResultEventPublisher;
import com.marekmaj.hfplatform.processor.AccountEventWorkHandler;
import com.marekmaj.hfplatform.processor.ResultEventHandler;
import com.marekmaj.hfplatform.service.impl.AkkaStmPublishingAccountService;
import com.marekmaj.hfplatform.utils.MinorStatsPrinter;
import net.openhft.affinity.AffinityThreadFactory;

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

    private final ExecutorService GATEWAY_CONSUMERS_EXECUTOR = AFFINITY ?
            Executors.newSingleThreadExecutor(new AffinityThreadFactory("GATEWAY_CONSUMERS_EXECUTOR")) :
            Executors.newSingleThreadExecutor();

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
        startWork();
    }

    private void startWork() throws Exception{
        final CountDownLatch latch = new CountDownLatch(1);
        resultEventHandler.reset(latch, batchEventProcessor.getSequence().get() + ITERATIONS);

        Future<?> future = GATEWAY_PUBLISHER_EXECUTOR.submit(accountEventPublisher);
        GATEWAY_CONSUMERS_EXECUTOR.submit(batchEventProcessor);

        workerPool.start(WORKERS_EXECUTOR);
        //RingBuffer<ResultEvent> ringBuffer2 = getPool.start(GATEWAY_CONSUMERS_EXECUTOR);

        cyclicBarrier.await();

        future.get();

        latch.await();
        workerPool.drainAndHalt();

        //getPool.drainAndHalt();
        batchEventProcessor.halt();
    }

    @Override
    protected void showStatsSpecific() {
        MinorStatsPrinter.printAccountEventHandlersStats(accountEventWorkHandlers);
        MinorStatsPrinter.printResultEventStats();
    }

    // TODO how many gc and small gc
}
