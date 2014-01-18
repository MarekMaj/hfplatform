package com.marekmaj.hfplatform.event.incoming;

import com.lmax.disruptor.RingBuffer;
import com.marekmaj.hfplatform.service.model.Account;
import com.marekmaj.hfplatform.utils.Stats;

import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ThreadLocalRandom;


public final class AccountEventPublisher implements Runnable {
    private final CyclicBarrier cyclicBarrier;
    private final RingBuffer<AccountEvent> ringBuffer;
    private final int iterations;
    private final Account[] accounts;

    public AccountEventPublisher(final CyclicBarrier cyclicBarrier,
                                 final RingBuffer<AccountEvent> ringBuffer,
                                 final int iterations,
                                 final Account[] accounts) {
        this.cyclicBarrier = cyclicBarrier;
        this.ringBuffer = ringBuffer;
        this.iterations = iterations;
        this.accounts = accounts;
    }

    @Override
    public void run() {
        try {
            cyclicBarrier.await();

            for (int i = 0; i < iterations; i++) {
                long sequence = ringBuffer.next();
                AccountEvent event = ringBuffer.get(sequence);
                event.setAccountCommand(createRandomTransferAccountCommand(i));
                Stats.startTimes[i] = System.nanoTime();
                ringBuffer.publish(sequence);
            }
        }
        catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    // TODO wyrzuc te randomy
    private TransferAccountCommand createRandomTransferAccountCommand(int eventId){
        return new TransferAccountCommand(eventId, accounts[ThreadLocalRandom.current().nextInt(accounts.length)],
                accounts[ThreadLocalRandom.current().nextInt(accounts.length)],
                ThreadLocalRandom.current().nextDouble(20));
    }

    private BalanceAccountCommand createRandomBalanceAccountCommand(int eventId){
        return new BalanceAccountCommand(eventId, accounts[ThreadLocalRandom.current().nextInt(accounts.length)]);
    }

}