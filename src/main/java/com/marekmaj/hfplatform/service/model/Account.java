package com.marekmaj.hfplatform.service.model;


public abstract class Account {

    // TODO double ...
    public abstract double getBalance();

    public abstract void decreaseBalance(double change) throws InsufficientFundsException;

    public abstract void increaseBalance(double change);
}
