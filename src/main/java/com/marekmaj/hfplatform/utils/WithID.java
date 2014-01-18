package com.marekmaj.hfplatform.utils;


public abstract class WithID {

    private int id;

    protected WithID(int id) {
        this.id = id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }
}
