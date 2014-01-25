package com.marekmaj.hfplatform.utils;


import com.marekmaj.hfplatform.processor.AccountEventWorkHandler;

public final class MinorStatsPrinter {

    public static void printAccountEventHandlersStats(AccountEventWorkHandler... accountEventWorkHandlers) {
        System.out.println();
        System.out.println( "-------------ACCOUNT EVENT HANDLERS-------------");
        for (AccountEventWorkHandler handler : accountEventWorkHandlers){
            System.out.println( "Total ops for handler " + handler.getCounter());
        }
    }

    public static void printResultEventStats() {
        System.out.println();
        System.out.println( "-------------RESULT EVENTS -------------");
        System.out.println( "Total logged results " + Stats.getLoggedResults());
        System.out.println( "Total ignored results " + Stats.getIgnoredResults());
    }
}
