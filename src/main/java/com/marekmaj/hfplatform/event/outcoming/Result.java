package com.marekmaj.hfplatform.event.outcoming;


import com.marekmaj.hfplatform.utils.WithID;

public final class Result extends WithID {

    private boolean status;
    private double amount;

    public Result(int id) {
        super(id);
    }

    public boolean isStatus() {
        return status;
    }

    public double getAmount() {
        return amount;
    }

    public void setStatus(boolean status) {
        this.status = status;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }
}
