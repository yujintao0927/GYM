package com.gym.modules.rankingcore.api;
public final class Failure extends RuntimeException  {
    private final int status;
    public Failure(int status,String code) {
        super(code);
        this.status=status;
    }
    public int status() {
        return status;
    }
}
