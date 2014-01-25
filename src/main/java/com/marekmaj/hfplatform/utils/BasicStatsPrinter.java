package com.marekmaj.hfplatform.utils;


public final class BasicStatsPrinter {

    public static void showStats(long opsPerSecond, double initialBalance, double balance){
        System.out.println( "Initial whole balance: " + initialBalance);
        System.out.println( "Current balance: " + balance);
        System.out.println( "Ops per second: " + opsPerSecond);
        System.out.println( "Rollbacks for transfer operation: " + Stats.getTransactionRollbacks());
        System.out.println( "Rollbacks for account manipulation: " + Stats.getAccountRollbacks());
        System.out.println( "Canceled (the same account): " + Stats.getCanceled());
        System.out.println( "Canceled (insufficient funds): " + Stats.getInsufficient());
    }
}
