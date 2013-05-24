package com.marekmaj.hfplatform.event.incoming;


public final class Result {

    private final boolean status;
    private final double amount;

    public Result(boolean status, double amount) {
        this.status = status;
        this.amount = amount;
    }

    public boolean isStatus() {
        return status;
    }

    public double getAmount() {
        return amount;
    }
}
