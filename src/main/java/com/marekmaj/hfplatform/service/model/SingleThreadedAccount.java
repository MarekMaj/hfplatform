package com.marekmaj.hfplatform.service.model;


public class SingleThreadedAccount extends Account{

    private double balance;

    public SingleThreadedAccount(double balance) {
        this.balance = balance;
    }

    @Override
    public double getBalance() {
        return balance;
    }

    @Override
    public void decreaseBalance(double change) throws InsufficientFundsException {
        if (balance < change){
            throw new InsufficientFundsException();
        }
        balance -= change;
    }

    @Override
    public void increaseBalance(double change) {
        balance += change;
    }
}
