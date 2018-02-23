package com.sumsubstance;

public class SBtuple {
    private final String id;
    private final Boolean result;

    public static SBtuple of(String id, Boolean result) {
        return new SBtuple(id, result);
    }

    private SBtuple(String id, Boolean result) {
        this.id = id;
        this.result = result;
    }
}
