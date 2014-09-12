package com.marekmaj.hfplatform.service;


import com.marekmaj.hfplatform.event.incoming.TransferAccountCommand;
import com.marekmaj.hfplatform.service.impl.SingleThreadedPublishingAccountService;
import com.marekmaj.hfplatform.service.model.Account;
import com.marekmaj.hfplatform.service.model.SingleThreadedAccount;

public class TransferOperationPerfTest {

    private static final int OPERATIONS = 500_000_000;
    private static final int WARMUP = 100_000_000;

    public static void main( String[] args ) throws Exception{
        long time = makeTransfer();
        System.out.println("Normal time: " + time + " Mean: " + time / OPERATIONS);
        System.setProperty("op.factor", "2");
        long timeComplex = makeTransfer();
        System.out.println("Complex time: " + timeComplex + " Mean: " + timeComplex / OPERATIONS);
    }

    private static long makeTransfer() {
        final SingleThreadedPublishingAccountService service = new SingleThreadedPublishingAccountService(null);
        final Account from = new SingleThreadedAccount(WARMUP + OPERATIONS);
        final Account to = new SingleThreadedAccount(0);
        final TransferAccountCommand command = new TransferAccountCommand(0);
        command.setFrom(from);
        command.setTo(to);
        command.setAmount(1);

        for (int i = 0; i < WARMUP; i++) {
            service.transfer(command);
        }
        long start = System.nanoTime();

        for (int i = 0; i < OPERATIONS; i++) {
            service.transfer(command);
        }
        return System.nanoTime() - start;
    }
}
