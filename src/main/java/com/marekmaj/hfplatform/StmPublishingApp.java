package com.marekmaj.hfplatform;

import com.lmax.disruptor.*;
import com.marekmaj.hfplatform.event.incoming.AccountEvent;
import com.marekmaj.hfplatform.event.incoming.AccountEventPublisher;
import com.marekmaj.hfplatform.event.outcoming.ResultEvent;
import com.marekmaj.hfplatform.event.outcoming.ResultEventPublisher;
import com.marekmaj.hfplatform.processor.AccountEventWorkHandler;
import com.marekmaj.hfplatform.processor.ResultEventHandler;
import com.marekmaj.hfplatform.service.impl.AkkaStmPublishingAccountService;
import com.marekmaj.hfplatform.service.model.StmAccount;
import com.marekmaj.hfplatform.utils.Stats;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static com.lmax.disruptor.RingBuffer.createMultiProducer;


public class StmPublishingApp extends BaseApp{


    private final RingBuffer<ResultEvent> outputDisruptor =
            createMultiProducer(ResultEvent.RESULT_EVENT_FACTORY, OUTPUT_DISRUPTOR_SIZE, new BusySpinWaitStrategy());

    {
        accounts = new StmAccount[NUM_ACCOUNTS];
        for (int i = 0; i < NUM_ACCOUNTS; i++) {
            accounts[i] = new StmAccount(INITIAL_BALANCE);
        }
    }

    private static final int NUM_WORKERS = 5;

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
    {
        for (int i = 0; i < GATEWAY_PUBLISHERS_COUNT; i++) {
            accountEventPublishers[i] = new AccountEventPublisher(cyclicBarrier, inputDisruptor, ITERATIONS, accounts);
        }
    }

    private final WorkerPool<AccountEvent> workerPool =
            new WorkerPool<AccountEvent>(inputDisruptor,
                    inputDisruptor.newBarrier(),
                    new FatalExceptionHandler(),
                    accountEventWorkHandlers);

    private final ResultEventHandler resultEventHandler = new ResultEventHandler();
    private final BatchEventProcessor<ResultEvent> batchEventProcessor = new BatchEventProcessor<ResultEvent>(outputDisruptor, outputDisruptor.newBarrier(), resultEventHandler);
    {
        outputDisruptor.addGatingSequences(batchEventProcessor.getSequence());
    }

    public static void main( String[] args ) throws Exception{
        new StmPublishingApp().run();
    }

    @Override
    public void start() throws Exception {
        startWork(ITERATIONS);
    }

    private void startWork(final long iterations) throws Exception{
        final CountDownLatch latch = new CountDownLatch(1);
        resultEventHandler.reset(latch, batchEventProcessor.getSequence().get() + ((ITERATIONS/NUM_WORKERS)*NUM_WORKERS));

        RingBuffer<AccountEvent> ringBuffer = workerPool.start(WORKERS_EXECUTOR);

        Future<?>[] futures = new Future[GATEWAY_PUBLISHERS_COUNT];
        for (int i = 0; i < GATEWAY_PUBLISHERS_COUNT; i++) {
            futures[i] = GATEWAY_PUBLISHERS_EXECUTOR.submit(accountEventPublishers[i]);
        }
        GATEWAY_CONSUMERS_EXECUTOR.submit(batchEventProcessor);

        cyclicBarrier.await();

        for (int i = 0; i < GATEWAY_PUBLISHERS_COUNT; i++) {
            futures[i].get();
        }

        latch.await();
        batchEventProcessor.halt();

        workerPool.drainAndHalt();
    }

    @Override
    protected void warmup() throws Exception {
        startWork(WARMUP);
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
    }


}
