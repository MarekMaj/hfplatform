package com.marekmaj.hfplatform.event;

import com.lmax.disruptor.RingBuffer;
import com.marekmaj.hfplatform.service.model.Account;

import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ThreadLocalRandom;


public final class AccountEventPublisher implements Runnable {
    private final CyclicBarrier cyclicBarrier;
    private final RingBuffer<AccountEvent> ringBuffer;
    private final long iterations;
    private final Account[] accounts;

    public AccountEventPublisher(final CyclicBarrier cyclicBarrier,
                                 final RingBuffer<AccountEvent> ringBuffer,
                                 final long iterations,
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

            for (long i = 0; i < iterations; i++) {
                long sequence = ringBuffer.next();
                AccountEvent event = ringBuffer.get(sequence);
                event.setAccountCommand(createRandomTransferAccountCommand());
                ringBuffer.publish(sequence);
            }
        }
        catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    // TODO wyrzuc te randomy
    private TransferAccountCommand createRandomTransferAccountCommand(){
        return new TransferAccountCommand(accounts[ThreadLocalRandom.current().nextInt(accounts.length)],
                accounts[ThreadLocalRandom.current().nextInt(accounts.length)],
                ThreadLocalRandom.current().nextDouble(20));
    }

    private BalanceAccountCommand createRandomBalanceAccountCommand(){
        return new BalanceAccountCommand(accounts[ThreadLocalRandom.current().nextInt(accounts.length)]);
    }

}